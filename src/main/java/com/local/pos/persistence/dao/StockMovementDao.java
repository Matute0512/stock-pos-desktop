package com.local.pos.persistence.dao;

import com.local.pos.core.TechnicalException;
import com.local.pos.domain.model.StockMovement;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class StockMovementDao {

    private final DataSource dataSource;

    public StockMovementDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public long insert(StockMovement movement) {
        try (Connection con = dataSource.getConnection()) {
            return insert(con, movement);
        } catch (SQLException e) {
            throw new TechnicalException("Error al guardar el movimiento de stock", e);
        }
    }

    public long insert(Connection con, StockMovement movement) throws SQLException {
        String sql = """
                INSERT INTO STOCK_MOVEMENT (
                    PRODUCT_ID, MOVEMENT_TYPE, QUANTITY, RESULT_STOCK_QTY,
                    REFERENCE_TYPE, REFERENCE_ID, REASON
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, movement.getProductId());
            ps.setString(2, movement.getMovementType());
            ps.setBigDecimal(3, movement.getQuantity());
            ps.setBigDecimal(4, movement.getResultStockQty());
            ps.setString(5, movement.getReferenceType());

            if (movement.getReferenceId() == null) {
                ps.setNull(6, java.sql.Types.BIGINT);
            } else {
                ps.setLong(6, movement.getReferenceId());
            }

            ps.setString(7, movement.getReason());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }

            throw new TechnicalException("No se pudo obtener el ID del movimiento");
        }
    }

    public List<StockMovement> findByProductId(long productId) {
        String sql = """
                SELECT m.ID, m.PRODUCT_ID, m.MOVEMENT_TYPE, m.QUANTITY,
                       m.RESULT_STOCK_QTY, m.REFERENCE_TYPE, m.REFERENCE_ID,
                       m.REASON, m.CREATED_AT,
                       p.NAME AS PRODUCT_NAME
                FROM STOCK_MOVEMENT m
                INNER JOIN PRODUCT p ON p.ID = m.PRODUCT_ID
                WHERE m.PRODUCT_ID = ?
                ORDER BY m.CREATED_AT DESC, m.ID DESC
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, productId);

            try (ResultSet rs = ps.executeQuery()) {
                List<StockMovement> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }

        } catch (SQLException e) {
            throw new TechnicalException("Error al listar movimientos", e);
        }
    }

    private StockMovement map(ResultSet rs) throws SQLException {
        StockMovement m = new StockMovement();
        m.setId(rs.getLong("ID"));
        m.setProductId(rs.getLong("PRODUCT_ID"));
        m.setProductName(rs.getString("PRODUCT_NAME"));
        m.setMovementType(rs.getString("MOVEMENT_TYPE"));
        m.setQuantity(rs.getBigDecimal("QUANTITY"));
        m.setResultStockQty(rs.getBigDecimal("RESULT_STOCK_QTY"));
        m.setReferenceType(rs.getString("REFERENCE_TYPE"));

        long refId = rs.getLong("REFERENCE_ID");
        m.setReferenceId(rs.wasNull() ? null : refId);

        m.setReason(rs.getString("REASON"));

        Timestamp ts = rs.getTimestamp("CREATED_AT");
        if (ts != null) {
            m.setCreatedAt(ts.toLocalDateTime());
        }

        return m;
    }
}
