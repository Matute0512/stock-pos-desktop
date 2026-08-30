package com.local.pos.domain.service;

import com.local.pos.core.BusinessException;
import com.local.pos.domain.model.Product;
import com.local.pos.persistence.dao.ProductDao;

import java.math.BigDecimal;
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
        validate(product);

        if (product.getBarcode() != null && productDao.barcodeExists(product.getBarcode())) {
            throw new BusinessException("Ya existe un producto con ese código de barras.");
        }

        long id = productDao.insertWithInitialStock(product);
        product.setId(id);

        return product;
    }

   private void validate(Product product) {
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
}
