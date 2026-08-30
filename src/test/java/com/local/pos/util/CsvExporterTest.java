package com.local.pos.util;

import com.local.pos.domain.model.LowStockProduct;
import com.local.pos.domain.model.SalesSummary;
import com.local.pos.domain.model.TopSellingProduct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void exportSalesCreaCsvConHeadersYContenido() throws Exception {
        Path file = tempDir.resolve("ventas.csv");
        CsvExporter.exportSales(List.of(
            new SalesSummary(3, new BigDecimal("600.00"), new BigDecimal("370.00"),
                new BigDecimal("200.00"), new BigDecimal("1170.00"))
        ), file);

        assertThat(Files.exists(file)).isTrue();

        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertThat(content.charAt(0)).isEqualTo((char) 0xFEFF);
        assertThat(content).contains("Cantidad de ventas");
        assertThat(content).contains("\"3\";\"600.00\";\"370.00\";\"200.00\";\"1170.00\"");
        assertThat(content).contains("1170.00");
    }

    @Test
    void exportTopSellingCreaCsvConDatos() throws Exception {
        Path file = tempDir.resolve("top.csv");
        CsvExporter.exportTopSelling(List.of(
            new TopSellingProduct(1, "Yerba", new BigDecimal("5.00"), new BigDecimal("1000.00"))
        ), file);

        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertThat(content).contains("ID producto");
        assertThat(content).contains("Yerba");
        assertThat(content).contains("1000.00");
    }

    @Test
    void exportLowStockCreaCsvConDatos() throws Exception {
        Path file = tempDir.resolve("bajo.csv");
        CsvExporter.exportLowStock(List.of(
            new LowStockProduct(2, "Café", new BigDecimal("2.00"), new BigDecimal("4.00"))
        ), file);

        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertThat(content).contains("Café");
        assertThat(content).contains("Stock actual");
    }
}
