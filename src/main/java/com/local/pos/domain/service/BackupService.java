package com.local.pos.domain.service;

import com.local.pos.core.TechnicalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    private static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_BACKUPS = 10;
    private static final String PREFIX = "backup_";

    private final DataSource dataSource;
    private final Path dataDir;
    private final Path backupDir;

    public BackupService(DataSource dataSource, Path dataDir, Path backupDir) {
        this.dataSource = dataSource;
        this.dataDir = dataDir;
        this.backupDir = backupDir;
    }

    public Path getBackupDir() {
        return backupDir;
    }

    public Path backup() {
        try {
            Files.createDirectories(backupDir);
        } catch (IOException e) {
            throw new TechnicalException("No se pudo crear el directorio de backups", e);
        }

        Path file = backupDir.resolve(PREFIX + LocalDateTime.now().format(FILE_FMT) + ".zip");

        String path = file.toAbsolutePath().toString()
            .replace('\\', '/')
            .replace("'", "''");
        String sql = "BACKUP TO '" + path + "'";

        try (Connection con = dataSource.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new TechnicalException("Error al crear la copia de seguridad", e);
        }

        log.info("Copia de seguridad creada: {}", file);
        return file;
    }

    public void backupIfNoneToday() {
        try {
            Files.createDirectories(backupDir);

            String today = LocalDate.now().format(DAY_FMT);
            try (Stream<Path> stream = Files.list(backupDir)) {
                boolean hasToday = stream
                    .filter(path -> path.getFileName().toString().startsWith(PREFIX + today))
                    .findAny()
                    .isPresent();

                if (hasToday) {
                    log.info("Ya existe una copia de seguridad de hoy, se omite.");
                    return;
                }
            }

            backup();
            pruneOldBackups();

        } catch (Exception e) {
            // Un fallo de backup jamás debe impedir el arranque de la app
            log.error("No se pudo crear la copia de seguridad automática", e);
        }
    }

    public List<Path> listBackups() {
        if (!Files.isDirectory(backupDir)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(backupDir)) {
            return stream
                .filter(path -> path.getFileName().toString().startsWith(PREFIX)
                    && path.getFileName().toString().endsWith(".zip"))
                .sorted(Comparator.comparing((Path path) -> path.getFileName().toString()).reversed())
                .toList();
        } catch (IOException e) {
            throw new TechnicalException("Error al listar las copias de seguridad", e);
        }
    }

    public void pruneOldBackups() {
        List<Path> backups = listBackups();
        if (backups.size() <= MAX_BACKUPS) {
            return;
        }

        for (int i = MAX_BACKUPS; i < backups.size(); i++) {
            try {
                Files.deleteIfExists(backups.get(i));
                log.info("Copia de seguridad antigua eliminada: {}", backups.get(i));
            } catch (IOException e) {
                log.warn("No se pudo eliminar la copia antigua {}", backups.get(i), e);
            }
        }
    }

    /**
     * Restaura una copia de seguridad: cierra el pool y reemplaza los
     * archivos .mv.db. La app debe reiniciarse después.
     */
    public void restore(Path zip, Runnable shutdown) {
        shutdown.run();

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.getName().endsWith(".mv.db")) {
                    continue;
                }

                Path target = dataDir.resolve(Path.of(entry.getName()).getFileName());
                Path tmp = dataDir.resolve(target.getFileName() + ".tmp");

                Files.copy(zis, tmp, StandardCopyOption.REPLACE_EXISTING);
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                log.info("Restaurado: {}", target);
            }
        } catch (IOException e) {
            throw new TechnicalException("Error al restaurar la copia de seguridad", e);
        }
    }
}
