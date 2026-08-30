# PROYECTO

Aplicación de escritorio Java 21 + JavaFX + Maven para un local pequeño.
Sistema completo: Stock, POS, Configuración, Backups, Reportes. 17 tests
en verde. Ahora necesitamos crear un INSTALADOR EJECUTABLE para Windows
que la dueña pueda instalar como cualquier programa (doble clic, ícono en
escritorio, sin necesitar Java instalado).

# TU TAREA

Configurar jpackage para generar un instalador Windows (.exe) que incluya:
- La aplicación completa
- Java Runtime embebido (sin necesidad de instalar Java)
- JavaFX Runtime
- Ícono personalizado
- Atajo en el escritorio
- Entrada en el menú de inicio
- Desinstalador

El instalador debe funcionar en una PC Windows limpia sin Java preinstalado.

# ANTES DE TOCAR CÓDIGO

Leé estos archivos:

- pom.xml (configuración actual de Maven)
- src/main/java/com/local/pos/PosApp.java (clase principal)
- src/main/java/com/local/pos/config/AppContext.java (rutas de datos)
- src/main/resources/images/ (si existe ícono, usarlo; si no, crear uno simple)
- docs/prompt_instalador.md (este archivo)

# CONVENCIONES OBLIGATORIAS

- Java 21 LTS
- NO modificar código de negocio ni tests
- Solo modificar pom.xml y agregar scripts de build
- El instalador final debe ser un archivo .exe autocontenido
- Las rutas de datos deben seguir funcionando (~/.stockventa)
- Documentar el proceso de build en README.md

# 1) Configurar plugins en pom.xml

Agregar al <build><plugins> del pom.xml:

a) Plugin para copiar dependencias a target/dependency:
<plugin>
<groupId>org.apache.maven.plugins</groupId>
<artifactId>maven-dependency-plugin</artifactId>
<version>3.6.1</version>
<executions>
<execution>
<id>copy-dependencies</id>
<phase>package</phase>
<goals><goal>copy-dependencies</goal></goals>
<configuration>
<outputDirectory>${project.build.directory}/dependency</outputDirectory>
<includeScope>runtime</includeScope>
</configuration>
</execution>
</executions>
</plugin>

b) Plugin para crear JAR ejecutable:
<plugin>
<groupId>org.apache.maven.plugins</groupId>
<artifactId>maven-jar-plugin</artifactId>
<version>3.3.0</version>
<configuration>
<archive>
<manifest>
<mainClass>com.local.pos.Main</mainClass>
<addClasspath>true</addClasspath>
<classpathPrefix>dependency/</classpathPrefix>
</manifest>
</archive>
</configuration>
</plugin>

c) Plugin exec-maven-plugin para ejecutar jpackage:
<plugin>
<groupId>org.codehaus.mojo</groupId>
<artifactId>exec-maven-plugin</artifactId>
<version>3.1.0</version>
</plugin>

# 2) Crear script de build (scripts/build-installer.bat)

Crear un script Windows batch que:

a) Ejecute mvn clean package
b) Verifique que jpackage esté disponible (debe estar en el JDK 21)
c) Ejecute jpackage con estos parámetros:
--name "Stock POS"
--app-version 1.0.0
--vendor "Tu Nombre"
--description "Sistema de stock y punto de venta"
--input target/dependency
--main-jar stock-pos-desktop-1.0.0.jar
--main-class com.local.pos.Main
--type exe
--win-dir-chooser
--win-menu
--win-shortcut
--icon src/main/resources/images/icon.ico
--dest target/installer

d) Si no existe icon.ico, crear uno simple (o usar un placeholder)

El script debe:
- Verificar que mvn package fue exitoso
- Verificar que el JAR se creó
- Mostrar progreso
- Manejar errores con mensajes claros

# 3) Crear ícono de la aplicación

Si no existe src/main/resources/images/icon.ico:
- Crear un ícono simple de 256x256 píxeles
- Puede ser un texto "SP" con fondo de color primario (#4f46e5)
- Guardarlo como icon.ico en src/main/resources/images/

Si no se puede crear el ícono, el script debe funcionar sin él
(jpackage usa un ícono por defecto).

# 4) Actualizar README.md

Agregar una sección "Instalación y distribución" con:

## Requisitos de desarrollo
- JDK 21 o superior
- Maven 3.6+

## Desarrollo
- Cómo ejecutar en modo desarrollo (mvn javafx:run)
- Cómo correr tests (mvn test)

## Generar instalador
- Ejecutar scripts/build-installer.bat
- El instalador se genera en target/installer/
- El archivo .exe pesa aproximadamente 80-120 MB (incluye Java)

## Instalar en una PC nueva
- Copiar el .exe a la PC destino
- Doble clic para instalar
- Elegir carpeta de instalación
- El programa aparece en el menú de inicio y escritorio
- No requiere Java preinstalado
- Los datos se guardan en %USERPROFILE%\.stockventa

## Desinstalar
- Panel de control > Programas > Stock POS > Desinstalar
- O ejecutar el desinstalador en la carpeta de instalación

## Actualizar la aplicación
- Generar nuevo instalador con versión actualizada
- Ejecutar el nuevo instalador (reemplaza archivos del programa)
- Los datos en .stockventa NO se borran

# 5) Verificación del instalador

Después de generar el instalador:

a) Verificar que el .exe se creó en target/installer/
b) Verificar que pesa entre 80-120 MB
c) (Opcional) Instalarlo en una VM Windows limpia para verificar que funciona

# 6) Script adicional (opcional): build-msi.bat

Crear un segundo script que genere un .msi en lugar de .exe:
--type msi
--win-upgrade-uuid <generar-uuid>

Esto es para distribución empresarial, pero no es obligatorio.

# 7) Documentación de troubleshooting

Agregar en README.md una sección de problemas comunes:

## jpackage not found
- Verificar que JDK 21 esté instalado (no JRE)
- Verificar que JAVA_HOME apunte al JDK
- jpackage está en %JAVA_HOME%/bin/jpackage.exe

## El instalador no incluye JavaFX
- Verificar que javafx-controls y javafx-fxml estén en pom.xml
- Verificar que maven-dependency-plugin los copie a target/dependency

## La app instalada no encuentra la base de datos
- Verificar que AppContext use System.getProperty("user.home")
- NO usar rutas relativas al directorio de instalación

## El ícono no aparece
- Verificar que icon.ico exista
- Verificar que sea un archivo .ico válido (no .png renombrado)

# VERIFICACIÓN MANUAL

1. Ejecutar scripts/build-installer.bat
2. Verificar que target/installer/Stock POS-1.0.0.exe existe
3. Verificar que pesa entre 80-120 MB
4. (Opcional) Instalarlo y verificar que:
    - Aparece en el menú de inicio
    - Tiene ícono en el escritorio
    - Abre correctamente
    - Crea la carpeta .stockventa en el perfil del usuario
    - Los datos persisten entre sesiones

# ENTREGA

- pom.xml actualizado con los plugins
- scripts/build-installer.bat funcional
- src/main/resources/images/icon.ico (o sin ícono si no se puede crear)
- README.md actualizado con sección de instalación
- Un único commit:
  "feat: empaquetado instalable con jpackage para distribución"
- Resumí al final: archivos creados/modificados, tamaño del instalador,
  y pasos para verificarlo.
