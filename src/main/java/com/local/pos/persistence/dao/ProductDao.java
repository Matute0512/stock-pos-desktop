package com.local.pos.persistence.dao;

import com.local.pos.core.TechnicalException;
import com.local.pos.domain.model.Product;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    private final DataSource dataSource;

    public ProductDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Product> findAllActive() {
        String sql = """
                SELECT ID, SKU, BARCODE, NAME, DESCRIPTION, CATEGORY_ID,
                       COST, PRICE, STOCK_QTY, MIN_STOCK_QTY, ACTIVE,
                       CREATED_AT, UPDATED_AT
                FROM PRODUCT
                WHERE ACTIVE = TRUE
                ORDER BY NAME
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Product> products = new ArrayList<>();
            while (rs.next()) {
                products.add(map(rs));
            }
            return products;

        } catch (SQLException e) {
            throw new TechnicalException("Error al listar productos", e);
        }
    }

    public List<Product> searchActive(String term) {
        String sql = """
                SELECT ID, SKU, BARCODE, NAME, DESCRIPTION, CATEGORY_ID,
                       COST, PRICE, STOCK_QTY, MIN_STOCK_QTY, ACTIVE,
                       CREATED_AT, UPDATED_AT
                FROM PRODUCT
                WHERE ACTIVE = TRUE
                  AND (
                        LOWER(NAME) LIKE ?
                        OR LOWER(COALESCE(BARCODE, '')) LIKE ?
                        OR LOWER(COALESCE(SKU, '')) LIKE ?
                      )
                ORDER BY NAME
                """;

        String like = "%" + term + "%";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);

            try (ResultSet rs = ps.executeQuery()) {
                List<Product> products = new ArrayList<>();
                while (rs.next()) {
                    products.add(map(rs));
                }
                return products;
            }

        } catch (SQLException e) {
            throw new TechnicalException("Error al buscar productos", e);
        }
    }

    public boolean barcodeExists(String barcode) {
        String sql = """
                SELECT COUNT(1)
                FROM PRODUCT
                WHERE BARCODE = ?
                  AND ACTIVE = TRUE
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, barcode);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new TechnicalException("Error al verificar código de barras", e);
        }
    }

    public long insertWithInitialStock(Product product) {
        String insertSql = """
                INSERT INTO PRODUCT (
                    SKU, BARCODE, NAME, DESCRIPTION, CATEGORY_ID,
                    COST, PRICE, STOCK_QTY, MIN_STOCK_QTY, ACTIVE
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)
                """;

        String movementSql = """
                INSERT INTO STOCK_MOVEMENT (
                    PRODUCT_ID, MOVEMENT_TYPE, QUANTITY, RESULT_STOCK_QTY,
                    REFERENCE_TYPE, REFERENCE_ID, REASON
                ) VALUES (?, 'INITIAL', ?, ?, 'PRODUCT', ?, 'Carga inicial')
                """;

        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);

            try {
                long productId;

                try (PreparedStatement ps = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, product.getSku());
                    ps.setString(2, product.getBarcode());
                    ps.setString(3, product.getName());
                    ps.setString(4, product.getDescription());
                    ps.setObject(5, product.getCategoryId());
                    ps.setBigDecimal(6, product.getCost());
                    ps.setBigDecimal(7, product.getPrice());
                    ps.setBigDecimal(8, product.getStockQty());
                    ps.setBigDecimal(9, product.getMinStockQty());
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new TechnicalException("No se pudo obtener el ID del producto");
                        }
                        productId = keys.getLong(1);
                    }
                }

                if (product.getStockQty() != null
                    && product.getStockQty().signum() > 0) {

                    try (PreparedStatement ps = con.prepareStatement(movementSql)) {
                        ps.setLong(1, productId);
                        ps.setBigDecimal(2, product.getStockQty());
                        ps.setBigDecimal(3, product.getStockQty());
                        ps.setLong(4, productId);
                        ps.executeUpdate();
                    }
                }

                con.commit();
                return productId;

            } catch (Exception e) {
                con.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new TechnicalException("Error al guardar el producto", e);
        }
    }

    private Product map(ResultSet rs) throws SQLException {
        Product product = new Product();

        product.setId(rs.getLong("ID"));
        product.setSku(rs.getString("SKU"));
        product.setBarcode(rs.getString("BARCODE"));
        product.setName(rs.getString("NAME"));
        product.setDescription(rs.getString("DESCRIPTION"));

        long categoryId = rs.getLong("CATEGORY_ID");
        product.setCategoryId(rs.wasNull() ? null : categoryId);

        product.setCost(rs.getBigDecimal("COST"));
        product.setPrice(rs.getBigDecimal("PRICE"));
        product.setStockQty(rs.getBigDecimal("STOCK_QTY"));
        product.setMinStockQty(rs.getBigDecimal("MIN_STOCK_QTY"));
        product.setActive(rs.getBoolean("ACTIVE"));

        Timestamp createdAt = rs.getTimestamp("CREATED_AT");
        if (createdAt != null) {
            product.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("UPDATED_AT");
        if (updatedAt != null) {
            product.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return product;
    }

    public boolean barcodeExistsForOtherProduct(String barcode, Long excludeProductId) {

        if (barcode == null || barcode.isBlank()) {
            return false;
        }

        String sql = """
                SELECT COUNT(1)
                FROM PRODUCT
                WHERE BARCODE = ?
                  AND ACTIVE = TRUE
                  AND (? IS NULL OR ID <> ?)
                """;

        try (Connection con = dataSource.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)){

            ps.setString(1, barcode);
            if (excludeProductId == null) {
                ps.setNull(2, java.sql.Types.BIGINT);
                ps.setNull(3, java.sql.Types.BIGINT);
            } else {
                ps.setLong(2, excludeProductId);
                ps.setLong(3, excludeProductId);
            }

            try (ResultSet rs = ps.executeQuery()){
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new TechnicalException("Error al verificar código de barras", e);
        }
    }

    public void update(Product product) {
        String sql = """
                UPDATE PRODUCT
                SET SKU = ?,
                    BARCODE = ?,
                    NAME = ?,
                    DESCRIPTION = ?,
                    CATEGORY_ID = ?,
                    COST = ?,
                    PRICE = ?,
                    MIN_STOCK_QTY = ?,
                    UPDATED_AT = CURRENT_TIMESTAMP
                WHERE ID = ?
        """;

        try (Connection con = dataSource.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)){

            ps.setString(1, product.getSku());
            ps.setString(2, product.getBarcode());
            ps.setString(3, product.getName());
            ps.setString(4, product.getDescription());
            ps.setObject(5, product.getCategoryId());
            ps.setBigDecimal(6, product.getCost());
            ps.setBigDecimal(7, product.getPrice());
            ps.setBigDecimal(8, product.getMinStockQty());
            ps.setLong(9, product.getId());

            int rows = ps.executeUpdate();
            if(rows == 0){
                throw new TechnicalException("No se pudo actualizar el producto.");
            }
        } catch (SQLException e) {
            throw new TechnicalException("Error al actualizar el producto", e);
        }
    }

    public void deactivate(long productId) {
        String sql = """
                UPDATE PRODUCT
                SET ACTIVE = FALSE,
                    UPDATED_AT = CURRENT_TIMESTAMP
                WHERE ID = ?
        """;

        try (Connection con = dataSource.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, productId);
            ps.executeUpdate();

        } catch (SQLException e){
            throw new TechnicalException("Error al desactivar el producto", e);
        }
    }
}
