# PROYECTO

Aplicación de escritorio Java 21 + JavaFX + Maven para un local pequeño:
stock y punto de venta. H2 embebida en ~/.stockventa/data con Flyway e
HikariCP. UI en español (voseo), estética moderna propia (sidebar oscuro,
tarjetas claras, tokens CSS en .app-shell). El POS ya funciona: carrito,
cobro, ticket, historial.

# TU TAREA

Implementar dos módulos que hacen al sistema usable en la vida real:

1) CONFIGURACIÓN DEL LOCAL (nombre en el ticket, datos del negocio).
2) COPIAS DE SEGURIDAD: backup automático diario, backup manual y
   restauración con reinicio.

Sin romper nada de lo existente (Stock, POS, navegación, tests).

# ANTES DE TOCAR CÓDIGO

Leé estos archivos para convenciones e integración:

- src/main/java/com/local/pos/config/AppContext.java
- src/main/java/com/local/pos/PosApp.java
- src/main/java/com/local/pos/ui/main/MainShellView.java
- src/main/java/com/local/pos/ui/navigation/AppView.java
- src/main/java/com/local/pos/ui/views/PosView.java
- src/main/java/com/local/pos/ui/pos/TicketPrinter.java
- src/main/java/com/local/pos/ui/pos/PaymentDialog.java (patrón de diálogo)
- src/main/java/com/local/pos/ui/views/SettingsView NO EXISTE: hoy
  AppView.SETTINGS muestra un PlaceholderView desde MainShellView.
- src/main/java/com/local/pos/core/AlertUtil.java
- src/main/java/com/local/pos/persistence/dao/* (patrón DAO)
- src/main/resources/db/migration/V1__crear_tablas_iniciales.sql
  (la tabla SETTING ya existe: SETTING_KEY, SETTING_VALUE)
- src/main/resources/css/app.css

# CONVENCIONES OBLIGATORIAS

- Java 21, SIN dependencias nuevas en pom.xml.
- SQL con PreparedStatement + text blocks + try-with-resources;
  SQLException -> TechnicalException; reglas de negocio ->
  BusinessException con mensaje claro en español.
- UI en español con voseo; reutilizar clases CSS existentes (.view,
  .view-title, .view-subtitle, .card, .card-title, .card-text,
  .button-primary, .button-secondary, .button-danger, .form-label,
  .form-info, .search-field). Estilos nuevos al final de app.css
  reutilizando los tokens de .app-shell.
- NO crear migraciones nuevas: SETTING ya existe.
- NO abrir Stages: todo en la vista central y diálogos.
- Logger con org.slf4j (ya está en el pom) para operaciones de backup.
- Recordá la lección del arranque: NUNCA bloquear el JavaFX
  Application Thread. init() ya corre en un Task de fondo: todo
  trabajo pesado de backup va ahí o en acciones de usuario rápidas.

# 1) SettingsService (domain/service/)

- Lectura/escritura de SETTING con upsert:
  MERGE INTO SETTING (SETTING_KEY, SETTING_VALUE) KEY(SETTING_KEY)
  VALUES (?, ?)
- Claves gestionadas (con defaults si no existen):
  STORE_NAME        -> "Mi Local"
  STORE_ADDRESS     -> ""
  STORE_PHONE       -> ""
  TICKET_FOOTER     -> "Gracias por su compra"
  (SALE_NUMBER_NEXT ya existe: NO tocarla desde acá.)
- API: getStoreName(), getStoreAddress(), getStorePhone(),
  getTicketFooter(), saveAll(name, address, phone, footer).
- Caché en memoria que se refresca tras saveAll().

# 2) BackupService (domain/service/)

- Directorio de backups: ~/.stockventa/backups (crearlo si no existe).
- backup(): ejecuta sobre una conexión del pool:
  BACKUP TO '<dir>/backup_yyyy-MM-dd_HH-mm.zip'
  (backup caliente consistente de H2). Devuelve el Path creado.
- backupIfNoneToday(): si ningún archivo del directorio tiene la fecha
  de hoy en su nombre, crea uno. Loguea con slf4j. Nunca lanza
  excepción hacia afuera (catch + log de error): un fallo de backup
  NO debe impedir el arranque.
- listBackups(): List<Path> ordenados por nombre descendente.
- pruneOldBackups(): conserva solo los 10 más recientes.
- restore(Path zip):
  a) Cierra el pool de HikariCP (recibe un Runnable o referencia al
  shutdown del AppContext).
  b) Con java.util.zip: recorre entradas del zip; toda entrada que
  termine en .mv.db se escribe en ~/.stockventa/data reemplazando
  el archivo existente (escribir primero a .tmp y luego renombrar).
  c) No reabre nada: la app se reinicia.

# 3) SettingsView (ui/views/)

Reemplaza el PlaceholderView de AppView.SETTINGS. Layout con dos cards:

Card "Datos del local":
- TextFields: Nombre del local, Dirección, Teléfono, Texto de cierre
  del ticket (pre-cargados desde SettingsService).
- Botón [Guardar] (.button-primary) -> saveAll -> AlertUtil.info
  "Configuración guardada".

Card "Copias de seguridad":
- Label .form-info con el directorio de backups y cantidad existente.
- Botón [Hacer copia ahora] (.button-secondary): llama backup() +
  pruneOldBackups() y muestra AlertUtil.info con el archivo creado.
- Botón [Restaurar copia...] (.button-danger): FileChooser filtrando
  *.zip apuntando al directorio de backups; AlertUtil.confirm con
  aviso fuerte ("Se reemplazarán TODOS los datos actuales y la
  aplicación se cerrará"); si confirma: restore(zip) y
  Platform.exit() con mensaje previo de "Reiniciá la aplicación para
  completar la restauración".

# 4) Integración

- AppContext: crear SettingsService y BackupService en init();
  getters. Al FINAL de init() (ya en el hilo de fondo) llamar
  backupService.backupIfNoneToday().
- MainShellView.initializeWithData: registrar SettingsView real con
  settingsService + backupService; y pasar settingsService a PosView
  (ajustar su constructor) para que el ticket use datos reales.
- TicketPrinter: reemplazar el "Mi Local" y el footer hardcodeados por
  los valores de SettingsService. El resto del ticket no cambia.
- AppView.REPORTS sigue con PlaceholderView: NO implementar reportes ahora.

# 5) Testing

- JUnit 5 con H2 en memoria (mismo patrón que SaleServiceTest):
  SettingsServiceTest: saveAll persiste y get refleja; defaults cuando
  no hay filas.
  BackupServiceTest con @TempDir: backup() crea el zip; listBackups lo
  ve; con 12 archivos fake, pruneOldBackups deja 10.
  (restore no se testa automático: requiere reinicio.)
- `mvn test` debe quedar en verde, incluidos los 5 tests del POS.

# VERIFICACIÓN MANUAL

- mvn -q clean compile && mvn test && mvn javafx:run
- Configuración: cargar nombre "Almacén de María", guardar, vender algo
  e imprimir ticket: debe salir el nombre real y el footer configurado.
- Backup manual: botón "Hacer copia ahora" -> aparece el zip en
  ~/.stockventa/backups.
- Restore: crear un producto "PRUEBA RESTORE", hacer backup, borrar el
  producto (desactivar), restaurar el backup... (ojo: el backup se hizo
  CON el producto; para probar bien: backup ANTES de crear el producto,
  crear producto, restaurar, reiniciar, y el producto NO debe estar).
- Reinicio: al arrancar de nuevo, si ya hay backup de hoy no crea otro.

# ENTREGA

- Código completo y compilable, sin TODOs.
- Un único commit:
  "feat: configuración del local y copias de seguridad con restauración"
- Resumí al final: archivos creados/modificados, tests ejecutados y
  pasos de verificación manual.
