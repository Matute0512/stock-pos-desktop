package com.local.pos.persistence.db;

import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

public final class FlywayMigrator {

    private FlywayMigrator() {
    }

    public static void migrate(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load();

        flyway.migrate();
    }
}
