package com.local.pos.domain.service;

import com.local.pos.persistence.db.FlywayMigrator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BackupServiceTest {

    @TempDir
    Path tempDir;

    private HikariDataSource dataSource;
    private BackupService backupService;
    private Path backupDir;

    @BeforeEach
    void setUp() throws IOException {
        Path dataDir = tempDir.resolve("data");
        backupDir = tempDir.resolve("backups");
        Files.createDirectories(dataDir);
        Files.createDirectories(backupDir);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(
            "jdbc:h2:file:" + dataDir.resolve("pos").toAbsolutePath().toString().replace('\\', '/')
                + ";DB_CLOSE_DELAY=-1"
        );
        config.setUsername("sa");
        config.setPassword("");
        dataSource = new HikariDataSource(config);
        FlywayMigrator.migrate(dataSource);

        backupService = new BackupService(dataSource, dataDir, backupDir);
    }

    @AfterEach
    void tearDown() {
        dataSource.close();
    }

    @Test
    void backupCreaZipYListBackupsLoVe() {
        Path file = backupService.backup();

        assertThat(Files.exists(file)).isTrue();
        assertThat(backupService.listBackups()).contains(file);
    }

    @Test
    void pruneOldBackupsConservaLos10MasRecientes() throws IOException {
        for (int i = 0; i < 12; i++) {
            Files.createFile(backupDir.resolve(String.format("backup_2026-01-%02d_10-00.zip", i + 1)));
        }

        backupService.pruneOldBackups();

        assertThat(backupService.listBackups()).hasSize(10);
    }

    @Test
    void backupIfNoneTodayNoCreaSiYaExisteUnoDeHoy() throws IOException {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Path existing = backupDir.resolve("backup_" + today + "_00-00.zip");
        Files.createFile(existing);

        backupService.backupIfNoneToday();

        assertThat(backupService.listBackups()).containsExactly(existing);
    }
}
