package com.local.pos.domain.service;

import com.local.pos.core.BusinessException;
import com.local.pos.domain.model.Product;
import com.local.pos.persistence.dao.ProductDao;

import java.util.List;

public class ProductService {

    private final ProductDao productDao;

    public ProductService(ProductDao productDao) {
        this.productDao = productDao;
    }

    public List<Product> listProducts() {
        return productDao.findAllActive();
    }

    public List<Product> searchProducts(String term) {
        if (term == null || term.isBlank()) {
            return productDao.findAllActive();
        }
        return productDao.searchActive(term.trim().toLowerCase());
    }

    public Product createProduct(Product product) {
        validateForCreate(product);

        if (product.getBarcode() != null
            && productDao.barcodeExists(product.getBarcode())) {
            throw new BusinessException("Ya existe un producto con ese código de barras.");
        }

        long id = productDao.insertWithInitialStock(product);
        product.setId(id);

        return product;
    }

    public Product updateProduct(Product product) {
        validateForUpdate(product);

        if (product.getBarcode() != null
            && productDao.barcodeExistsForOtherProduct(product.getBarcode(), product.getId())) {
            throw new BusinessException("Ya existe otro producto con ese código de barras.");
        }

        productDao.update(product);

        return product;
    }

    public void deactivateProduct(Product product) {
        if (product == null || product.getId() == null) {
            throw new BusinessException("No se puede desactivar un producto sin ID.");
        }
        productDao.deactivate(product.getId());
    }

    private void validateForCreate(Product product) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new BusinessException("El nombre del producto es obligatorio.");
        }

        if (product.getPrice() == null || product.getPrice().signum() < 0) {
            throw new BusinessException("El precio debe ser mayor o igual a cero.");
        }

        if (product.getCost() == null || product.getCost().signum() < 0) {
            throw new BusinessException("El costo debe ser mayor o igual a cero.");
        }

        if (product.getStockQty() == null || product.getStockQty().signum() < 0) {
            throw new BusinessException("El stock no puede ser negativo.");
        }

        if (product.getMinStockQty() == null || product.getMinStockQty().signum() < 0) {
            throw new BusinessException("El stock mínimo no puede ser negativo.");
        }
    }

    private void validateForUpdate(Product product) {
        if (product.getId() == null) {
            throw new BusinessException("No se puede actualizar un producto sin ID.");
        }

        validateForCreate(product);
    }
}
