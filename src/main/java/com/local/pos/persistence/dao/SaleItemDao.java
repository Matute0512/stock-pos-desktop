package com.local.pos.persistence.dao;

import com.local.pos.core.TechnicalException;
import com.local.pos.domain.model.SaleItem;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SaleItemDao {

    private final DataSource dataSource;

    public SaleItemDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void insertItem(Connection con, SaleItem item) throws SQLException {
        String sql = """
                INSERT INTO SALE_ITEM (
                    SALE_ID, PRODUCT_ID, PRODUCT_NAME, QUANTITY, UNIT_PRICE, LINE_TOTAL
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, item.getSaleId());

            if (item.getProductId() == null) {
                ps.setNull(2, java.sql.Types.BIGINT);
            } else {
                ps.setLong(2, item.getProductId());
            }

            ps.setString(3, item.getProductName());
            ps.setBigDecimal(4, item.getQuantity());
            ps.setBigDecimal(5, item.getUnitPrice());
            ps.setBigDecimal(6, item.getLineTotal());
            ps.executeUpdate();
        }
    }

    public List<SaleItem> findBySaleId(long saleId) {
        String sql = """
                SELECT ID, SALE_ID, PRODUCT_ID, PRODUCT_NAME, QUANTITY,
                       UNIT_PRICE, LINE_TOTAL
                FROM SALE_ITEM
                WHERE SALE_ID = ?
                ORDER BY ID
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, saleId);

            try (ResultSet rs = ps.executeQuery()) {
                List<SaleItem> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(map(rs));
                }
                return items;
            }

        } catch (SQLException e) {
            throw new TechnicalException("Error al listar items de venta", e);
        }
    }

    private SaleItem map(ResultSet rs) throws SQLException {
        SaleItem item = new SaleItem();
        item.setId(rs.getLong("ID"));
        item.setSaleId(rs.getLong("SALE_ID"));

        long productId = rs.getLong("PRODUCT_ID");
        item.setProductId(rs.wasNull() ? null : productId);

        item.setProductName(rs.getString("PRODUCT_NAME"));
        item.setQuantity(rs.getBigDecimal("QUANTITY"));
        item.setUnitPrice(rs.getBigDecimal("UNIT_PRICE"));
        item.setLineTotal(rs.getBigDecimal("LINE_TOTAL"));
        return item;
    }
}
