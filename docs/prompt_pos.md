# PROYECTO

Aplicación de escritorio Java 21 + JavaFX + Maven para un local pequeño:
gestión de stock y punto de venta. Base de datos H2 embebida (archivo en
~/.stockventa/data) con migraciones Flyway y pool HikariCP. UI 100% en
español (voseo), estética moderna propia: sidebar oscuro, fondo claro,
tarjetas blancas con sombra, botones redondeados. NO usar look genérico
de JavaFX.

# TU TAREA

Implementar el módulo PUNTO DE VENTA (POS) completo sobre el código
existente, sin romper nada de lo que ya funciona (módulo Stock,
navegación, base de datos, diálogos existentes).

# ANTES DE TOCAR CÓDIGO

Leé estos archivos para absorber convenciones y puntos de integración:

- pom.xml
- src/main/java/com/local/pos/PosApp.java
- src/main/java/com/local/pos/config/AppContext.java
- src/main/java/com/local/pos/ui/main/MainShellView.java
- src/main/java/com/local/pos/ui/navigation/AppView.java
- src/main/java/com/local/pos/ui/navigation/NavigationService.java
- src/main/java/com/local/pos/ui/views/StockView.java
- src/main/java/com/local/pos/ui/views/PosView.java (placeholder a reemplazar)
- src/main/java/com/local/pos/ui/stock/ProductFormDialog.java
- src/main/java/com/local/pos/ui/stock/StockAdjustDialog.java
- src/main/java/com/local/pos/ui/stock/StockHistoryDialog.java
- src/main/java/com/local/pos/persistence/dao/ProductDao.java
- src/main/java/com/local/pos/persistence/dao/StockMovementDao.java
- src/main/java/com/local/pos/domain/service/ProductService.java
- src/main/java/com/local/pos/domain/service/StockService.java
- src/main/java/com/local/pos/core/BusinessException.java
- src/main/java/com/local/pos/core/TechnicalException.java
- src/main/java/com/local/pos/core/AlertUtil.java
- src/main/java/com/local/pos/domain/model/MovementType.java
- src/main/resources/db/migration/V1__crear_tablas_iniciales.sql
- src/main/resources/css/app.css

# CONVENCIONES OBLIGATORIAS

- Java 21. NO agregar dependencias nuevas al pom.xml.
- Dinero SIEMPRE BigDecimal (scale 2, RoundingMode.HALF_UP).
  Prohibido double/float para montos o cantidades.
- SQL con PreparedStatement + text blocks + try-with-resources.
  SQLException se envuelve en TechnicalException.
- Errores de reglas de negocio -> BusinessException con mensaje claro
  en español ("Stock insuficiente para: X").
- DAOs: constructor(DataSource); transacción interna cuando tocan
  varias tablas (ver ProductDao.insertWithInitialStock como referencia).
- Servicios validan y orquestan. Una venta = UNA transacción con
  setAutoCommit(false)/commit/rollback.
- Diálogos: mismo patrón que ProductFormDialog/StockAdjustDialog
  (Dialog<T>, setResultConverter, stylesheet /css/app.css,
  clase CSS .modern-dialog).
- UI: reutilizar clases CSS existentes (.view, .view-title,
  .view-subtitle, .toolbar-card, .search-field, .button-primary,
  .button-secondary, .button-danger, .card, .card-title, .card-text,
  .modern-table, .table-card, .form-label, .form-info, .cell-warning).
  Los tokens de color viven en .app-shell (-primary, -bg, -surface,
  -text, -text-muted, -border, -success, -warning, -danger).
  Los estilos nuevos del POS se agregan AL FINAL de app.css
  reutilizando esos tokens.
- Textos de UI y mensajes en español con voseo ("Seleccioná", "Ingresá").
- NO modificar la migración V1 ni crear V2: las tablas SALE, SALE_ITEM
  y SETTING ya existen.
- No abrir Stages nuevos: todo vive en la vista central y en diálogos.
- Las operaciones de DB son locales y rápidas; ejecutarlas en el hilo
  de UI como hace StockView (consistencia), salvo que detectes bloqueos.
- Si hay ambigüedad, elegí la opción más simple y consistente con el
  código existente.

# ESQUEMA RELEVANTE (ya creado en V1, no lo modifiques)

SALE(ID, SALE_NUMBER, SALE_DATE, SUBTOTAL, DISCOUNT_AMOUNT, TOTAL,
PAYMENT_METHOD CHECK IN ('CASH','CARD','TRANSFER','OTHER'),
CASH_RECEIVED, CHANGE_AMOUNT,
STATUS CHECK IN ('COMPLETED','CANCELLED','REFUNDED'), NOTES)
SALE_ITEM(ID, SALE_ID, PRODUCT_ID, PRODUCT_NAME, QUANTITY,
UNIT_PRICE, LINE_TOTAL)
SETTING(SETTING_KEY, SETTING_VALUE) con fila ('SALE_NUMBER_NEXT','1')
STOCK_MOVEMENT: usar MOVEMENT_TYPE='SALE' al descontar stock.

# FUNCIONALIDAD A IMPLEMENTAR

## 1) PosView (reemplazar el placeholder de ui/views/PosView.java)

- Header con .view-title "Punto de venta" y .view-subtitle descriptivo.
- Barra de búsqueda .search-field con foco automático al entrar a la
  vista y después de cada producto agregado. Debe soportar lector de
  código de barras USB (escribe dígitos + Enter).
    - Mientras se escribe: mostrar tabla de coincidencias (productos
      activos por nombre/barcode/SKU, reutilizar ProductService) con
      columnas Nombre, Código, Precio, Stock disponible. Doble clic o
      botón "Agregar" suma al carrito.
    - Enter con barcode exacto: agrega directo al carrito y limpia el campo.
- Carrito: TableView con columnas Nombre, Cantidad (celda editable:
  TextField que confirma con Enter y valida contra stock disponible),
  Precio unitario, Total línea y acción Quitar. Si el producto ya está
  en el carrito, incrementar cantidad (validando stock).
- Panel derecho tipo .card con totales: Subtotal, campo Descuento
  (monto, por defecto 0, no puede superar el subtotal) y TOTAL en
  tipografía grande. Botones grandes:
  [Cobrar] (.button-primary, atajo F4),
  [Cancelar venta] (.button-danger, atajo ESC, pide confirmación con
  AlertUtil.confirm antes de vaciar el carrito),
  [Historial de ventas] (.button-secondary).
- Atajos de teclado implementados con un event filter de KeyEvent sobre
  la raíz de la vista (F4, ESC). Enter lo maneja el campo de búsqueda.

## 2) Modelo de carrito

- Clase en ui/pos (por ejemplo CartItem): productId, name, unitPrice,
  quantity, availableStock; método lineTotal(). Propiedades observables
  para bindear la tabla.

## 3) PaymentDialog (ui/pos/)

- Muestra el TOTAL a cobrar.
- ComboBox de método de pago con labels en español:
  Efectivo / Tarjeta / Transferencia.
- Si Efectivo: campo "Recibido" + botones rápidos (Exacto, 1000, 2000,
  5000, 10000) y label de Cambio calculado en vivo; validar
  recibido >= total antes de confirmar.
- Tarjeta/Transferencia: sin campo de recibido ni vuelto.
- Devuelve un record PaymentResult(PaymentMethod method,
  BigDecimal cashReceived). Crear enum PaymentMethod en domain/model
  con labels en español.

## 4) SaleService (domain/service/)

- createSale(List<CartItem> items, BigDecimal discount,
  PaymentMethod method, BigDecimal cashReceived) en UNA transacción:
  a) Número de venta: SELECT SETTING_VALUE FROM SETTING WHERE
  SETTING_KEY='SALE_NUMBER_NEXT' FOR UPDATE, luego UPDATE +1.
  Formato: String.format("%08d", n).
  b) Por cada item: SELECT del producto FOR UPDATE; validar que esté
  activo y que el stock alcance (comparando contra la suma ya
  cargada en el carrito); insertar SALE_ITEM con snapshot de
  PRODUCT_NAME; descontar PRODUCT.STOCK_QTY; insertar
  STOCK_MOVEMENT (MOVEMENT_TYPE='SALE', QUANTITY, RESULT_STOCK_QTY,
  REFERENCE_TYPE='SALE', REFERENCE_ID=idVenta,
  REASON='Venta N° <numero>').
  c) Calcular subtotal, total = subtotal - descuento, y change
  (solo CASH: recibido - total).
  d) Insertar SALE con STATUS='COMPLETED' y commitear.
  Rollback ante cualquier excepción.
- listSalesToday(): ventas del día ordenadas descendentes.
- getSaleItems(saleId): items de una venta.
- Crear SaleDao y SaleItemDao en persistence/dao siguiendo el patrón
  de los DAOs existentes, y modelos Sale y SaleItem en domain/model.

## 5) SalesHistoryDialog (ui/pos/)

- Tabla de ventas del día: Número, Fecha/Hora, Método, Total.
- Al seleccionar una venta, tabla inferior con sus items.
- Botón Cerrar.

## 6) Ticket imprimible (ui/pos/TicketPrinter o util/PrinterService)

- Construir un VBox con: nombre del local ("Mi Local" por ahora),
  fecha/hora, número de venta, líneas de items (nombre, cantidad,
  total), subtotal, descuento, TOTAL, método de pago, recibido y
  cambio si efectivo, y "Gracias por su compra".
- Imprimir con javafx.print.PrinterJob mostrando el diálogo de
  impresión; si no hay impresora instalada, AlertUtil.info y continuar.
- Después de cobrar con éxito: AlertUtil.confirm("¿Imprimir ticket?").

## 7) Integración

- AppContext: instanciar SaleDao/SaleItemDao y SaleService en init();
  agregar getter getSaleService().
- MainShellView.initializeWithData: registrar la PosView real con
  productService y saleService, reemplazando el placeholder de
  AppView.POS. No tocar el resto de la navegación.

# TESTING

- JUnit 5 + H2 en memoria. En el setup: crear DataSource
  jdbc:h2:mem:test;DB_CLOSE_DELAY=-1 y ejecutar
  FlywayMigrator.migrate(dataSource) (lee V1 del classpath).
- SaleServiceTest debe cubrir:
    1) venta correcta descuenta stock y genera movimientos 'SALE';
    2) total y cambio de efectivo correctos;
    3) stock insuficiente lanza BusinessException y hace rollback
       (el stock queda intacto);
    4) descuento mayor al subtotal rechazado;
    5) números de venta incrementales (00000001, 00000002).
- `mvn test` debe pasar en verde.

# VERIFICACIÓN MANUAL

- `mvn -q clean compile` sin errores.
- `mvn javafx:run` y recorrer este flujo completo:
  cargar un producto en Stock -> ir a Vender -> buscar por nombre y
  por barcode -> agregar al carrito -> cambiar cantidad -> quitar un
  item -> aplicar descuento -> cobrar en efectivo verificando vuelto ->
  cobrar otra venta con tarjeta -> revisar Historial de ventas ->
  verificar en Stock que el stock quedó descontado y en Historial del
  producto que aparecen los movimientos 'SALE'.

# ENTREGA

- Código completo y compilable, sin TODOs ni placeholders.
- Un único commit final:
  "feat: punto de venta con carrito, cobro, ticket e historial".
- Al terminar, resumí: archivos creados, archivos modificados,
  pruebas ejecutadas y cómo verificarlo manualmente.
