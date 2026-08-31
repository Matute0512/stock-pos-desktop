# Manual de usuario — Stock y Punto de Venta

Versión 1.0

---

## 1. ¿Qué es este sistema?

Es el programa de gestión de tu local. Te sirve para:

- Cargar productos y controlar el stock.
- Vender rápido, con lector de código de barras.
- Imprimir tickets.
- Ver cuánto vendiste y qué te falta reponer.
- Proteger tu información con copias de seguridad automáticas.

Funciona sin internet. Toda tu información queda guardada en tu computadora.

---

## 2. Instalación

1. Hacé doble clic en el instalador (`Stock POS-1.0.0.exe`).
2. Aceptá y elegí la carpeta de instalación (podés dejar la que propone).
3. Al terminar, vas a tener un ícono en el escritorio y en el menú de inicio.

No necesitás instalar nada más: el programa ya trae todo lo necesario.

---

## 3. Primer arranque

1. Doble clic en el ícono del escritorio.
2. La primera vez, el programa crea solo la carpeta con tu información.
3. Vas a ver la pantalla principal con el menú lateral.

> **Importante:** tu información se guarda en una carpeta llamada
> `.stockventa` dentro de tu usuario de Windows. **No la borres nunca**:
> ahí están tus productos, tus ventas y tus copias de seguridad.

---

## 4. Pantalla principal

A la izquierda está el menú:

| Opción | Para qué sirve |
|---|---|
| **Panel** | Pantalla de inicio. |
| **Stock** | Cargar y administrar productos. |
| **Vender** | Caja: buscar productos, cobrar e imprimir ticket. |
| **Reportes** | Ventas del día, más vendidos, valor del stock, reposición. |
| **Configuración** | Datos del local y copias de seguridad. |

Hacé clic en una opción para cambiar de pantalla. Arriba siempre ves el
nombre de la pantalla donde estás.

---

## 5. Vender (punto de venta)

### 5.1 Buscar o escanear un producto

- Al entrar a **Vender**, el cursor ya está en el buscador.
- **Con lector:** escaneá el código de barras. El producto se agrega
  solo al carrito y el buscador queda listo para el siguiente.
- **A mano:** escribí parte del nombre (por ejemplo `gaseo`) y elegí de
  la lista con doble clic o con el botón **Agregar**.

### 5.2 El carrito

- Muestra producto, cantidad, precio y total por línea.
- **Cambiar cantidad:** hacé clic sobre la cantidad, escribí el número
  y presioná **Enter**.
- **Quitar un producto:** usá la acción de quitar de la fila.
- El sistema **no te deja vender más unidades que el stock disponible**.

### 5.3 Descuento

- En el panel de totales hay un campo **Descuento**.
- Escribí el monto en pesos y el total se recalcula solo.
- No puede ser mayor al total de la venta.

### 5.4 Cobrar

1. Tocá **Cobrar** (o la tecla **F4**).
2. Elegí el método de pago: **Efectivo**, **Tarjeta** o **Transferencia**.
3. Si es efectivo: escribí cuánto recibiste, o usá los botones rápidos
   (Exacto, 1000, 2000, 5000, 10000). El **cambio** se calcula solo.
4. Confirmá.

### 5.5 Ticket

Después de cobrar, el sistema te pregunta si querés imprimir el ticket.
El ticket lleva el nombre del local y el texto de cierre que
configuraste en **Configuración**.

### 5.6 Cancelar una venta

- Tecla **ESC** o botón **Cancelar venta**.
- Te pide confirmación antes de vaciar el carrito.
- Si ya confirmaste el cobro, la venta queda registrada: no se puede
  "borrar", porque el stock y los números deben quedar verdaderos.

### 5.7 Historial de ventas

- Botón **Historial de ventas**.
- Muestra las ventas del día con número, hora, método y total.
- Tocando una venta ves los productos que incluyó.

---

## 6. Stock

### 6.1 Buscar

El buscador filtra por nombre, código de barras o SKU mientras escribís.

### 6.2 Cargar un producto nuevo

1. Botón **Nuevo producto**.
2. Completá: **Nombre** (obligatorio), código de barras, SKU, costo,
   precio, stock inicial y stock mínimo.
3. **Guardar**.

El **stock mínimo** es el número que te avisa que hay que reponer.

### 6.3 Editar un producto

- Doble clic sobre el producto, o seleccionarlo y tocar **Editar**.
- Podés cambiar nombre, códigos, precios y stock mínimo.
- El **stock no se cambia acá**: se cambia con **Ajustar stock**, para
  que siempre quede registrado el motivo.

### 6.4 Ajustar stock

1. Seleccioná el producto y tocá **Ajustar stock**.
2. Elegí el tipo:
    - **Ingreso / Compra** o **Devolución**: suma unidades.
    - **Ajuste de egreso**: resta unidades (por roturas, faltantes, etc.).
    - **Ajuste de ingreso (corrección)**: escribís el stock final real
      después de contar el inventario.
3. Escribí la cantidad y el **motivo** (obligatorio).
4. **Aplicar**.

### 6.5 Historial de un producto

Botón **Historial**: muestra fecha, tipo, cantidad, stock resultante y
motivo de cada movimiento. Es tu auditoría: nada se mueve sin quedar
registrado.

### 6.6 Desactivar un producto

Si dejás de vender algo, seleccionarlo y tocar **Desactivar**.
Desaparece de las listas, pero **las ventas antiguas se conservan**.

### 6.7 Stock bajo

Cuando el stock de un producto está por debajo del mínimo, el número de
stock se ve **en naranja** en la tabla.

---

## 7. Reportes

| Reporte | Qué te muestra |
|---|---|
| **Ventas del día** | Cantidad de ventas y totales por efectivo, tarjeta y otros. |
| **Productos más vendidos** | Ranking por cantidad vendida en el período que elijas. |
| **Valor de stock** | Cuánta plata tenés en mercadería a costo y a precio de venta, y la ganancia estimada. |
| **Bajo stock** | Qué productos hay que reponer y cuánto falta. |

Cada reporte tiene un botón **Exportar CSV**: guarda un archivo que se
abre directamente con Excel.

---

## 8. Configuración y copias de seguridad

### 8.1 Datos del local

Cargá nombre, dirección, teléfono y el texto de cierre del ticket
(por ejemplo: *"Gracias por su compra"*). Tocá **Guardar**.

### 8.2 Copia de seguridad manual

Botón **Hacer copia ahora**: crea un archivo de respaldo en la carpeta
de backups y te muestra dónde quedó.

### 8.3 Restaurar una copia

1. Botón **Restaurar copia...**.
2. Elegí el archivo de backup.
3. Confirmá el aviso (se reemplazan **todos** los datos actuales).
4. El programa se cierra solo: al abrirlo de nuevo, tenés la
   información de esa copia.

### 8.4 Backup automático

Todos los días, al abrir el programa, se hace una copia sola.
Se conservan las **últimas 10**. Vos no tenés que acordarte de nada.

---

## 9. Atajos de teclado

| Tecla | Acción |
|---|---|
| **Enter** | Agrega el producto buscado (en Vender). |
| **F4** | Cobrar. |
| **ESC** | Cancelar la venta actual. |

---

## 10. Rutina diaria recomendada

**Al abrir el local:** abrí el programa (se hace el backup automático).

**Durante el día:** vendé normalmente; cargá la mercadería que llega con
*Ajustar stock → Ingreso*.

**Al cerrar el día:** mirá **Reportes → Ventas del día** para saber
cuánto entró. Si querés, tocá *Hacer copia ahora*.

---

## 11. Problemas frecuentes

| Problema | Solución |
|---|---|
| Escaneo y no agrega el producto | Verificá en **Stock** que el producto exista con ese código de barras exacto. |
| Me equivoqué al cobrar | Si todavía no confirmaste: **ESC** y volvé a empezar. Si ya confirmaste, la venta queda registrada (el stock ya se descontó). |
| No imprime el ticket | Revisá que la impresora esté encendida y configurada como predeterminada en Windows. |
| El programa no abre | Cerralo desde el administrador de tareas y volvé a abrirlo. Si sigue fallando, restaurá la última copia desde Configuración. |
| Cambié de computadora | Instalá el programa en la PC nueva, abrilo una vez, y usá **Restaurar copia...** con un backup llevado en pendrive. |

---

## 12. Avisos importantes

- **Nunca borres** la carpeta `.stockventa` de tu usuario.
- Las copias de seguridad están en `.stockventa\backups`: copiá una a un
  pendrive de vez en cuando, por si se rompe la computadora.
- Desactivá los productos que no vendas más; no los borres.
- Si algo no cierra en los números, revisá el **Historial** del producto:
  ahí está todo lo que pasó.
