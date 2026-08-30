package com.local.pos.domain.service;

import com.local.pos.core.BusinessException;
import com.local.pos.core.TechnicalException;
import com.local.pos.domain.model.MovementType;
import com.local.pos.domain.model.PaymentMethod;
import com.local.pos.domain.model.Product;
import com.local.pos.domain.model.Sale;
import com.local.pos.domain.model.SaleItem;
import com.local.pos.domain.model.StockMovement;
import com.local.pos.persistence.dao.ProductDao;
import com.local.pos.persistence.dao.SaleDao;
import com.local.pos.persistence.dao.SaleItemDao;
import com.local.pos.persistence.dao.StockMovementDao;
import com.local.pos.ui.pos.CartItem;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class SaleService {

    private static final int MONEY_SCALE = 2;

    private final DataSource dataSource;
    private final SaleDao saleDao;
    private final SaleItemDao saleItemDao;
    private final ProductDao productDao;
    private final StockMovementDao stockMovementDao;

    public SaleService(DataSource dataSource,
                       SaleDao saleDao,
                       SaleItemDao saleItemDao,
                       ProductDao productDao,
                       StockMovementDao stockMovementDao) {
        this.dataSource = dataSource;
        this.saleDao = saleDao;
        this.saleItemDao = saleItemDao;
        this.productDao = productDao;
        this.stockMovementDao = stockMovementDao;
    }

    /**
     * Registra una venta completa en UNA transacción: número incremental,
     * items con snapshot de nombre, descuento de stock y movimientos 'SALE'.
     */
    public Sale createSale(List<CartItem> items,
                           BigDecimal discount,
                           PaymentMethod method,
                           BigDecimal cashReceived) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("El carrito está vacío.");
        }
        if (discount == null) {
            discount = BigDecimal.ZERO;
        }
        if (discount.signum() < 0) {
            throw new BusinessException("El descuento no puede ser negativo.");
        }
        if (method == null) {
            throw new BusinessException("Seleccioná un método de pago.");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            subtotal = subtotal.add(item.lineTotal());
        }
        subtotal = subtotal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        if (discount.compareTo(subtotal) > 0) {
            throw new BusinessException("El descuento no puede superar el subtotal.");
        }

        BigDecimal total = subtotal.subtract(discount)
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);

            try {
                int next = saleDao.consumeNextSaleNumber(con);
                String saleNumber = String.format("%08d", next);

                Sale sale = new Sale();
                sale.setSaleNumber(saleNumber);
                sale.setSaleDate(LocalDateTime.now());
                sale.setSubtotal(subtotal);
                sale.setDiscountAmount(discount);
                sale.setTotal(total);
                sale.setPaymentMethod(method.name());
                sale.setStatus("COMPLETED");

                if (method == PaymentMethod.CASH) {
                    if (cashReceived == null || cashReceived.signum() < 0) {
                        throw new BusinessException("Ingresá el monto recibido en efectivo.");
                    }
                    if (cashReceived.compareTo(total) < 0) {
                        throw new BusinessException("El monto recibido no alcanza el total.");
                    }
                    sale.setCashReceived(cashReceived.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
                    sale.setChangeAmount(cashReceived.subtract(total)
                        .setScale(MONEY_SCALE, RoundingMode.HALF_UP));
                }

                long saleId = saleDao.insertSale(con, sale);
                sale.setId(saleId);

                for (CartItem item : items) {
                    Product product = productDao.findByIdForUpdate(con, item.getProductId());
                    if (product == null || !product.isActive()) {
                        throw new BusinessException("El producto '" + item.getName() + "' no está disponible.");
                    }
                    if (item.getQuantity().compareTo(product.getStockQty()) > 0) {
                        throw new BusinessException("Stock insuficiente para: " + product.getName());
                    }

                    BigDecimal newStock = product.getStockQty().subtract(item.getQuantity());

                    SaleItem saleItem = new SaleItem();
                    saleItem.setSaleId(saleId);
                    saleItem.setProductId(product.getId());
                    saleItem.setProductName(product.getName());
                    saleItem.setQuantity(item.getQuantity());
                    saleItem.setUnitPrice(item.getUnitPrice());
                    saleItem.setLineTotal(item.lineTotal());
                    saleItemDao.insertItem(con, saleItem);

                    productDao.updateStock(con, product.getId(), newStock);

                    StockMovement movement = new StockMovement();
                    movement.setProductId(product.getId());
                    movement.setMovementType(MovementType.SALE.name());
                    movement.setQuantity(item.getQuantity());
                    movement.setResultStockQty(newStock);
                    movement.setReferenceType("SALE");
                    movement.setReferenceId(saleId);
                    movement.setReason("Venta N° " + saleNumber);
                    stockMovementDao.insert(con, movement);
                }

                con.commit();
                return sale;

            } catch (Exception e) {
                con.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new TechnicalException("Error al registrar la venta", e);
        }
    }

    public List<Sale> listSalesToday() {
        return saleDao.listSalesToday();
    }

    public List<SaleItem> getSaleItems(long saleId) {
        return saleItemDao.findBySaleId(saleId);
    }
}
