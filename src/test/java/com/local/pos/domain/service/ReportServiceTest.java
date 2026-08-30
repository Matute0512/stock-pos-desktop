package com.local.pos.domain.service;

import com.local.pos.domain.model.LowStockProduct;
import com.local.pos.domain.model.SalesSummary;
import com.local.pos.domain.model.StockValuation;
import com.local.pos.domain.model.TopSellingProduct;
import com.local.pos.persistence.dao.ReportDao;
import com.local.pos.persistence.db.FlywayMigrator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportServiceTest {

    private DataSource dataSource;
    private ReportService reportService;

    private long p1;
    private long p2;
    private long p3;
    private long p4;

    @BeforeEach
    void setUp() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:report" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        dataSource = new HikariDataSource(config);
        FlywayMigrator.migrate(dataSource);

        reportService = new ReportService(new ReportDao(dataSource));

        p1 = insertProduct("Yerba", "100.00", "200.00", "10", "5", true);
        p2 = insertProduct("Café", "50.00", "150.00", "2", "4", true);
        p3 = insertProduct("Azúcar", "30.00", "70.00", "3", "3", true);
        p4 = insertProduct("Inactivo", "10.00", "20.00", "0", "1", false);

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        long s1 = insertSale("00000001", today, "CASH", "600.00");
        insertItem(s1, p1, "Yerba", "3", "200.00");

        long s2 = insertSale("00000002", today, "CARD", "370.00");
        insertItem(s2, p2, "Café", "2", "150.00");
        insertItem(s2, p3, "Azúcar", "1", "70.00");

        long s3 = insertSale("00000003", today, "TRANSFER", "200.00");
        insertItem(s3, p1, "Yerba", "1", "200.00");

        long s4 = insertSale("00000004", yesterday, "CARD", "350.00");
        insertItem(s4, p1, "Yerba", "1", "200.00");
        insertItem(s4, p2, "Café", "1", "150.00");
    }

    @Test
    void getSalesSummaryAgrupaPorMetodoDePago() {
        SalesSummary summary = reportService.getSalesSummary(LocalDate.now());

        assertThat(summary.totalSales()).isEqualTo(3);
        assertThat(summary.cashTotal()).isEqualByComparingTo("600.00");
        assertThat(summary.cardTotal()).isEqualByComparingTo("370.00");
        assertThat(summary.otherTotal()).isEqualByComparingTo("200.00");
        assertThat(summary.grandTotal()).isEqualByComparingTo("1170.00");
    }

    @Test
    void getTopSellingProductsRankingYLimite() {
        List<TopSellingProduct> today = reportService.getTopSellingProducts(LocalDate.now(), LocalDate.now(), 5);

        assertThat(today).hasSize(3);
        assertThat(today.get(0).productId()).isEqualTo(p1);
        assertThat(today.get(0).quantitySold()).isEqualByComparingTo("4.00");
        assertThat(today.get(0).totalRevenue()).isEqualByComparingTo("800.00");
        assertThat(today.get(1).productId()).isEqualTo(p2);
        assertThat(today.get(2).productId()).isEqualTo(p3);

        List<TopSellingProduct> week = reportService.getTopSellingProducts(LocalDate.now().minusDays(7), LocalDate.now(), 5);
        assertThat(week).hasSize(3);
        assertThat(week.get(0).productId()).isEqualTo(p1);
        assertThat(week.get(0).quantitySold()).isEqualByComparingTo("5.00");
        assertThat(week.get(0).totalRevenue()).isEqualByComparingTo("1000.00");

        List<TopSellingProduct> limited = reportService.getTopSellingProducts(LocalDate.now(), LocalDate.now(), 2);
        assertThat(limited).hasSize(2);
    }

    @Test
    void getStockValuationCalculaValoresYExcluyeInactivos() {
        List<StockValuation> valuations = reportService.getStockValuation();

        assertThat(valuations).hasSize(3);

        StockValuation yerba = valuations.stream()
            .filter(v -> v.productId() == p1)
            .findFirst()
            .orElseThrow();
        assertThat(yerba.stockQty()).isEqualByComparingTo("10.00");
        assertThat(yerba.valueAtCost()).isEqualByComparingTo("1000.00");
        assertThat(yerba.valueAtPrice()).isEqualByComparingTo("2000.00");

        assertThat(valuations).noneMatch(v -> v.productId() == p4);
    }

    @Test
    void getLowStockProductsSoloBajoElMinimo() {
        List<LowStockProduct> low = reportService.getLowStockProducts();

        assertThat(low).hasSize(1);
        assertThat(low.get(0).productId()).isEqualTo(p2);
        assertThat(low.get(0).stockQty()).isEqualByComparingTo("2.00");
        assertThat(low.get(0).minStockQty()).isEqualByComparingTo("4.00");
    }

    private long insertProduct(String name, String cost, String price,
                               String stock, String min, boolean active) throws Exception {
        String sql = "INSERT INTO PRODUCT (NAME, COST, PRICE, STOCK_QTY, MIN_STOCK_QTY, ACTIVE) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setBigDecimal(2, new BigDecimal(cost));
            ps.setBigDecimal(3, new BigDecimal(price));
            ps.setBigDecimal(4, new BigDecimal(stock));
            ps.setBigDecimal(5, new BigDecimal(min));
            ps.setBoolean(6, active);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private long insertSale(String number, LocalDate date, String method, String total) throws Exception {
        String sql = "INSERT INTO SALE (SALE_NUMBER, SALE_DATE, SUBTOTAL, DISCOUNT_AMOUNT, TOTAL, PAYMENT_METHOD, STATUS) VALUES (?, ?, ?, 0, ?, ?, 'COMPLETED')";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, number);
            ps.setTimestamp(2, Timestamp.valueOf(date.atTime(10, 0)));
            ps.setBigDecimal(3, new BigDecimal(total));
            ps.setBigDecimal(4, new BigDecimal(total));
            ps.setString(5, method);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private void insertItem(long saleId, long productId, String name,
                            String qty, String unitPrice) throws Exception {
        String sql = "INSERT INTO SALE_ITEM (SALE_ID, PRODUCT_ID, PRODUCT_NAME, QUANTITY, UNIT_PRICE, LINE_TOTAL) VALUES (?, ?, ?, ?, ?, ?)";
        BigDecimal q = new BigDecimal(qty);
        BigDecimal unit = new BigDecimal(unitPrice);
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, saleId);
            ps.setLong(2, productId);
            ps.setString(3, name);
            ps.setBigDecimal(4, q);
            ps.setBigDecimal(5, unit);
            ps.setBigDecimal(6, q.multiply(unit));
            ps.executeUpdate();
        }
    }
}
