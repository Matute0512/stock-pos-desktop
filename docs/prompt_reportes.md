# PROYECTO

Aplicación de escritorio Java 21 + JavaFX + Maven para un local pequeño:
stock y punto de venta. H2 embebida en ~/.stockventa/data con Flyway e
HikariCP. UI en español (voseo), estética moderna propia. El sistema ya
tiene Stock, POS, Configuración y Backups funcionando.

# TU TAREA

Implementar el módulo REPORTES con cuatro vistas simples pero útiles para
la dueña del local:

1. Ventas del día (resumen general).
2. Productos más vendidos (ranking).
3. Valor de stock (inversión y potencial de venta).
4. Productos bajo stock mínimo (alertas de reposición).

Todos con exportación a CSV usando OpenCSV (ya está en el pom).

Sin romper nada de lo existente (Stock, POS, Configuración, Backups, tests).

# ANTES DE TOCAR CÓDIGO

Leé estos archivos para convenciones e integración:

- src/main/java/com/local/pos/config/AppContext.java
- src/main/java/com/local/pos/ui/main/MainShellView.java
- src/main/java/com/local/pos/ui/navigation/AppView.java
- src/main/java/com/local/pos/persistence/dao/SaleDao.java (patrón)
- src/main/java/com/local/pos/persistence/dao/SaleItemDao.java
- src/main/java/com/local/pos/persistence/dao/ProductDao.java
- src/main/java/com/local/pos/domain/service/SaleService.java (patrón)
- src/main/java/com/local/pos/domain/service/ProductService.java
- src/main/java/com/local/pos/core/AlertUtil.java
- src/main/resources/db/migration/V1__crear_tablas_iniciales.sql
  (tablas SALE, SALE_ITEM, PRODUCT ya existen)
- src/main/resources/css/app.css
- pom.xml (com.opencsv:opencsv ya está)

# CONVENCIONES OBLIGATORIAS

- Java 21, SIN dependencias nuevas.
- SQL con PreparedStatement + text blocks + try-with-resources;
  SQLException -> TechnicalException.
- UI en español con voseo; reutilizar clases CSS existentes (.view,
  .view-title, .view-subtitle, .card, .card-title, .card-text,
  .button-primary, .button-secondary, .modern-table, .table-card,
  .form-label, .form-info). Estilos nuevos al final de app.css.
- NO crear migraciones nuevas: las tablas ya existen.
- Dinero SIEMPRE BigDecimal (scale 2, RoundingMode.HALF_UP).
- Para exportar CSV: usar FileChooser + OpenCSV (CSVWriter).
- Los reportes son de lectura (solo SELECT), sin transacciones.

# ESQUEMA RELEVANTE (no modificar)

SALE(ID, SALE_NUMBER, SALE_DATE, SUBTOTAL, DISCOUNT_AMOUNT, TOTAL,
PAYMENT_METHOD, CASH_RECEIVED, CHANGE_AMOUNT, STATUS, NOTES)
SALE_ITEM(ID, SALE_ID, PRODUCT_ID, PRODUCT_NAME, QUANTITY,
UNIT_PRICE, LINE_TOTAL)
PRODUCT(ID, SKU, BARCODE, NAME, COST, PRICE, STOCK_QTY, MIN_STOCK_QTY, ACTIVE)

# 1) ReportService (domain/service/)

Métodos de consulta (todos con LocalDate para rangos):

a) getSalesSummary(LocalDate date):
- Cantidad total de ventas del día (STATUS='COMPLETED').
- Total vendido en efectivo (PAYMENT_METHOD='CASH').
- Total vendido en tarjeta (PAYMENT_METHOD='CARD').
- Total vendido en otros métodos.
- Total general.
  Devolver un record SalesSummary(int totalSales, BigDecimal cashTotal,
  BigDecimal cardTotal, BigDecimal otherTotal, BigDecimal grandTotal).

b) getTopSellingProducts(LocalDate fromDate, LocalDate toDate, int limit):
- JOIN entre SALE_ITEM y SALE (filtrando por fecha y STATUS='COMPLETED').
- GROUP BY PRODUCT_ID, PRODUCT_NAME.
- SUM(QUANTITY) como quantitySold.
- SUM(LINE_TOTAL) como totalRevenue.
- ORDER BY quantitySold DESC LIMIT limit.
  Devolver List<TopSellingProduct> con record TopSellingProduct(long productId,
  String productName, BigDecimal quantitySold, BigDecimal totalRevenue).

c) getStockValuation():
- Solo productos activos.
- Calcular para cada uno:
    * valueAtCost = COST * STOCK_QTY
    * valueAtPrice = PRICE * STOCK_QTY
      Devolver List<StockValuation> con record StockValuation(long productId,
      String productName, BigDecimal stockQty, BigDecimal cost, BigDecimal price,
      BigDecimal valueAtCost, BigDecimal valueAtPrice).

d) getLowStockProducts():
- Solo productos activos.
- WHERE STOCK_QTY < MIN_STOCK_QTY.
  Devolver List<LowStockProduct> con record LowStockProduct(long productId,
  String productName, BigDecimal stockQty, BigDecimal minStockQty).

# 2) ReportDao (persistence/dao/)

Implementar los SELECTs necesarios para los métodos del servicio.
Patrón estándar: Connection del pool, PreparedStatement, map a records.
No necesita transacciones (solo lectura).

# 3) CsvExporter (util/)

Clase utilitaria con métodos estáticos:

- exportSales(List<SalesSummary> data, Path file): exporta a CSV con headers.
- exportTopSelling(List<TopSellingProduct> data, Path file).
- exportStockValuation(List<StockValuation> data, Path file).
- exportLowStock(List<LowStockProduct> data, Path file).

Usar CSVWriter de OpenCSV. Formato UTF-8 con BOM para que Excel lo abra bien.
Separador: punto y coma (;) para compatibilidad con Excel en español.

# 4) ReportView (ui/views/)

Reemplazar el PlaceholderView de AppView.REPORTS. Layout con cuatro cards:

Card "Ventas del día":
- Label con la fecha actual.
- Tabla resumen: Cantidad de ventas, Total efectivo, Total tarjeta,
  Total otros, Total general.
- Botón [Exportar CSV] (.button-secondary) -> FileChooser -> exportSales.

Card "Productos más vendidos":
- DatePicker para rango de fechas (últimos 7 días por defecto).
- ComboBox para límite (5, 10, 20, 50 productos).
- Botón [Consultar] (.button-secondary).
- Tabla con columnas: Producto, Cantidad vendida, Total vendido.
- Botón [Exportar CSV] (deshabilitado hasta que haya datos).

Card "Valor de stock":
- Tabla con columnas: Producto, Stock, Costo, Precio, Valor a costo,
  Valor a precio.
- Label resumen al final: Total inversión (suma valueAtCost),
  Total potencial de venta (suma valueAtPrice), Ganancia estimada (diferencia).
- Botón [Exportar CSV].

Card "Productos bajo stock mínimo":
- Tabla con columnas: Producto, Stock actual, Stock mínimo, Diferencia.
- Botón [Exportar CSV].

Todas las tablas usan .modern-table y se cargan al entrar a la vista
(ventana lazy: cada card carga su consulta cuando el usuario la toca
o al entrar, según sea más simple).

# 5) Integración

- AppContext: crear ReportService en init(); getter getReportService().
- MainShellView.initializeWithData: registrar ReportView real con
  reportService; reemplazar el PlaceholderView de AppView.REPORTS.
- Los records de reportes (SalesSummary, TopSellingProduct, etc.) van en
  domain/model/ para que sean visibles en todo el proyecto.

# 6) Testing

- JUnit 5 con H2 en memoria (mismo patrón que tests existentes).
- ReportServiceTest:
  a) getSalesSummary con ventas de distintos métodos de pago.
  b) getTopSellingProducts con varias ventas y verificaciones de ranking.
  c) getStockValuation con productos a distintos precios.
  d) getLowStockProducts con productos sobre y bajo el mínimo.
- CsvExporterTest con @TempDir: exportar una lista y verificar que el
  CSV se crea y tiene el contenido esperado.
- `mvn test` debe quedar en verde, incluidos los 10 tests anteriores.

# VERIFICACIÓN MANUAL

- mvn -q clean compile && mvn test && mvn javafx:run
- Crear varios productos con distintos stocks y precios.
- Hacer varias ventas (algunas en efectivo, otras en tarjeta).
- Ir a Reportes:
    * Ventas del día: debe mostrar el resumen correcto.
    * Productos más vendidos: cambiar fechas, verificar ranking.
    * Valor de stock: verificar cálculos de inversión y potencial.
    * Bajo stock: debe mostrar solo los que están bajo el mínimo.
- Exportar cada reporte a CSV y abrirlos en Excel (deben verse bien con
  tildes y separadores correctos).

# ENTREGA

- Código completo y compilable, sin TODOs.
- Un único commit:
  "feat: reportes de ventas, stock y exportación CSV"
- Resumí al final: archivos creados/modificados, tests ejecutados y
  pasos de verificación manual.
