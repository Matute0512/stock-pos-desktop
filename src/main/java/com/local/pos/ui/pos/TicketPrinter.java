package com.local.pos.ui.pos;

import com.local.pos.core.AlertUtil;
import com.local.pos.domain.model.PaymentMethod;
import com.local.pos.domain.model.Sale;
import com.local.pos.domain.model.SaleItem;
import com.local.pos.domain.service.SettingsService;
import javafx.print.PrinterJob;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class TicketPrinter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private TicketPrinter() {
    }

    public static VBox buildTicket(SettingsService settings, Sale sale, List<SaleItem> items) {
        VBox ticket = new VBox(4);
        ticket.getStyleClass().add("ticket");

        Label local = new Label(settings.getStoreName());
        local.getStyleClass().add("ticket-title");

        String date = sale.getSaleDate() != null ? sale.getSaleDate().format(DATE_FMT) : "";
        Label meta = new Label(date + "   N° " + sale.getSaleNumber());
        meta.getStyleClass().add("ticket-line");

        ticket.getChildren().addAll(local, meta, separator());

        for (SaleItem item : items) {
            Label line = new Label(String.format(
                "%-28s  %6s x $ %s",
                truncate(item.getProductName(), 28),
                item.getQuantity().stripTrailingZeros().toPlainString(),
                item.getUnitPrice().setScale(2, RoundingMode.HALF_UP).toPlainString()
            ));
            line.getStyleClass().add("ticket-line");
            ticket.getChildren().add(line);
        }

        ticket.getChildren().add(separator());

        addLine(ticket, "Subtotal", sale.getSubtotal());
        addLine(ticket, "Descuento", sale.getDiscountAmount());
        addLine(ticket, "TOTAL", sale.getTotal());

        ticket.getChildren().add(separator());

        addTextLine(ticket, "Método: " + methodLabel(sale));
        if (sale.getCashReceived() != null) {
            addTextLine(ticket, "Recibido: $ " + money(sale.getCashReceived()));
            addTextLine(ticket, "Cambio: $ " + money(sale.getChangeAmount()));
        }

        ticket.getChildren().add(separator());

        Label thanks = new Label(settings.getTicketFooter());
        thanks.getStyleClass().add("ticket-title");
        ticket.getChildren().add(thanks);

        return ticket;
    }

    public static void print(VBox ticket) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            AlertUtil.info("Imprimir ticket", "No se encontró ninguna impresora instalada.");
            return;
        }

        Window owner = ticket.getScene() != null ? ticket.getScene().getWindow() : null;
        boolean proceed = job.showPrintDialog(owner);
        if (!proceed) {
            return;
        }

        if (job.printPage(ticket)) {
            job.endJob();
        } else {
            AlertUtil.error("Imprimir ticket", "No se pudo imprimir el ticket.");
        }
    }

    private static void addLine(VBox ticket, String caption, BigDecimal value) {
        Label line = new Label(String.format("%-18s$ %s", caption, money(value)));
        line.getStyleClass().add("ticket-line");
        ticket.getChildren().add(line);
    }

    private static void addTextLine(VBox ticket, String text) {
        Label line = new Label(text);
        line.getStyleClass().add("ticket-line");
        ticket.getChildren().add(line);
    }

    private static Label separator() {
        Label sep = new Label("----------------------");
        sep.getStyleClass().add("ticket-line");
        return sep;
    }

    private static String money(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String methodLabel(Sale sale) {
        try {
            return PaymentMethod.valueOf(sale.getPaymentMethod()).getLabel();
        } catch (IllegalArgumentException e) {
            return sale.getPaymentMethod();
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
