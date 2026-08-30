package com.local.pos.config;

import com.local.pos.domain.service.BackupService;
import com.local.pos.domain.service.ProductService;
import com.local.pos.domain.service.ReportService;
import com.local.pos.domain.service.SaleService;
import com.local.pos.domain.service.SettingsService;
import com.local.pos.domain.service.StockService;
import com.local.pos.persistence.dao.ProductDao;
import com.local.pos.persistence.dao.ReportDao;
import com.local.pos.persistence.dao.SaleDao;
import com.local.pos.persistence.dao.SaleItemDao;
import com.local.pos.persistence.dao.StockMovementDao;
import com.local.pos.persistence.db.DataSourceFactory;
import com.local.pos.persistence.db.FlywayMigrator;
import com.zaxxer.hikari.HikariDataSource;

import java.nio.file.Path;

public class AppContext {

    private HikariDataSource dataSource;
    private ProductService productService;
    private StockService stockService;
    private SaleService saleService;
    private SettingsService settingsService;
    private BackupService backupService;
    private ReportService reportService;

    public void init() {
        Path dataDir = Path.of(System.getProperty("user.home"), ".stockventa", "data");
        Path backupDir = Path.of(System.getProperty("user.home"), ".stockventa", "backups");

        dataSource = DataSourceFactory.create(dataDir);
        FlywayMigrator.migrate(dataSource);

        ProductDao productDao = new ProductDao(dataSource);
        StockMovementDao stockMovementDao = new StockMovementDao(dataSource);
        SaleDao saleDao = new SaleDao(dataSource);
        SaleItemDao saleItemDao = new SaleItemDao(dataSource);

        productService = new ProductService(productDao);
        stockService = new StockService(dataSource, productDao, stockMovementDao);
        saleService = new SaleService(dataSource, saleDao, saleItemDao, productDao, stockMovementDao);
        settingsService = new SettingsService(dataSource);
        backupService = new BackupService(dataSource, dataDir, backupDir);
        reportService = new ReportService(new ReportDao(dataSource));

        // Ya en el hilo de fondo: copia de seguridad automática del día
        backupService.backupIfNoneToday();
    }

    public ProductService getProductService() {
        return productService;
    }

    public StockService getStockService() {
        return stockService;
    }

    public SaleService getSaleService() {
        return saleService;
    }

    public SettingsService getSettingsService() {
        return settingsService;
    }

    public BackupService getBackupService() {
        return backupService;
    }

    public ReportService getReportService() {
        return reportService;
    }

    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
