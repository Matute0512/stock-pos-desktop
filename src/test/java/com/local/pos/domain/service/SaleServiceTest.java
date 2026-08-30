package com.local.pos.domain.service;

import com.local.pos.core.BusinessException;
import com.local.pos.domain.model.PaymentMethod;
import com.local.pos.domain.model.Product;
import com.local.pos.domain.model.Sale;
import com.local.pos.domain.model.StockMovement;
import com.local.pos.persistence.dao.ProductDao;
import com.local.pos.persistence.dao.SaleDao;
import com.local.pos.persistence.dao.SaleItemDao;
import com.local.pos.persistence.dao.StockMovementDao;
import com.local.pos.persistence.db.FlywayMigrator;
import com.local.pos.ui.pos.CartItem;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SaleServiceTest {

    private DataSource dataSource;
    private SaleService saleService;
    private ProductDao productDao;
    private StockMovementDao stockMovementDao;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:sale" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        dataSource = new HikariDataSource(config);
        FlywayMigrator.migrate(dataSource);

        productDao = new ProductDao(dataSource);
        stockMovementDao = new StockMovementDao(dataSource);
        SaleDao saleDao = new SaleDao(dataSource);
        SaleItemDao saleItemDao = new SaleItemDao(dataSource);
        saleService = new SaleService(dataSource, saleDao, saleItemDao, productDao, stockMovementDao);

        Product product = new Product();
        product.setName("Coca");
        product.setBarcode("7790000000011");
        product.setPrice(new BigDecimal("100.00"));
        product.setCost(new BigDecimal("50.00"));
        product.setStockQty(new BigDecimal("10"));
        product.setMinStockQty(BigDecimal.ZERO);
        product.setActive(true);

        long id = productDao.insertWithInitialStock(product);
        product.setId(id);
        testProduct = productDao.findById(id);
    }

    private CartItem item(BigDecimal quantity) {
        return new CartItem(testProduct.getId(), testProduct.getName(),
            testProduct.getPrice(), quantity, testProduct.getStockQty());
    }

    @Test
    void ventaCorrectaDescuentaStockYGeneraMovimientoSALE() {
        Sale sale = saleService.createSale(List.of(item(new BigDecimal("2"))),
            BigDecimal.ZERO, PaymentMethod.CARD, null);

        assertThat(sale.getId()).isNotNull();
        assertThat(sale.getSubtotal()).isEqualByComparingTo("200.00");
        assertThat(sale.getTotal()).isEqualByComparingTo("200.00");

        Product after = productDao.findById(testProduct.getId());
        assertThat(after.getStockQty()).isEqualByComparingTo("8.00");

        List<StockMovement> movements = stockMovementDao.findByProductId(testProduct.getId());
        List<StockMovement> sales = movements.stream()
            .filter(m -> "SALE".equals(m.getMovementType()))
            .toList();
        assertThat(sales).hasSize(1);
        assertThat(sales.get(0).getQuantity()).isEqualByComparingTo("2.00");
        assertThat(sales.get(0).getResultStockQty()).isEqualByComparingTo("8.00");
        assertThat(sales.get(0).getReferenceType()).isEqualTo("SALE");
        assertThat(sales.get(0).getReferenceId()).isEqualTo(sale.getId());
    }

    @Test
    void totalYCambioDeEfectivoCorrectos() {
        Sale sale = saleService.createSale(List.of(item(new BigDecimal("2"))),
            BigDecimal.ZERO, PaymentMethod.CASH, new BigDecimal("500.00"));

        assertThat(sale.getSubtotal()).isEqualByComparingTo("200.00");
        assertThat(sale.getTotal()).isEqualByComparingTo("200.00");
        assertThat(sale.getPaymentMethod()).isEqualTo("CASH");
        assertThat(sale.getCashReceived()).isEqualByComparingTo("500.00");
        assertThat(sale.getChangeAmount()).isEqualByComparingTo("300.00");
    }

    @Test
    void stockInsuficienteLanzaBusinessExceptionYHaceRollback() {
        assertThatThrownBy(() ->
            saleService.createSale(List.of(item(new BigDecimal("99"))),
                BigDecimal.ZERO, PaymentMethod.CARD, null))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Stock insuficiente");

        Product after = productDao.findById(testProduct.getId());
        assertThat(after.getStockQty()).isEqualByComparingTo("10.00");

        List<StockMovement> movements = stockMovementDao.findByProductId(testProduct.getId());
        assertThat(movements).noneMatch(m -> "SALE".equals(m.getMovementType()));
    }

    @Test
    void descuentoMayorAlSubtotalEsRechazado() {
        assertThatThrownBy(() ->
            saleService.createSale(List.of(item(BigDecimal.ONE)),
                new BigDecimal("500.00"), PaymentMethod.CARD, null))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("El descuento no puede superar el subtotal");
    }

    @Test
    void numerosDeVentaIncrementales() {
        Sale first = saleService.createSale(List.of(item(BigDecimal.ONE)),
            BigDecimal.ZERO, PaymentMethod.CARD, null);
        Sale second = saleService.createSale(List.of(item(BigDecimal.ONE)),
            BigDecimal.ZERO, PaymentMethod.CARD, null);

        assertThat(first.getSaleNumber()).isEqualTo("00000001");
        assertThat(second.getSaleNumber()).isEqualTo("00000002");
    }
}
