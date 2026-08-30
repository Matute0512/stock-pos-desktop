package com.local.pos.config;

import com.local.pos.domain.service.ProductService;
import com.local.pos.persistence.dao.ProductDao;
import com.local.pos.persistence.db.DataSourceFactory;
import com.local.pos.persistence.db.FlywayMigrator;
import com.zaxxer.hikari.HikariDataSource;

import java.nio.file.Path;

public class AppContext {

    private HikariDataSource dataSource;
    private ProductDao productDao;
    private ProductService productService;

    public void init(){
        Path dataDir = Path.of(System.getProperty("user.home"), ".stockventa", "data");

        dataSource = DataSourceFactory.create(dataDir);
        FlywayMigrator.migrate(dataSource);

        productDao = new ProductDao(dataSource);
        productService = new ProductService(productDao);
    }

    public ProductService getProductService() {
        return productService;
    }

    public void shutdown(){
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
