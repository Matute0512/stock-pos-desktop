package com.local.pos.persistence.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DataSourceFactory {

    private DataSourceFactory() {
    }

    public static HikariDataSource create(Path dataDir) {
        try{
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear la carpeta de datos: " + dataDir, e);
        }

        Path dbPath = dataDir.resolve("pos").toAbsolutePath();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:file:"+ dbPath + ";DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(5);

        return new HikariDataSource(config);
    }
}
