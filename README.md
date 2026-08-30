# Stock POS Desktop

Aplicación de escritorio para gestión de stock y punto de venta para un local pequeño.

## Tecnologías

- Java 21 LTS
- JavaFX
- Maven
- H2 Database (embebida en `~/.stockventa/data`)
- Flyway
- HikariCP
- OpenCSV
- Git

## Estado

Sistema completo: Stock, Punto de venta (POS), Configuración, Copias de seguridad y Reportes.

---

# Instalación y distribución

## Requisitos de desarrollo

- JDK 21 o superior
- Maven 3.6+

## Desarrollo

- Ejecutar en modo desarrollo:

  ```bash
  mvn javafx:run
  ```

- Correr los tests:

  ```bash
  mvn test
  ```

## Generar instalador

1. Ejecutar el script de build:

   ```bat
   scripts\build-installer.bat
   ```

2. El script compila con `mvn clean package`, prepara el staging y ejecuta
   `jpackage` para generar el instalador.

3. El instalador se genera en `target/installer/`.

4. El archivo `.exe` pesa aproximadamente 80-120 MB (incluye Java runtime embebido,
   así que no hace falta tener Java instalado en la PC destino).

> Nota: para generar el `.exe`/`.msi`, `jpackage` requiere **WiX Toolset 3.11**
> instalado en la máquina de build. Sin WiX se puede generar una app-image con
> `jpackage --type app-image`.

## Instalar en una PC nueva

- Copiar el `.exe` a la PC destino.
- Doble clic para instalar.
- Elegir carpeta de instalación.
- El programa aparece en el menú de inicio y en el escritorio.
- No requiere Java preinstalado.
- Los datos se guardan en `%USERPROFILE%\.stockventa`.

## Desinstalar

- Panel de control > Programas > Stock POS > Desinstalar.
- O ejecutar el desinstalador en la carpeta de instalación.

## Actualizar la aplicación

- Generar nuevo instalador con versión actualizada.
- Ejecutar el nuevo instalador (reemplaza archivos del programa).
- Los datos en `.stockventa` NO se borran.

## Distribución empresarial (MSI)

Para generar un `.msi` (instalación silenciosa / GPO), usar:

```bat
scripts\build-msi.bat
```

---

# Problemas comunes

## `jpackage not found`

- Verificar que JDK 21 esté instalado (no JRE).
- Verificar que `JAVA_HOME` apunte al JDK.
- `jpackage` está en `%JAVA_HOME%/bin/jpackage.exe`.

## El instalador no incluye JavaFX

- Verificar que `javafx-controls` y `javafx-fxml` estén en `pom.xml`.
- Verificar que `maven-dependency-plugin` los copie a `target/dependency`
  (se ejecuta en la fase `package`).

## La app instalada no encuentra la base de datos

- Verificar que `AppContext` use `System.getProperty("user.home")`.
- NO usar rutas relativas al directorio de instalación.

## El ícono no aparece

- Verificar que `src/main/resources/images/icon.ico` exista.
- Verificar que sea un archivo `.ico` válido (no un `.png` renombrado).

## WiX no está instalado

- `jpackage --type exe/msi` requiere WiX Toolset 3.11.
- Descargarlo desde <https://wixtoolset.org> e instalarlo.
- Alternativa: generar una app-image con `jpackage --type app-image`
  (no requiere WiX).
