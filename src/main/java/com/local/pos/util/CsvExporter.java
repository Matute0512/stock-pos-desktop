package com.local.pos.util;

import com.local.pos.core.TechnicalException;
import com.local.pos.domain.model.LowStockProduct;
import com.local.pos.domain.model.SalesSummary;
import com.local.pos.domain.model.StockValuation;
import com.local.pos.domain.model.TopSellingProduct;
import com.opencsv.CSVWriter;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CsvExporter {

    private CsvExporter() {
    }

    public static void exportSales(List<SalesSummary> data, Path file) {
        String[] header = {"Cantidad de ventas", "Total efectivo", "Total tarjeta", "Total otros", "Total general"};
        List<String[]> rows = new ArrayList<>();
        for (SalesSummary s : data) {
            rows.add(new String[]{
                String.valueOf(s.totalSales()),
                money(s.cashTotal()),
                money(s.cardTotal()),
                money(s.otherTotal()),
                money(s.grandTotal())
            });
        }
        write(file, header, rows);
    }

    public static void exportTopSelling(List<TopSellingProduct> data, Path file) {
        String[] header = {"ID producto", "Producto", "Cantidad vendida", "Total vendido"};
        List<String[]> rows = new ArrayList<>();
        for (TopSellingProduct p : data) {
            rows.add(new String[]{
                String.valueOf(p.productId()),
                p.productName(),
                p.quantitySold().stripTrailingZeros().toPlainString(),
                money(p.totalRevenue())
            });
        }
        write(file, header, rows);
    }

    public static void exportStockValuation(List<StockValuation> data, Path file) {
        String[] header = {"ID producto", "Producto", "Stock", "Costo", "Precio", "Valor a costo", "Valor a precio"};
        List<String[]> rows = new ArrayList<>();
        for (StockValuation v : data) {
            rows.add(new String[]{
                String.valueOf(v.productId()),
                v.productName(),
                v.stockQty().stripTrailingZeros().toPlainString(),
                money(v.cost()),
                money(v.price()),
                money(v.valueAtCost()),
                money(v.valueAtPrice())
            });
        }
        write(file, header, rows);
    }

    public static void exportLowStock(List<LowStockProduct> data, Path file) {
        String[] header = {"ID producto", "Producto", "Stock actual", "Stock mínimo"};
        List<String[]> rows = new ArrayList<>();
        for (LowStockProduct p : data) {
            rows.add(new String[]{
                String.valueOf(p.productId()),
                p.productName(),
                p.stockQty().stripTrailingZeros().toPlainString(),
                p.minStockQty().stripTrailingZeros().toPlainString()
            });
        }
        write(file, header, rows);
    }

    private static void write(Path file, String[] header, List<String[]> rows) {
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            // BOM para que Excel interprete UTF-8 y muestre bien las tildes
            writer.write(0xFEFF);

            CSVWriter csvWriter = new CSVWriter(writer, ';',
                CSVWriter.DEFAULT_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);

            csvWriter.writeNext(header);
            rows.forEach(csvWriter::writeNext);
            csvWriter.flush();

        } catch (IOException e) {
            throw new TechnicalException("No se pudo exportar el CSV", e);
        }
    }

    private static String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
