# Alcance y Limitaciones del Framework — Que SI y que NO se puede automatizar

> **Para quien es este documento:**
> Para cualquier QA que se pregunte "¿esto se puede automatizar con este framework?"
> antes de comprometerse a automatizar algo.
>
> **Regla de oro:** si no sabes si algo se puede automatizar, lee este documento antes
> de empezar. Ahorras horas de trabajo en la direccion equivocada.

---

## Por que necesitas saber esto antes de automatizar

Selenium es una herramienta de automatizacion de **interfaz de usuario web**. Eso significa
que puede hacer exactamente lo que un usuario humano hace en el navegador: navegar, hacer
click, llenar formularios, leer texto en pantalla.

Lo que Selenium **no puede hacer** es todo lo que no pasa por la pantalla del navegador:
consultar bases de datos directamente, verificar que un correo llego, inspeccionar logs
del servidor, o validar que un proceso de backend funciono internamente.

Automatizar algo fuera del alcance del framework produce uno de estos dos problemas:
- El test **no se puede escribir** y el QA pierde tiempo intentandolo.
- El test **se puede escribir mal** (verificando cosas incorrectas) y da falsa confianza.

---

## Lo que SI puede automatizar este framework

### Navegacion web

| Que se puede probar | Ejemplo en banca |
|---|---|
| Abrir una URL y verificar que carga | El portal de clientes abre sin error 500 |
| Navegar entre pantallas (links, botones de menu) | Ir de "Inicio" a "Transferencias" |
| Verificar que la URL cambio al navegar | Tras login exitoso, la URL contiene `/home` |
| Verificar el titulo de la pagina | La pestaña del navegador dice "QA Automation" |
| Uso del boton Atras y Adelante | El usuario vuelve al paso anterior del formulario |

```java
// Ejemplo — verificar navegacion exitosa tras login
public boolean panelPrincipalVisible() {
    return isElementVisible(PANEL_PRINCIPAL);
}

public boolean urlContieneDashboard() {
    return getCurrentUrl().contains("/home");
}
```

---

### Formularios y entrada de datos

| Que se puede probar | Ejemplo en banca |
|---|---|
| Llenar campos de texto | Ingresar numero de cuenta destino |
| Limpiar y volver a llenar campos | Corregir un monto ingresado |
| Seleccionar opciones de dropdown | Elegir tipo de cuenta: Ahorros / Corriente |
| Marcar y desmarcar checkboxes | Aceptar terminos y condiciones |
| Subir archivos (Excel, PDF, imagen) | Cargar sustento de operacion en formato .xlsx |
| Escribir y presionar Enter | Buscar un cliente por DNI |
| Verificar que un campo esta deshabilitado | El campo "Monto" esta bloqueado si no hay saldo |
| Verificar mensajes de validacion | "El monto minimo es S/ 100.00" |

```java
// Ejemplo — llenar formulario de transferencia
public void ingresarDatosDeTransferencia(String cuentaDestino, String monto, String concepto) {
    write(CAMPO_CUENTA_DESTINO, cuentaDestino);
    write(CAMPO_MONTO, monto);
    write(CAMPO_CONCEPTO, concepto);
}

public boolean campoMontoEstaDeshabilitado() {
    return isElementDisabled(CAMPO_MONTO);
}

public String obtenerMensajeDeValidacion() {
    return getText(MENSAJE_VALIDACION);
}
```

---

### Verificaciones visuales en pantalla

| Que se puede probar | Ejemplo en banca |
|---|---|
| Que un elemento es visible | El mensaje "Transferencia exitosa" aparecio |
| Que un elemento desaparecio | El spinner de carga ya no esta visible |
| El texto exacto de un elemento | El saldo muestra "S/ 1,250.00" |
| El estado de un boton (activo/inactivo) | El boton "Confirmar" esta habilitado |
| Que un elemento tiene una clase CSS especifica | La pestaña seleccionada tiene la clase "active" |
| Numero de filas en una tabla | La busqueda retorna exactamente 5 resultados |
| Contenido de una columna completa | Todos los estados en la tabla dicen "Aprobado" |
| Que un texto esta en la tabla | El cliente "Juan Perez" aparece en la lista |

```java
// Ejemplo — verificar tabla de historial
public boolean transaccionEstaEnHistorial(String numeroOperacion) {
    return isTextPresentInTable(FILAS_TABLA_HISTORIAL, numeroOperacion);
}

public int cantidadDeResultados() {
    return getTableRowCount(FILAS_TABLA_HISTORIAL);
}

public List<String> obtenerEstadosDeOperaciones() {
    return getColumnValues(COLUMNA_ESTADO);
}
```

---

### Alertas y modales

| Que se puede probar | Ejemplo en banca |
|---|---|
| Alertas nativas del navegador (window.alert) | "¿Esta seguro de realizar esta operacion?" |
| Confirmar o cancelar en window.confirm | Aceptar o rechazar la confirmacion |
| Ingresar texto en window.prompt | Ingresar clave de confirmacion en el prompt |
| Modales HTML/Angular (divs modales) | El modal de "Exito" con los datos de la operacion |
| Cerrar modales con click en boton o X | Cerrar el resumen de la operacion |
| Verificar el contenido de un modal | Los datos del modal coinciden con lo ingresado |

```java
// Ejemplo — alerta nativa
public String obtenerMensajeDeAlerta() {
    return getAlertText();
}

public void aceptarConfirmacionDeOperacion() {
    acceptAlert();
}
```

---

### Descargas de archivos

| Que se puede probar | Ejemplo en banca |
|---|---|
| Que el archivo se descargo | Al exportar, el Excel llega a la carpeta de descargas |
| Que el nombre del archivo es correcto | El archivo se llama "reporte_clientes_2026.xlsx" |
| Que el archivo es del tipo correcto | El reporte se descargo como .pdf y no como .html |

> **Importante:** este framework verifica que el archivo **existe en el disco**.
> Para verificar el **contenido interno** del Excel (que las celdas tengan los valores
> correctos), se necesita una libreria adicional como Apache POI. Ver seccion "Limitaciones".

```java
// Ejemplo — verificar descarga
public void exportarReporteDeClientes() {
    clickElement(BOTON_EXPORTAR);
    waitUntilFileDownloaded("reporte_clientes", Duration.ofSeconds(30));
}

public boolean reporteDescargadoExitosamente() {
    return isFileDownloaded("reporte_clientes");
}
```

---

### Flujos completos end-to-end

| Que se puede probar | Ejemplo en banca |
|---|---|
| Login y autenticacion completa | Usuario ingresa credenciales y llega al panel |
| Flujo completo de un proceso | Solicitar prestamo: llenar datos → validar → confirmar → ver numero |
| Flujo negativo (datos invalidos) | Intentar transferir sin saldo disponible |
| Flujo con multiples pantallas | Consultar cliente → ver historial → exportar reporte |
| Sesion y cierre de sesion | El usuario puede cerrar sesion y la URL vuelve al login |

---

### Pestanas y ventanas del navegador

| Que se puede probar | Ejemplo en banca |
|---|---|
| Que se abrio una nueva pestana | El PDF de comprobante se abrio en otra pestana |
| Cambiar a la nueva pestana y leer contenido | Verificar el numero de operacion en el PDF |
| Cerrar la pestana y volver a la principal | Volver al flujo principal tras revisar el PDF |

---

## Lo que NO puede automatizar este framework

Estas son las limitaciones reales. No son fallas del framework — son limitaciones inherentes
a Selenium como herramienta de UI. Ningun framework de Selenium las puede superar sin
herramientas adicionales.

---

### Verificacion de correos electronicos

**No se puede hacer:** verificar que un correo de confirmacion llego a la bandeja de entrada
del usuario (ni siquiera si llego al spam).

**Por que:** Selenium controla el navegador. El correo viaja por servidores SMTP externos
(Exchange, Postfix, etc.) que no son parte del navegador.

**Lo que SI puedes hacer con este framework:**
- Verificar que la pantalla muestra "Te enviamos un correo a juan@ejemplo.com"
- Verificar que la aplicacion confirma que el envio fue solicitado

**Herramientas para la verificacion real del correo:**
- API de Mailosaur o Mailhog para ambientes de prueba
- API de Microsoft Graph (Exchange) con credenciales de servicio
- Verificacion manual como parte de pruebas manuales exploratorias

```gherkin
# INCORRECTO — Selenium no puede hacer esto
Entonces el correo de confirmacion llega a la bandeja de juan@example.pe

# CORRECTO — esto SI puede verificar Selenium
Entonces la pantalla muestra "Se envio un correo de confirmacion a juan@example.pe"
```

---

### Consultas y verificaciones en base de datos

**No se puede hacer:** consultar directamente la base de datos para verificar que un
registro se guardo correctamente. Por ejemplo: verificar que la tabla `OPERACIONES`
tiene el registro de la transferencia con el monto correcto.

**Por que:** Selenium no tiene conexion a bases de datos. Su alcance es exclusivamente
la capa de presentacion (el navegador).

**Lo que SI puedes hacer con este framework:**
- Verificar que la UI muestra el numero de operacion (lo que implica que se guardo)
- Verificar que el historial en la pantalla refleja la operacion

**Herramientas para verificacion en base de datos:**
- Tests de integracion con JDBC directamente en Java (fuera del framework UI)
- Stored procedures de validacion ejecutados por el equipo de backend
- Herramientas como DbUnit para tests de base de datos
- Consultas manuales como parte del ciclo de testing de integracion

```gherkin
# INCORRECTO — Selenium no puede verificar la BD
Entonces en la tabla OPERACIONES existe un registro con monto 500 y estado APROBADO

# CORRECTO — verifica el resultado a traves de la UI
Entonces la pantalla muestra el numero de operacion y el estado "Aprobada"
Y el historial de movimientos refleja el debito de S/ 500.00
```

---

### Lectura del contenido interno de archivos descargados

**No se puede hacer:** abrir un Excel descargado y verificar que la celda B3 contiene
"Juan Perez", o que el PDF de 5 paginas tiene el logo de QA Automation en cada pagina.

**Por que:** el framework solo verifica que el archivo **existe y se descargo**. Leer
el contenido binario de Excel o PDF requiere librerias especializadas.

**Lo que SI puedes hacer con este framework:**
- Verificar que el archivo se descargo
- Verificar que el nombre del archivo es correcto
- Verificar la extension del archivo (.xlsx, .pdf)

**Herramientas para verificar contenido de archivos:**
- **Apache POI** (Java): leer y escribir Excel (.xlsx, .xls)
- **iText / PDFBox** (Java): leer contenido de PDFs
- Estas librerias se pueden agregar al `build.gradle` del proyecto y usarse en steps

```gherkin
# Lo que SI puede este framework:
Entonces el archivo de reporte fue descargado exitosamente
Y el archivo descargado tiene el nombre que incluye "reporte_clientes"

# Para verificar el contenido del Excel (requiere Apache POI en los steps):
Entonces el Excel descargado contiene los datos del cliente en la primera fila
# (el step implementa la lectura con Apache POI, no con Selenium)
```

---

### Verificacion de procesos en segundo plano (backend)

**No se puede hacer:** verificar que un proceso batch se ejecuto, que un job de noche
proceso los intereses correctamente, o que un servicio de mensajeria (Kafka, RabbitMQ)
recibio el evento.

**Por que:** estos procesos no tienen interfaz de usuario.

**Lo que SI puedes hacer con este framework:**
- Verificar en la UI el resultado del proceso cuando tiene pantalla (ej: el saldo
  actualizado despues del proceso nocturno, si la aplicacion lo muestra)
- Verificar mensajes de estado que la aplicacion expone en pantalla

**Herramientas para verificacion de procesos backend:**
- Tests de integracion con REST Assured para APIs
- Consumers de Kafka con clientes de prueba
- Logs de servidor analizados con scripts o ELK Stack

---

### Pruebas de performance y carga

**No se puede hacer:** simular 500 usuarios concurrentes haciendo transferencias al
mismo tiempo para medir tiempos de respuesta bajo carga.

**Por que:** Selenium lanza un navegador real por cada usuario. Escalar a cientos de
usuarios paralelos es inviable en recursos y lento.

**Lo que SI puedes hacer con este framework:**
- Ejecutar escenarios en paralelo (4-8 hilos para paralelismo funcional, no de carga)
- Medir tiempos de interaccion individuales de forma aproximada

**Herramientas para pruebas de performance:**
- **Apache JMeter**: pruebas de carga sobre APIs y flujos HTTP
- **Gatling**: pruebas de carga con DSL en Scala/Java
- **k6**: pruebas de carga modernas con JavaScript

---

### Pruebas de seguridad (DAST/SAST)

**No se puede hacer:** detectar vulnerabilidades como SQL Injection, XSS, CSRF o
datos sensibles expuestos en endpoints de API.

**Por que:** Selenium sigue flujos de usuario feliz. No esta disenado para atacar
aplicaciones ni analizar trafico de red.

**Lo que SI puedes hacer con este framework:**
- Verificar que campos de texto no muestran datos sensibles en pantalla
- Verificar que mensajes de error no exponen informacion tecnica al usuario final
- Verificar que una sesion vencida redirige al login

**Herramientas para seguridad:**
- **Checkmarx** (SAST — ya integrado en el pipeline de la plantilla)
- **OWASP ZAP** (DAST — proxy de seguridad activo)
- **Burp Suite** (pruebas de penetracion manuales)

---

### Aplicaciones de escritorio (no web)

**No se puede hacer:** automatizar aplicaciones Windows nativas (`.exe`), aplicaciones
instaladas (SAP GUI, Oracle Forms clasicos no web, instaladores).

**Por que:** Selenium controla navegadores web. No tiene acceso a la API de Windows.

**Herramientas para aplicaciones de escritorio:**
- **WinAppDriver** (Microsoft): automatizacion de aplicaciones Windows
- **Sikuli / SikuliX**: automatizacion por reconocimiento de imagen

---

### Automatizacion de APIs (sin UI)

**No se puede hacer:** llamar directamente a un endpoint REST o SOAP sin pasar por
la interfaz grafica, verificar el JSON de respuesta, o testear APIs que no tienen
representacion en la UI.

**Por que:** Selenium abre navegadores. No hace llamadas HTTP directas.

**Lo que SI puedes hacer:** si el resultado de la API se muestra en pantalla, verificas
ese resultado a traves de la UI como cualquier otro escenario.

**Herramientas para pruebas de API:**
- **REST Assured** (Java): pruebas de APIs REST, se puede agregar al proyecto
- **Postman + Newman**: colecciones ejecutables en CI/CD
- **SOAP UI**: para servicios web SOAP

---

### Captchas y autenticacion de dos factores (OTP)

**No se puede hacer:** resolver captchas automaticamente ni interceptar el codigo OTP
que llega al celular o correo del usuario.

**Por que:** los captchas estan disenados especificamente para bloquear bots.
Los OTP son secretos de un solo uso generados fuera del navegador.

**Solucion para ambientes de prueba:**
- Solicitar al equipo de infraestructura que **deshabilite el captcha** en ambientes
  QA/Dev (practica estandar en banca)
- Usar **cuentas de prueba con OTP predecible** o **bypass de OTP** para el ambiente
  de automatizacion (requiere coordinacion con Seguridad)
- Si no es posible el bypass, esos flujos se quedan como **prueba manual**

---

### Iframes de terceros bloqueados por CORS

**No se puede hacer:** interactuar con iframes de dominios externos que tienen politicas
de CORS estrictas (ej: widget de pago de una pasarela externa en un iframe de otro dominio).

**Lo que SI puedes hacer:**
- Verificar que el iframe cargo (esta presente en el DOM)
- Verificar texto visible alrededor del iframe
- Si el iframe es del mismo dominio, puedes hacer `driver.switchTo().frame()` y trabajar dentro

---

## Cuadro resumen rapido — para decidir si se puede automatizar

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  PREGUNTA CLAVE: ¿Lo puede ver y hacer un usuario humano en el navegador?   │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         ├── SI → este framework puede automatizarlo
         │
         └── NO → necesitas otra herramienta o prueba manual

Casos rapidos:

  ✅ Navegar pantallas                    → SI, este framework
  ✅ Llenar formularios                   → SI, este framework
  ✅ Hacer click en botones              → SI, este framework
  ✅ Leer texto de la pantalla           → SI, este framework
  ✅ Verificar que algo es visible       → SI, este framework
  ✅ Subir un archivo Excel              → SI, este framework
  ✅ Verificar que el Excel se descargo  → SI, este framework
  ✅ Alertas del navegador               → SI, este framework
  ✅ Tablas y grids de datos             → SI, este framework

  ⚠️  Leer el contenido DENTRO del Excel → necesita Apache POI adicional
  ❌  Verificar que llego un correo      → necesita API de email
  ❌  Consultar la base de datos         → necesita JDBC / test de integracion
  ❌  Pruebas de carga (100+ usuarios)   → necesita JMeter / Gatling
  ❌  Detectar vulnerabilidades XSS      → necesita Checkmarx / OWASP ZAP
  ❌  Resolver captchas                  → necesita bypass en el ambiente
  ❌  Verificar OTP por SMS/correo       → necesita bypass o prueba manual
  ❌  Automatizar apps de escritorio     → necesita WinAppDriver
  ❌  Llamar APIs sin UI                 → necesita REST Assured / Postman
  ❌  Verificar procesos backend/batch   → necesita tests de integracion
```

---

## Cuando un escenario esta en la zona gris

Algunos escenarios parecen automatizables pero tienen partes que no lo son.
La regla es **separar lo que SI de lo que NO**:

**Ejemplo real — transferencia bancaria:**

```gherkin
# El escenario COMPLETO que pide el negocio:
Scenario: Transferencia interbancaria exitosa
  Given el usuario tiene saldo disponible de S/ 2,000.00
  When realiza una transferencia de S/ 500.00 a cuenta BCP
  Then la transferencia se procesa correctamente
  And el saldo en la BD queda en S/ 1,500.00        ← NO automatizable con Selenium
  And llega correo de confirmacion al titular        ← NO automatizable con Selenium

# Como lo automatizamos correctamente:
Scenario: Transferencia interbancaria exitosa
  Given el usuario tiene saldo disponible de S/ 2,000.00
  When realiza una transferencia de S/ 500.00 a cuenta BCP
  Then la pantalla muestra el numero de operacion generado    ← SI
  And el mensaje confirma "Transferencia procesada"           ← SI
  And el historial refleja el debito de S/ 500.00             ← SI
  And la pantalla indica que se envio confirmacion al correo  ← SI (lo que dice la UI)

# Las verificaciones de BD y correo real van a:
# - Tests de integracion del equipo de backend (JDBC)
# - Prueba manual de regresion para el flujo completo de correo
```

---

## Cuando escalar el problema

Si despues de leer este documento todavia tienes dudas sobre si algo se puede automatizar:

1. **Pregunta primero al QA referente tecnico** antes de empezar a implementar.
2. Si el referente tampoco esta seguro, **crea una spike de investigacion** (max 2 horas)
   para probar si es factible.
3. Si no es posible, documenta en el feature file con un `@Manual` tag y deja el escenario
   como referencia para prueba manual.
4. **Nunca simules** una verificacion que no puedes hacer. Un test que siempre pasa
   sin verificar nada real es peor que no tener el test.

```gherkin
# Marcar escenarios que requieren verificacion manual adicional
@Manual @CORREO
Scenario: Correo de confirmacion llega al titular
  # Este escenario requiere verificacion manual del correo.
  # La UI fue verificada en el escenario de Transferencia_exitosa.
  # Herramienta requerida: acceso a bandeja del titular o API de correo.
  Given la transferencia fue realizada exitosamente
  Then el correo de confirmacion llega a la bandeja del titular
```

---

*Documento v1.0 — QA Automation QA Automatizacion — 2026-03-15*
*Revision sugerida: cada vez que se incorpore una nueva herramienta al stack tecnologico.*
