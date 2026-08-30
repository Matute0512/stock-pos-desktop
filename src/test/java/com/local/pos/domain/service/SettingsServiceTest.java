package com.local.pos.domain.service;

import com.local.pos.persistence.db.FlywayMigrator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class SettingsServiceTest {

    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:settings" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        dataSource = new HikariDataSource(config);
        FlywayMigrator.migrate(dataSource);
    }

    @Test
    void defaultsCuandoNoHayFilas() {
        SettingsService service = new SettingsService(dataSource);

        assertThat(service.getStoreName()).isEqualTo("Mi Local");
        assertThat(service.getStoreAddress()).isEqualTo("");
        assertThat(service.getStorePhone()).isEqualTo("");
        assertThat(service.getTicketFooter()).isEqualTo("Gracias por su compra");
    }

    @Test
    void saveAllPersisteYGetRefleja() {
        SettingsService service = new SettingsService(dataSource);
        service.saveAll("Almacén de María", "Av. Siempre Viva 123", "555-1234", "Vuelva pronto");

        assertThat(service.getStoreName()).isEqualTo("Almacén de María");
        assertThat(service.getStoreAddress()).isEqualTo("Av. Siempre Viva 123");
        assertThat(service.getStorePhone()).isEqualTo("555-1234");
        assertThat(service.getTicketFooter()).isEqualTo("Vuelva pronto");

        // Una instancia nueva lee lo persistido
        SettingsService reloaded = new SettingsService(dataSource);
        assertThat(reloaded.getStoreName()).isEqualTo("Almacén de María");
        assertThat(reloaded.getStoreAddress()).isEqualTo("Av. Siempre Viva 123");
        assertThat(reloaded.getStorePhone()).isEqualTo("555-1234");
        assertThat(reloaded.getTicketFooter()).isEqualTo("Vuelva pronto");
    }
}
