package com.local.pos.persistence.dao;

import com.local.pos.core.TechnicalException;
import com.local.pos.domain.model.LowStockProduct;
import com.local.pos.domain.model.SalesSummary;
import com.local.pos.domain.model.StockValuation;
import com.local.pos.domain.model.TopSellingProduct;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportDao {

    private final DataSource dataSource;

    public ReportDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public SalesSummary getSalesSummary(LocalDate date) {
        String sql = """
                SELECT COUNT(*) AS total_sales,
                       COALESCE(SUM(CASE WHEN PAYMENT_METHOD = 'CASH' THEN TOTAL ELSE 0 END), 0) AS cash_total,
                       COALESCE(SUM(CASE WHEN PAYMENT_METHOD = 'CARD' THEN TOTAL ELSE 0 END), 0) AS card_total,
                       COALESCE(SUM(CASE WHEN PAYMENT_METHOD NOT IN ('CASH', 'CARD') THEN TOTAL ELSE 0 END), 0) AS other_total,
                       COALESCE(SUM(TOTAL), 0) AS grand_total
                FROM SALE
                WHERE CAST(SALE_DATE AS DATE) = ?
                  AND STATUS = 'COMPLETED'
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new SalesSummary(
                        rs.getInt("total_sales"),
                        rs.getBigDecimal("cash_total"),
                        rs.getBigDecimal("card_total"),
                        rs.getBigDecimal("other_total"),
                        rs.getBigDecimal("grand_total")
                    );
                }
                return new SalesSummary(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            }

        } catch (SQLException e) {
            throw new TechnicalException("Error al obtener el resumen de ventas", e);
        }
    }

    public List<TopSellingProduct> getTopSellingProducts(LocalDate fromDate, LocalDate toDate, int limit) {
        String sql = """
                SELECT si.PRODUCT_ID,
                       si.PRODUCT_NAME,
                       SUM(si.QUANTITY)   AS quantity_sold,
                       SUM(si.LINE_TOTAL) AS total_revenue
                FROM SALE_ITEM si
                INNER JOIN SALE s ON s.ID = si.SALE_ID
                WHERE s.STATUS = 'COMPLETED'
                  AND CAST(s.SALE_DATE AS DATE) >= ?
                  AND CAST(s.SALE_DATE AS DATE) <= ?
                GROUP BY si.PRODUCT_ID, si.PRODUCT_NAME
                ORDER BY quantity_sold DESC
                LIMIT ?
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(fromDate));
            ps.setDate(2, Date.valueOf(toDate));
            ps.setInt(3, limit);

            try (ResultSet rs = ps.executeQuery()) {
                List<TopSellingProduct> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new TopSellingProduct(
                        rs.getLong("PRODUCT_ID"),
                        rs.getString("PRODUCT_NAME"),
                        rs.getBigDecimal("quantity_sold"),
                        rs.getBigDecimal("total_revenue")
                    ));
                }
                return list;
            }

        } catch (SQLException e) {
            throw new TechnicalException("Error al obtener los productos más vendidos", e);
        }
    }

    public List<StockValuation> getStockValuation() {
        String sql = """
                SELECT ID, NAME, STOCK_QTY, COST, PRICE,
                       COST * STOCK_QTY  AS value_at_cost,
                       PRICE * STOCK_QTY AS value_at_price
                FROM PRODUCT
                WHERE ACTIVE = TRUE
                ORDER BY NAME
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<StockValuation> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new StockValuation(
                    rs.getLong("ID"),
                    rs.getString("NAME"),
                    rs.getBigDecimal("STOCK_QTY"),
                    rs.getBigDecimal("COST"),
                    rs.getBigDecimal("PRICE"),
                    rs.getBigDecimal("value_at_cost"),
                    rs.getBigDecimal("value_at_price")
                ));
            }
            return list;

        } catch (SQLException e) {
            throw new TechnicalException("Error al obtener la valorización de stock", e);
        }
    }

    public List<LowStockProduct> getLowStockProducts() {
        String sql = """
                SELECT ID, NAME, STOCK_QTY, MIN_STOCK_QTY
                FROM PRODUCT
                WHERE ACTIVE = TRUE
                  AND STOCK_QTY < MIN_STOCK_QTY
                ORDER BY NAME
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<LowStockProduct> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new LowStockProduct(
                    rs.getLong("ID"),
                    rs.getString("NAME"),
                    rs.getBigDecimal("STOCK_QTY"),
                    rs.getBigDecimal("MIN_STOCK_QTY")
                ));
            }
            return list;

        } catch (SQLException e) {
            throw new TechnicalException("Error al obtener productos bajo stock mínimo", e);
        }
    }
}
