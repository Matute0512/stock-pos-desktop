package com.local.pos.config;

import com.local.pos.domain.service.ProductService;
import com.local.pos.domain.service.StockService;
import com.local.pos.persistence.dao.ProductDao;
import com.local.pos.persistence.dao.StockMovementDao;
import com.local.pos.persistence.db.DataSourceFactory;
import com.local.pos.persistence.db.FlywayMigrator;
import com.zaxxer.hikari.HikariDataSource;

import java.nio.file.Path;

public class AppContext {

    private HikariDataSource dataSource;
    private ProductService productService;
    private StockService stockService;

    public void init() {
        Path dataDir = Path.of(System.getProperty("user.home"), ".stockventa", "data");

        dataSource = DataSourceFactory.create(dataDir);
        FlywayMigrator.migrate(dataSource);

        ProductDao productDao = new ProductDao(dataSource);
        StockMovementDao stockMovementDao = new StockMovementDao(dataSource);

        productService = new ProductService(productDao);
        stockService = new StockService(dataSource, productDao, stockMovementDao);
    }

    public ProductService getProductService() {
        return productService;
    }

    public StockService getStockService() {
        return stockService;
    }

    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
