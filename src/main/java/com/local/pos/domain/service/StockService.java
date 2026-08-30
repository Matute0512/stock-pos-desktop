package com.local.pos.domain.service;

import com.local.pos.core.BusinessException;
import com.local.pos.core.TechnicalException;
import com.local.pos.domain.model.MovementType;
import com.local.pos.domain.model.Product;
import com.local.pos.domain.model.StockMovement;
import com.local.pos.persistence.dao.ProductDao;
import com.local.pos.persistence.dao.StockMovementDao;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class StockService {

    private final DataSource dataSource;
    private final ProductDao productDao;
    private final StockMovementDao stockMovementDao;

    public StockService(DataSource dataSource,
                        ProductDao productDao,
                        StockMovementDao stockMovementDao) {
        this.dataSource = dataSource;
        this.productDao = productDao;
        this.stockMovementDao = stockMovementDao;
    }

    public List<StockMovement> getHistory(long productId) {
        return stockMovementDao.findByProductId(productId);
    }

    public void adjustStock(long productId,
                            MovementType type,
                            BigDecimal quantity,
                            String reason) {
        if (type == null) {
            throw new BusinessException("Debe seleccionar un tipo de movimiento.");
        }

        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessException("La cantidad debe ser mayor a cero.");
        }

        if (reason == null || reason.isBlank()) {
            throw new BusinessException("El motivo es obligatorio.");
        }

        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);

            try {
                Product product = productDao.findById(productId);
                if (product == null || !product.isActive()) {
                    throw new BusinessException("El producto no existe o está desactivado.");
                }

                BigDecimal current = product.getStockQty();
                BigDecimal newStock;
                BigDecimal movementQuantity;

                switch (type) {
                    case PURCHASE, RETURN -> {
                        movementQuantity = quantity;
                        newStock = current.add(quantity);
                    }
                    case ADJUST_OUT -> {
                        movementQuantity = quantity;
                        newStock = current.subtract(quantity);
                        if (newStock.signum() < 0) {
                            throw new BusinessException(
                                "Stock insuficiente. Stock actual: " + current.toPlainString()
                            );
                        }
                    }
                    case ADJUST_IN -> {
                        // Corrección: la cantidad es el stock FINAL deseado
                        newStock = quantity;
                        if (newStock.signum() < 0) {
                            throw new BusinessException("El stock resultante no puede ser negativo.");
                        }
                        movementQuantity = newStock.subtract(current).abs();
                    }
                    default -> throw new BusinessException("Tipo de movimiento no soportado en ajuste.");
                }

                productDao.updateStock(productId, newStock);

                StockMovement movement = new StockMovement();
                movement.setProductId(productId);
                movement.setMovementType(type.name());
                movement.setQuantity(movementQuantity);
                movement.setResultStockQty(newStock);
                movement.setReferenceType("MANUAL");
                movement.setReason(reason.trim());

                stockMovementDao.insert(movement);

                con.commit();

            } catch (Exception e) {
                con.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new TechnicalException("Error al ajustar el stock", e);
        }
    }
}
