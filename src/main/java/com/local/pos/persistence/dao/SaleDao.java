package com.local.pos.persistence.dao;

import com.local.pos.core.TechnicalException;
import com.local.pos.domain.model.Sale;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class SaleDao {

    private final DataSource dataSource;

    public SaleDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Consume el próximo número de venta dentro de la transacción del llamador
     * (bloquea la fila con FOR UPDATE y deja el contador en n+1).
     */
    public int consumeNextSaleNumber(Connection con) throws SQLException {
        String sql = """
                SELECT SETTING_VALUE
                FROM SETTING
                WHERE SETTING_KEY = 'SALE_NUMBER_NEXT'
                FOR UPDATE
                """;

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (!rs.next()) {
                throw new TechnicalException("No se encontró el contador SALE_NUMBER_NEXT.");
            }

            int next = Integer.parseInt(rs.getString(1));

            try (PreparedStatement up = con.prepareStatement(
                    "UPDATE SETTING SET SETTING_VALUE = ? WHERE SETTING_KEY = 'SALE_NUMBER_NEXT'")) {
                up.setString(1, String.valueOf(next + 1));
                up.executeUpdate();
            }

            return next;
        }
    }

    public long insertSale(Connection con, Sale sale) throws SQLException {
        String sql = """
                INSERT INTO SALE (
                    SALE_NUMBER, SALE_DATE, SUBTOTAL, DISCOUNT_AMOUNT, TOTAL,
                    PAYMENT_METHOD, CASH_RECEIVED, CHANGE_AMOUNT, STATUS, NOTES
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sale.getSaleNumber());
            ps.setTimestamp(2, Timestamp.valueOf(sale.getSaleDate()));
            ps.setBigDecimal(3, sale.getSubtotal());
            ps.setBigDecimal(4, sale.getDiscountAmount());
            ps.setBigDecimal(5, sale.getTotal());
            ps.setString(6, sale.getPaymentMethod());

            if (sale.getCashReceived() == null) {
                ps.setNull(7, java.sql.Types.DECIMAL);
            } else {
                ps.setBigDecimal(7, sale.getCashReceived());
            }

            if (sale.getChangeAmount() == null) {
                ps.setNull(8, java.sql.Types.DECIMAL);
            } else {
                ps.setBigDecimal(8, sale.getChangeAmount());
            }

            ps.setString(9, sale.getStatus());
            ps.setString(10, sale.getNotes());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }

            throw new TechnicalException("No se pudo obtener el ID de la venta");
        }
    }

    public List<Sale> listSalesToday() {
        String sql = """
                SELECT ID, SALE_NUMBER, SALE_DATE, SUBTOTAL, DISCOUNT_AMOUNT, TOTAL,
                       PAYMENT_METHOD, CASH_RECEIVED, CHANGE_AMOUNT, STATUS, NOTES
                FROM SALE
                WHERE CAST(SALE_DATE AS DATE) = CURRENT_DATE
                ORDER BY SALE_DATE DESC, ID DESC
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Sale> sales = new ArrayList<>();
            while (rs.next()) {
                sales.add(map(rs));
            }
            return sales;

        } catch (SQLException e) {
            throw new TechnicalException("Error al listar ventas del día", e);
        }
    }

    private Sale map(ResultSet rs) throws SQLException {
        Sale sale = new Sale();
        sale.setId(rs.getLong("ID"));
        sale.setSaleNumber(rs.getString("SALE_NUMBER"));

        Timestamp ts = rs.getTimestamp("SALE_DATE");
        if (ts != null) {
            sale.setSaleDate(ts.toLocalDateTime());
        }

        sale.setSubtotal(rs.getBigDecimal("SUBTOTAL"));
        sale.setDiscountAmount(rs.getBigDecimal("DISCOUNT_AMOUNT"));
        sale.setTotal(rs.getBigDecimal("TOTAL"));
        sale.setPaymentMethod(rs.getString("PAYMENT_METHOD"));

        BigDecimal cash = rs.getBigDecimal("CASH_RECEIVED");
        sale.setCashReceived(rs.wasNull() ? null : cash);

        BigDecimal change = rs.getBigDecimal("CHANGE_AMOUNT");
        sale.setChangeAmount(rs.wasNull() ? null : change);

        sale.setStatus(rs.getString("STATUS"));
        sale.setNotes(rs.getString("NOTES"));

        return sale;
    }
}
