# Guia de BasePage — Como usar la clase base del framework

> Este documento explica cada metodo de `BasePage` con ejemplos reales.
> Lee esto antes de escribir tu primer Page Object.
>
> **Prerequisito:** entender que es un Page Object. Si no lo tienes claro,
> lee primero [COMO_FUNCIONA_EL_FRAMEWORK.md](../01-onboarding/COMO_FUNCIONA_EL_FRAMEWORK.md).

---

## Que es BasePage y por que existe

Todos los Page Objects del framework extienden `BasePage`. Esta clase centraliza
la infraestructura compartida de Selenium para que no tengas que repetirla en cada
Page Object que crees.

Sin `BasePage`, cada page object tendria que hacer esto manualmente:

```java
// SIN BasePage — codigo que se repetia en CADA page object (prohibido)
public class LoginPage {
    private WebDriver driver;
    private WebDriverWait wait;

    public LoginPage(WebDriver driver) {
        this.driver = driver;                                    // repetido
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20)); // repetido
    }
}
```

Con `BasePage`, tu page object hace solo esto:

```java
// CON BasePage — limpio, sin repeticion
public class LoginPage extends BasePage {

    public LoginPage() {
        super(); // BasePage inicializa todo: driver, wait, fastWait, clickWait
    }
}
```

`BasePage` te da acceso a: el driver del hilo actual, tres tipos de esperas preconfiguradas,
y 15+ metodos listos para usar sin configuracion adicional.

---

## Los tres tipos de esperas que heredas

Cuando haces `super()` en tu constructor, heredas estos tres objetos de espera:

```
┌────────────────────────────────────────────────────────────────────┐
│  wait       → WebDriverWait de 20 segundos, polling cada 500ms    │
│               Para la mayoria de interacciones estandar            │
│                                                                    │
│  fastWait   → FluentWait de 5 segundos, polling cada 100ms        │
│               Para condiciones que se cumplen rapido (<1s)         │
│               Modales, animaciones, cambios de estado en Angular   │
│                                                                    │
│  clickWait  → WebDriverWait de 15 segundos (uso interno)          │
│               Lo usa clickElement() internamente, no lo usas tu   │
└────────────────────────────────────────────────────────────────────┘
```

En la practica, rara vez usas estos objetos directamente. Los metodos de BasePage
los usan internamente. Pero es bueno saber que existen.

---

## Metodos de navegacion

### `navigateTo(String url)`

Lleva el navegador a una URL. Equivale a escribir la URL en la barra del navegador.

```java
// En tu Page Object:
public void abrirPortalDeClientes() {
    navigateTo(ConfigManager.get("base.url") + "/clientes");
}

// En el Step:
clientePage.abrirPortalDeClientes();
```

**Cuanto espera:** espera a que el browser complete la carga inicial de la pagina.
Para aplicaciones Angular que cargan dinamicamente, usa los metodos de espera
despues de la navegacion si necesitas confirmar que un elemento especifico ya cargo.

---

## Metodos de localizacion de elementos

Estos son los metodos que usas para encontrar elementos en la pagina.
Nunca uses `driver.findElement()` directamente en un Page Object.

### `find(By locator)` — presencia en el DOM

Espera hasta que el elemento **exista en el DOM** (aunque este oculto).
Usa el `wait` estandar de 20 segundos.

```java
// Cuándo usar find():
// - Leer texto o atributos de un elemento que puede estar oculto
// - Verificar el valor de un input hidden
// - Leer datos que Angular inserta antes de mostrarlos

public String obtenerMensajeDeError() {
    return find(MENSAJE_ERROR).getText();
}

public String obtenerValorDelCampoOculto() {
    return find(CAMPO_TOKEN).getAttribute("value");
}
```

**No usar `find()` antes de un click o sendKeys.** Para interacciones, usa `findVisible()`.

### `findVisible(By locator)` — visible e interactuable

Espera hasta que el elemento sea **visible** en pantalla: existe en el DOM,
tiene dimensiones mayores a cero, y no tiene `display:none` ni `visibility:hidden`.

```java
// Cuándo usar findVisible():
// - Leer texto de un elemento que debe estar visible para el usuario
// - Verificar que algo aparecio en pantalla
// - Antes de interacciones (aunque para click usa clickElement, y para escritura usa write)

public String obtenerNombreDeUsuarioEnPanel() {
    return findVisible(NOMBRE_USUARIO_PANEL).getText();
}
```

**La regla practica:**
```
¿Vas a leer texto/atributo de algo que puede estar oculto?  → find()
¿Necesitas confirmar que algo es visible en pantalla?        → findVisible()
¿Vas a hacer click?                                          → clickElement()
¿Vas a escribir en un campo?                                 → write()
```

---

## Metodos de interaccion

### `clickElement(By locator)` — click robusto

El metodo de click central del framework. No uses `driver.findElement().click()`.

```java
// En tu Page Object:
public void confirmarOperacion() {
    clickElement(BOTON_CONFIRMAR);
}

public void seleccionarTipoDeCuenta() {
    clickElement(DROPDOWN_TIPO_CUENTA);
}
```

**Que hace internamente (no tienes que preocuparte por esto, pero es util saber):**
1. Espera hasta 15 segundos a que el elemento sea clickeable (visible + habilitado)
2. Hace scroll para centrar el elemento en el viewport (evita que headers fijos lo tapen)
3. Hace el click
4. Si el DOM cambia por Angular y el elemento queda "stale" (obsoleto), reintenta hasta 3 veces
5. Si falla 3 veces, lanza `AutomationException` con el locator en el mensaje

**Cuando usas `jsClick()` en cambio:**
Solo como ultimo recurso cuando `clickElement()` falla con `ElementClickInterceptedException`
(un overlay, spinner o modal tape el elemento). El click JS no dispara eventos de mouse,
asi que puede dar resultados distintos al click nativo en aplicaciones Angular complejas.

```java
// Fallback — solo si clickElement falla con ElementClickInterceptedException
protected void hacerClickForzado(By locator) {
    jsClick(findVisible(locator));
}
```

### `write(By locator, String value)` — escritura en campos

Escribe texto en un campo. Limpia el valor previo antes de escribir.

```java
// En tu Page Object:
public void ingresarDni(String dni) {
    write(CAMPO_DNI, dni);
}

public void ingresarMonto(String monto) {
    write(CAMPO_MONTO, monto);
}
```

**Que hace internamente:**
1. Espera a que el campo sea visible (`findVisible`)
2. Limpia el contenido previo (`clear()`)
3. Escribe el nuevo valor (`sendKeys()`)

No uses `driver.findElement(CAMPO).sendKeys()` directamente — no limpia el campo previo
y puede concatenar texto si el campo ya tenia valor.

---

## Metodos de verificacion

Estos metodos retornan `true/false` o texto. Los steps usan ese resultado para hacer assertions.
Nunca lanzar `AssertionError` desde un Page Object.

### `isElementVisible(By locator)` — ¿es visible? (5 segundos)

```java
// Con timeout por defecto de 5 segundos:
public boolean mensajeDeExitoVisible() {
    return isElementVisible(MENSAJE_EXITO);
}

public boolean tablaDeResultadosVisible() {
    return isElementVisible(TABLA_RESULTADOS);
}
```

### `isElementVisible(By locator, Duration timeout)` — ¿es visible? (timeout custom)

Cuando necesitas un timeout diferente al default de 5 segundos.

```java
// Para elementos que tardan mas en aparecer (operaciones lentas del sistema):
public boolean reporteGeneradoVisible() {
    return isElementVisible(REPORTE_PDF, Duration.ofSeconds(30));
}

// Para verificacion inmediata (no quieres esperar):
public boolean modalDeErrorYaVisible() {
    return isElementVisible(MODAL_ERROR, Duration.ofMillis(500));
}
```

Los valores de timeout estan en `TimeoutConstants`:
- `STANDARD` = 20 segundos (estandar)
- `QUICK` = 5 segundos (verificaciones rapidas)
- `INSTANT` = 500ms (verificacion inmediata)

### `isElementPresent(By locator)` — ¿existe en el DOM? (sin espera)

Retorna inmediatamente sin esperar. No bloquea el hilo.

```java
// Para lógica condicional rápida (sin espera):
public boolean hayMensajeDeAdvertencia() {
    return isElementPresent(BANNER_ADVERTENCIA);
}

// Útil para: "¿el modal opcional aparecio?" sin esperar innecesariamente
public void cerrarModalSiEstaPresente() {
    if (isElementPresent(BOTON_CERRAR_MODAL)) {
        clickElement(BOTON_CERRAR_MODAL);
    }
}
```

**La diferencia con `isElementVisible()`:**
- `isElementPresent()` → chequeo instantaneo, sin espera. El elemento puede estar oculto.
- `isElementVisible()` → espera hasta el timeout. Confirma que el usuario lo puede ver.

---

## Metodos de espera y sincronizacion

### `waitUntilUrlContains(String partial)` — esperar navegacion

Espera hasta que la URL del navegador contenga el texto indicado.
Util para confirmar que Angular completo una navegacion de ruta.

```java
public void esperarQueCarguePanelDeOperaciones() {
    waitUntilUrlContains("/operaciones/panel");
}

public void esperarRedirectionALogin() {
    waitUntilUrlContains("/auth/login");
}
```

### `waitUntilInvisible(By locator)` — esperar que desaparezca

Espera hasta que un elemento deje de ser visible.
Fundamental para modales y spinners de carga en Angular.

```java
public void esperarQueCierreModalDeConfirmacion() {
    waitUntilInvisible(MODAL_CONFIRMACION);
}

public void esperarQuePareElSpinner() {
    waitUntilInvisible(SPINNER_CARGA);
}

// Uso tipico: antes de la siguiente interaccion
public void confirmarYEsperarCierre() {
    clickElement(BOTON_ACEPTAR);
    waitUntilInvisible(MODAL_CONFIRMACION); // esperar que se cierre antes de continuar
}
```

**Por que es importante:** si no esperas que el modal cierre, el siguiente click puede
ser interceptado por el overlay del modal que aun esta animando su cierre.

### `waitUntilTextPresent(By locator, String text)` — esperar texto dinamico

Espera hasta que un elemento contenga el texto indicado.
Para mensajes de exito/error que Angular actualiza dinamicamente.

```java
public void esperarMensajeDeOperacionExitosa() {
    waitUntilTextPresent(MENSAJE_ESTADO, "Operacion registrada exitosamente");
}

public void esperarEstadoDeAprobacion() {
    waitUntilTextPresent(ETIQUETA_ESTADO, "Aprobado");
}
```

### `waitSmall()` — pausa tactica de 150ms

Usa esto raramente, solo cuando una animacion CSS impide que el siguiente paso funcione
y no hay una condicion esperable con `ExpectedConditions`.

```java
// Caso de uso: esperar que Angular procese un cambio de estado antes de leer
public boolean estadoActualizadoA(String estadoEsperado) {
    clickElement(BOTON_ACTUALIZAR);
    waitSmall(); // la etiqueta parpadea brevemente al actualizarse
    return find(ETIQUETA_ESTADO).getText().equals(estadoEsperado);
}
```

**No abuses de `waitSmall()`.** Si lo usas en mas de 2-3 lugares de un Page Object,
probablemente hay una condicion esperable que no encontraste. Revisa primero
`waitUntilInvisible`, `waitUntilTextPresent` o `isElementVisible` con un timeout corto.

---

## Metodos de dropdown (SELECT HTML nativo)

Para dropdowns `<select>` HTML nativos. Los dropdowns Angular Material (mat-select)
usan un patron diferente (click en el trigger + click en la opcion).

### `selectByText(By locator, String text)` — seleccionar por texto visible

```java
public void seleccionarTipoDeDocumento(String tipo) {
    selectByText(DROPDOWN_TIPO_DOC, tipo);
    // Ejemplo: selectByText(DROPDOWN_TIPO_DOC, "DNI")
    // Ejemplo: selectByText(DROPDOWN_TIPO_DOC, "Carnet de Extranjeria")
}
```

### `getDropdownValues(By locator)` — obtener todas las opciones

```java
public List<String> obtenerTiposDeDocumentoDisponibles() {
    return getDropdownValues(DROPDOWN_TIPO_DOC);
    // Retorna: ["DNI", "Carnet de Extranjeria", "RUC", "Pasaporte"]
}

// Uso en el Step para verificar:
// assertThat(page.obtenerTiposDeDocumentoDisponibles()).contains("DNI");
```

---

## Metodos SSL (para ambientes de prueba)

### `warmupSslFor(String url)` — aceptar certificado SSL invalido

Solo necesario cuando el servidor de pruebas tiene un certificado autofirmado
que Chrome bloquea. Llamado normalmente desde los `@Before` Hooks, no desde Page Objects.

```java
// En Hooks.java (no en un Page Object normal):
@Before
public void setUp() {
    DriverFactory.initDriver();
    BasePage basePage = new BasePage(){};
    basePage.warmupSslFor(ConfigManager.get("base.url"));
}
```

`warmupApiSsl()` esta **deprecada** desde marzo 2026. No usar.

---

## Metodos JavaScript (uso avanzado)

### `executeJS(String script, Object... args)` — ejecutar JavaScript

Para casos donde Selenium no puede interactuar directamente.

```java
// Cambiar el valor de un input que no acepta sendKeys (readonly via JS):
protected void forzarValorEnCampo(By locator, String valor) {
    WebElement campo = find(locator);
    executeJS("arguments[0].value = arguments[1];", campo, valor);
}

// Hacer scroll a una posicion especifica:
protected void scrollAlFinal() {
    executeJS("window.scrollTo(0, document.body.scrollHeight);");
}
```

### `scrollIntoViewCenter(WebElement element)` — scroll al centro

Centrar un elemento en el viewport antes de interactuar. Lo llama `clickElement()`
automaticamente. Solo necesitas llamarlo manualmente para aserciones visuales
de elementos fuera del viewport.

```java
protected void verificarElementoFueraDelViewport(By locator) {
    WebElement el = find(locator);
    scrollIntoViewCenter(el);
    assertThat(el.isDisplayed()).isTrue();
}
```

---

---

## Metodos de lectura de datos

### `getText(By locator)` — leer texto visible de un elemento

Lee el texto que el usuario ve en pantalla de un elemento como `<span>`, `<p>`, `<td>`, `<label>`.

```java
// En tu Page Object:
public String obtenerMensajeDeConfirmacion() {
    return getText(MENSAJE_CONFIRMACION);
    // Retorna: "Operacion registrada exitosamente"
}

public String obtenerEstadoDeLaSolicitud() {
    return getText(ETIQUETA_ESTADO);
    // Retorna: "Aprobado", "Pendiente", "Rechazado"
}
```

**Diferencia clave con `getInputValue()`:**
```
getText(locator)      → para <span>, <p>, <td>, etiquetas — texto que el usuario VE
getInputValue(locator)→ para <input>, <textarea>           — texto que el usuario EDITA
```

### `getAttribute(By locator, String attribute)` — leer atributo HTML

Lee el valor de cualquier atributo HTML de un elemento. Util para verificar estado
de componentes Angular que comunican su estado por atributos, no por texto visible.

```java
// Verificar el valor actual de un campo input:
public String obtenerValorDelCampoMonto() {
    return getAttribute(CAMPO_MONTO, "value");
    // Retorna: "500.00"
}

// Verificar si un boton esta deshabilitado via atributo:
public boolean botonConfirmarDeshabilitado() {
    return getAttribute(BOTON_CONFIRMAR, "disabled") != null;
}

// Verificar clases activas en componentes Angular Material:
public boolean pestanaParametrosActiva() {
    String clases = getAttribute(PESTANA_PARAMETROS, "class");
    return clases != null && clases.contains("active");
}

// Leer data attributes personalizados:
public String obtenerIdDeOperacion() {
    return getAttribute(FILA_OPERACION, "data-operacion-id");
}
```

**Atributos mas utiles en banca:**
```
"value"         → contenido actual de un <input>
"class"         → clases CSS (detecta estados: active, disabled, selected)
"disabled"      → si retorna null = habilitado, cualquier valor = deshabilitado
"aria-expanded" → si un acordeon/panel esta abierto ("true") o cerrado ("false")
"aria-selected" → si una pestaña o opcion esta seleccionada
"data-*"        → atributos personalizados del sistema
"href"          → URL de un enlace
"placeholder"   → texto de ayuda de un campo input
```

### `getInputValue(By locator)` — leer el valor de un campo editable

Lee lo que hay escrito en un campo `<input>` o `<textarea>`. Usar en lugar de `getText()`
para campos de formulario, porque `getText()` retorna vacio en inputs.

```java
public String obtenerMontoIngresado() {
    return getInputValue(CAMPO_MONTO);
    // Retorna: "1500.50"
}

public String obtenerDniIngresado() {
    return getInputValue(CAMPO_DNI);
    // Retorna: "12345678"
}
```

### `getCurrentUrl()` — leer la URL actual

Retorna la URL completa que esta en la barra de direcciones del navegador en ese momento.

```java
// En el Page Object para verificar navegacion:
public boolean estaEnPanelDeOperaciones() {
    return getCurrentUrl().contains("/operaciones/panel");
}

// En el Step directamente (via la pagina):
// assertThat(operacionesPage.getCurrentUrl()).contains("/home");
```

---

## Metodos de localizacion multiple

### `findAll(By locator)` — obtener lista de elementos

Retorna todos los elementos que coinciden con el locator. Esencial para trabajar
con tablas, listas y cualquier componente que repite elementos.

```java
// Obtener todas las filas de una tabla:
private static final By FILAS_HISTORIAL = By.cssSelector("table tbody tr");

public List<WebElement> obtenerFilasDelHistorial() {
    return findAll(FILAS_HISTORIAL);
}

// Obtener todas las opciones de un menu:
public List<WebElement> obtenerItemsDelMenu() {
    return findAll(By.cssSelector("nav .menu-item"));
}
```

**Importante:** `findAll` espera que al menos un elemento sea visible antes de retornar.
Si la tabla esta vacia, lanza `TimeoutException`. Usa `countElements()` si necesitas
saber si hay elementos sin fallar cuando no los hay.

### `countElements(By locator)` — contar elementos sin esperar

Cuenta los elementos que coinciden con el locator en el DOM actual, sin esperar.
Retorna 0 si no hay ninguno (no lanza excepcion).

```java
// Verificar si la tabla tiene resultados:
public boolean hayResultadosEnLaTabla() {
    return countElements(FILAS_TABLA) > 0;
}

// Obtener el numero de notificaciones:
public int obtenerCantidadDeNotificaciones() {
    return countElements(BADGE_NOTIFICACION);
    // Retorna: 0, 1, 2, 5... segun las notificaciones visibles
}
```

**Cuando usar `findAll` vs `countElements`:**
```
findAll(locator)      → cuando necesitas INTERACTUAR con los elementos (leer texto, hacer click)
countElements(locator)→ cuando solo necesitas saber CUANTOS hay, sin tocarlos
```

---

## Nuevos metodos de interaccion

### `clearField(By locator)` — limpiar un campo sin escribir

Borra el contenido de un campo sin escribir nada nuevo. Util cuando necesitas
dejar un campo vacio para probar validaciones de campo requerido.

```java
public void borrarElCampoDeDni() {
    clearField(CAMPO_DNI);
    // El campo queda vacio para probar el mensaje "Campo obligatorio"
}
```

### `writeAndPressEnter(By locator, String value)` — escribir y confirmar con Enter

Escribe texto en un campo y presiona Enter. Patron comun en campos de busqueda
y autocompletados que disparan la accion al presionar Enter.

```java
public void buscarClientePorNombre(String nombre) {
    writeAndPressEnter(CAMPO_BUSQUEDA, nombre);
    // Equivale a: write(CAMPO, nombre) + pressKey(CAMPO, Keys.ENTER)
}
```

### `pressKey(By locator, Keys key)` — presionar una tecla especial

Envia una tecla especial a un elemento sin escribir texto. Util para navegar
entre campos con Tab, cancelar con Escape, o activar funciones con teclas especiales.

```java
// Presionar Tab para ir al siguiente campo (dispara validacion en Angular):
public void pasarAlSiguienteCampo() {
    pressKey(CAMPO_DNI, Keys.TAB);
}

// Cancelar un modal presionando Escape:
public void cancelarConEscape() {
    pressKey(CAMPO_ACTIVO, Keys.ESCAPE);
}

// Confirmar con Enter en un dialogo:
public void confirmarConEnter() {
    pressKey(CAMPO_ACTIVO, Keys.ENTER);
}
```

**Teclas mas usadas en banca:**

| Tecla | Cuando usarla |
|---|---|
| `Keys.TAB` | Mover al siguiente campo; Angular dispara validacion al perder foco |
| `Keys.ENTER` | Confirmar seleccion en autocomplete o enviar formulario |
| `Keys.ESCAPE` | Cerrar modal o cancelar edicion |
| `Keys.F2` | Activar modo edicion en grids de datos |
| `Keys.DELETE` | Borrar texto seleccionado |
| `Keys.CONTROL + "a"` | Seleccionar todo el texto de un campo |

### `hoverOver(By locator)` — pasar el mouse sobre un elemento

Mueve el cursor sobre un elemento sin hacer click. Necesario para menus Angular Material
(`mat-menu`) que se despliegan al pasar el mouse, no al hacer click.

```java
// Abrir un menu que aparece al hacer hover:
public void abrirMenuDeOpciones() {
    hoverOver(ICONO_OPCIONES);
    // El menu aparece, ahora se puede hacer click en sus items
}

// Patron completo hover + click en submenu:
public void irAConfiguracion() {
    hoverOver(MENU_PRINCIPAL);           // abre el menu
    clickElement(SUBMENU_CONFIGURACION); // hace click en el item
}
```

### `scrollToElement(By locator)` — hacer scroll a un elemento por locator

Hace scroll para centrar un elemento en la pantalla, usando su locator.
Diferente de `scrollIntoViewCenter(WebElement)` que requiere tener el WebElement ya.

```java
// Util para formularios largos donde el boton de guardar esta al final:
public void irAlBotonGuardar() {
    scrollToElement(BOTON_GUARDAR);
    // Ahora el boton es visible, se puede hacer click
}
```

---

## Nuevos metodos de verificacion de estado

### `isElementEnabled(By locator)` — ¿el elemento esta habilitado?

Verifica si un boton o campo esta habilitado (no tiene el atributo `disabled`).
En banca, los botones de confirmacion frecuentemente se habilitan solo cuando
todos los campos requeridos estan correctamente llenos.

```java
// Verificar que el boton se habilito despues de completar el formulario:
public boolean botonConfirmarHabilitado() {
    return isElementEnabled(BOTON_CONFIRMAR);
}

// En el Step:
// assertThat(transferenciasPage.botonConfirmarHabilitado())
//     .as("El boton debe habilitarse al completar todos los campos")
//     .isTrue();
```

### `isElementDisabled(By locator)` — ¿el elemento esta deshabilitado?

Complemento de `isElementEnabled`. Util para verificar que controles de seguridad
esten activos: un campo de monto no deberia ser editable en ciertos estados.

```java
// Verificar que el campo no es editable en modo solo lectura:
public boolean campoCuentaDestinoDeshabilitado() {
    return isElementDisabled(CAMPO_CUENTA_DESTINO);
}

// En el Step para verificar que el control de seguridad funciona:
// assertThat(page.campoCuentaDestinoDeshabilitado())
//     .as("El campo cuenta no debe ser editable despues de confirmar")
//     .isTrue();
```

---

## Nuevos metodos de espera

### `waitUntilAttributeContains(By, String attribute, String value)` — esperar cambio de atributo

Espera hasta que un atributo HTML de un elemento contenga el valor indicado.
Es el metodo mas importante para esperar cambios de estado en aplicaciones Angular.

```java
// Esperar que una pestaña quede seleccionada (Angular añade clase "active"):
public void esperarPestanaActiva(By locatorPestana) {
    waitUntilAttributeContains(locatorPestana, "class", "active");
}

// Esperar que un acordeon se expanda:
public void esperarPanelExpandido(By locatorPanel) {
    waitUntilAttributeContains(locatorPanel, "aria-expanded", "true");
}

// Esperar que un campo quede habilitado (Angular remueve "disabled"):
public void esperarCampoHabilitado(By locatorCampo) {
    // Alternativa: usar waitUntilEnabled(locatorCampo)
    waitUntilAttributeContains(locatorCampo, "class", "ng-valid");
}
```

**Por que existe este metodo:** Angular comunica el estado de sus componentes
(seleccionado, expandido, activo, valido) cambiando clases CSS y atributos aria-*,
no haciendo visible/invisible elementos. Sin este metodo, los tests no pueden saber
cuando Angular termino de procesar un cambio de estado.

### `waitUntilEnabled(By locator)` — esperar que un elemento se habilite

Espera hasta que un elemento este visible Y habilitado. Mas semantico que
`waitUntilAttributeContains` para el caso especifico de esperar que un boton
se habilite despues de que el usuario complete un formulario.

```java
public void esperarQueSeHabiliteElBotonConfirmar() {
    waitUntilEnabled(BOTON_CONFIRMAR);
    // Ahora el boton esta listo para recibir el click
}

// Patron completo para formularios bancarios:
public void completarFormularioYConfirmar(String monto, String cuenta) {
    write(CAMPO_MONTO, monto);
    write(CAMPO_CUENTA_DESTINO, cuenta);
    waitUntilEnabled(BOTON_CONTINUAR); // Angular valida y habilita
    clickElement(BOTON_CONTINUAR);
}
```

### `waitUntilTextNotPresent(By locator, String text)` — esperar que un texto desaparezca

Espera hasta que el texto de un elemento deje de contener el valor indicado.
Complemento de `waitUntilTextPresent`.

```java
// Esperar que el estado cambie de "Procesando" a cualquier otro valor:
public void esperarFinDeProcesamiento() {
    waitUntilTextNotPresent(ETIQUETA_ESTADO, "Procesando...");
    // Ahora el estado puede ser "Aprobado", "Rechazado", "Error", etc.
}

// Esperar que un mensaje temporal desaparezca:
public void esperarQueLimpieElMensajeDeValidacion() {
    waitUntilTextNotPresent(MENSAJE_ERROR, "Campo obligatorio");
}
```

---

## Alertas nativas del navegador

Las alertas nativas son ventanas emergentes que genera el propio navegador cuando
el JavaScript del sistema llama a `alert()`, `confirm()` o `prompt()`.
Son distintas a los modales de Angular — estas bloquean todo el navegador hasta
que el usuario responde.

```
┌─────────────────────────────────────────────────────────────────┐
│  alert("mensaje")     → solo tiene boton OK                     │
│  confirm("pregunta")  → tiene botones OK y Cancelar             │
│  prompt("pregunta")   → tiene campo de texto + OK y Cancelar    │
└─────────────────────────────────────────────────────────────────┘
```

### `getAlertText()` — leer el mensaje de la alerta

```java
public String obtenerMensajeDeLaAlerta() {
    return getAlertText();
    // Retorna: "¿Confirma la eliminacion del registro?"
}
```

### `acceptAlert()` — aceptar la alerta (click OK)

```java
// Para alert() simple: cierra la alerta
// Para confirm(): retorna true al JS, procede con la accion
public void aceptarAlertaDeConfirmacion() {
    acceptAlert();
}
```

### `dismissAlert()` — rechazar la alerta (click Cancelar)

```java
// Solo funciona en confirm() y prompt(). Retorna false al JS.
public void cancelarAlertaDeConfirmacion() {
    dismissAlert();
}
```

### `typeInAlert(String text)` — escribir en prompt y aceptar

```java
// Para prompt(): escribe el texto y hace click OK
public void ingresarMotivoEnAlerta(String motivo) {
    typeInAlert(motivo);
    // Escribe el motivo y presiona OK automaticamente
}
```

**Patron completo para un escenario con alerta de confirmacion:**

```java
// En el Page Object:
public void eliminarRegistro() {
    clickElement(BOTON_ELIMINAR);
    // La alerta aparece automaticamente
}

public String obtenerMensajeDeAlerta() {
    return getAlertText();
}

public void confirmarEliminacion() {
    acceptAlert();
}

public void cancelarEliminacion() {
    dismissAlert();
}

// En el Step:
// reportesPage.eliminarRegistro();
// assertThat(reportesPage.obtenerMensajeDeAlerta()).contains("confirma la eliminacion");
// reportesPage.confirmarEliminacion();
// assertThat(reportesPage.mensajeDeExitoVisible()).isTrue();
```

---

## Carga de archivos

### `uploadFile(By locator, String absolutePath)` — subir un archivo

Carga un archivo al sistema a traves de un campo `<input type="file">`.
No requiere hacer click en el campo ni en el boton — Selenium lo maneja
enviando la ruta del archivo directamente al elemento HTML.

```java
// En el Page Object:
private static final By INPUT_EXCEL = By.id("inputArchivoExcel");
private static final By BOTON_PROCESAR = By.id("btnProcesarArchivo");

public void cargarArchivoExcelDeClientes(String rutaArchivo) {
    uploadFile(INPUT_EXCEL, rutaArchivo);
    clickElement(BOTON_PROCESAR);
}

// En el Step:
// String ruta = System.getProperty("user.dir") + "/src/test/resources/testdata/clientes.xlsx";
// cargaPage.cargarArchivoExcelDeClientes(ruta);
```

**Como organizar los archivos de prueba:**
```
src/test/resources/
└── testdata/
    ├── clientes_validos.xlsx     ← archivo de prueba para carga exitosa
    ├── clientes_formato_malo.xlsx← para probar rechazo por formato
    └── clientes_datos_vacios.xlsx← para probar validacion de campos
```

**Como construir la ruta en el Step:**
```java
// La forma correcta — funciona en local Y en el pipeline CI/CD:
String rutaBase = System.getProperty("user.dir");
String rutaArchivo = rutaBase + "/src/test/resources/testdata/clientes_validos.xlsx";
cargaPage.cargarArchivoExcel(rutaArchivo);
```

---

## Descarga y verificacion de archivos

Los archivos descargados van a `build/downloads/` (configurado en BrowserConfig).
Cuando Chrome descarga un archivo, primero crea un `.crdownload` temporal.
Solo cuando ese temporal desaparece el archivo esta completo.

```
build/downloads/
├── reporte_clientes_2026-03-15.xlsx   ← archivo completo
├── estado_cuenta.pdf                  ← archivo completo
└── reporte_tmp.crdownload             ← descarga en progreso (Chrome temporal)
```

### `waitUntilFileDownloaded(String pattern, Duration timeout)` — esperar la descarga

```java
// En el Page Object:
public void descargarReporteDeClientes() {
    clickElement(BOTON_EXPORTAR_EXCEL);
    waitUntilFileDownloaded("reporte_clientes", Duration.ofSeconds(30));
    // Espera hasta 30s a que aparezca un archivo que contenga "reporte_clientes"
    // y que no tenga extension .crdownload (descarga completada)
}
```

### `isFileDownloaded(String pattern)` — verificar si ya se descargo

```java
// Verificacion rapida sin esperar:
public boolean reporteExcelYaDescargado() {
    return isFileDownloaded("reporte_clientes");
    // true si existe "reporte_clientes_2026.xlsx" en build/downloads/
}
```

### `getDownloadedFilePath(String pattern)` — obtener la ruta del archivo

```java
// Util si necesitas leer el contenido del archivo (con Apache POI para Excel):
public String obtenerRutaDelReporteDescargado() {
    return getDownloadedFilePath("reporte_clientes");
    // Retorna: "C:/proyecto/build/downloads/reporte_clientes_2026-03-15.xlsx"
}
```

### `deleteDownloadedFile(String pattern)` — limpiar archivos de prueba

```java
// Llamar en @Before para que descargas de tests anteriores no interfieran:
public void limpiarDescargas() {
    deleteDownloadedFile("reporte_clientes");
    deleteDownloadedFile("estado_cuenta");
}
```

**Patron completo para un escenario de descarga de Excel:**

```java
// En el Page Object:
public void exportarListadoDeClientes() {
    clickElement(BOTON_EXPORTAR);
    waitUntilFileDownloaded("listado_clientes", Duration.ofSeconds(30));
}

public boolean reporteExcelDescargado() {
    return isFileDownloaded("listado_clientes");
}

public void limpiarReporteDescargado() {
    deleteDownloadedFile("listado_clientes");
}

// En el Feature:
// Then el sistema descarga el listado de clientes en formato Excel
//
// En el Step:
// assertThat(clientesPage.reporteExcelDescargado())
//     .as("El archivo Excel debe descargarse correctamente")
//     .isTrue();
```

---

## Tablas y grids de datos

Las tablas son centrales en sistemas bancarios: historiales de transacciones,
listados de clientes, resultados de busqueda, reportes. Estos metodos simplifican
trabajar con tablas HTML (`<table><tr><td>`).

**Como identificar los locators de una tabla:**
```html
<table>
  <thead>
    <tr>  ← NO son los locators que usas (son encabezados)
      <th>DNI</th><th>Nombre</th><th>Saldo</th>
    </tr>
  </thead>
  <tbody>
    <tr>  ← FILAS_TABLA = By.cssSelector("table tbody tr")
      <td>12345678</td>   ← COLUMNA_DNI = By.cssSelector("table tbody tr td:nth-child(1)")
      <td>Juan Perez</td> ← COLUMNA_NOMBRE = By.cssSelector("table tbody tr td:nth-child(2)")
      <td>S/. 1500</td>   ← COLUMNA_SALDO = By.cssSelector("table tbody tr td:nth-child(3)")
    </tr>
  </tbody>
</table>
```

### `getColumnValues(By cellsLocator)` — obtener toda una columna

```java
private static final By COLUMNA_ESTADO = By.cssSelector("table tbody tr td:nth-child(4)");

public List<String> obtenerTodosLosEstados() {
    return getColumnValues(COLUMNA_ESTADO);
    // Retorna: ["Aprobado", "Pendiente", "Aprobado", "Rechazado"]
}

// En el Step para verificar que todos los registros tienen estado:
// assertThat(tablaPage.obtenerTodosLosEstados()).doesNotContain("").doesNotContainNull();
```

### `findRowContaining(By rowsLocator, String text)` — encontrar una fila por texto

```java
private static final By FILAS_TABLA = By.cssSelector("table tbody tr");
private static final By BTN_DETALLE = By.cssSelector("button.btn-ver-detalle");

// Buscar la fila del cliente y hacer click en su boton de detalle:
public void verDetalleDelCliente(String dni) {
    WebElement fila = findRowContaining(FILAS_TABLA, dni);
    fila.findElement(BTN_DETALLE).click();
}

// Si el cliente no existe en la tabla, lanza NoSuchElementException con mensaje claro
```

### `isTextPresentInTable(By rowsLocator, String text)` — buscar texto en la tabla

```java
// Version boolean — no lanza excepcion si no encuentra:
public boolean clienteApareceEnListado(String dni) {
    return isTextPresentInTable(FILAS_TABLA, dni);
}

// En el Step:
// assertThat(busquedaPage.clienteApareceEnListado("12345678"))
//     .as("El cliente debe aparecer en los resultados")
//     .isTrue();
```

### `getTableRowCount(By rowsLocator)` — contar filas de la tabla

```java
public int obtenerCantidadDeResultados() {
    return getTableRowCount(FILAS_TABLA);
}

// En el Step:
// assertThat(busquedaPage.obtenerCantidadDeResultados())
//     .as("La busqueda debe retornar al menos un resultado")
//     .isGreaterThan(0);
```

**Patron completo para un escenario de tabla:**

```java
package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import java.util.List;

public class HistorialTransaccionesPage extends BasePage {

    private static final By FILAS_HISTORIAL      = By.cssSelector("table.historial tbody tr");
    private static final By COLUMNA_MONTO        = By.cssSelector("table.historial tbody tr td:nth-child(3)");
    private static final By COLUMNA_ESTADO       = By.cssSelector("table.historial tbody tr td:nth-child(4)");
    private static final By BTN_VER_COMPROBANTE  = By.cssSelector("button.btn-comprobante");
    private static final By MENSAJE_SIN_HISTORIAL= By.cssSelector(".empty-state-message");

    public HistorialTransaccionesPage() { super(); }

    public int obtenerCantidadDeTransacciones() {
        return getTableRowCount(FILAS_HISTORIAL);
    }

    public List<String> obtenerMontosDeLasTransacciones() {
        return getColumnValues(COLUMNA_MONTO);
    }

    public boolean transaccionApareceEnHistorial(String numeroOperacion) {
        return isTextPresentInTable(FILAS_HISTORIAL, numeroOperacion);
    }

    public void verComprobanteDe(String numeroOperacion) {
        WebElement fila = findRowContaining(FILAS_HISTORIAL, numeroOperacion);
        fila.findElement(BTN_VER_COMPROBANTE).click();
    }

    public boolean historialVacioVisible() {
        return isElementVisible(MENSAJE_SIN_HISTORIAL);
    }
}
```

---

## Pestanas y ventanas del navegador

Algunos flujos bancarios abren documentos (PDFs, comprobantes) en una nueva pestana.
Selenium llama a estas "windows" internamente, aunque visualmente sean pestanas.

```
Pestaña 1 (principal)    → el sistema de QA Automation
     ↓ click en "Ver PDF"
Pestaña 2 (nueva)        → el comprobante en PDF
     ↓ switchToNewTab()   → driver se mueve a pestaña 2
     ↓ [verificar PDF]
     ↓ closeCurrentTabAndSwitchBack() → cierra pestaña 2, vuelve a 1
Pestaña 1 (principal)    → el driver esta de vuelta aqui
```

### `switchToNewTab()` — ir a la nueva pestana

```java
// En el Page Object:
public void abrirComprobanteEnNuevaPestana() {
    clickElement(BOTON_VER_COMPROBANTE);
    switchToNewTab(); // el driver ahora controla la pestaña del PDF
}
```

### `switchToMainTab()` — volver a la pestana principal

```java
public void volverAlSistema() {
    switchToMainTab();
    // el driver vuelve a controlar la pestaña principal del sistema
}
```

### `closeCurrentTabAndSwitchBack()` — cerrar la pestana actual y volver

```java
public void cerrarComprobanteYVolver() {
    closeCurrentTabAndSwitchBack();
    // cierra la pestaña del PDF y vuelve al sistema automaticamente
}
```

**Patron completo para verificar un PDF descargado en nueva pestana:**

```java
// En el Page Object:
public void abrirComprobante(String numeroOperacion) {
    WebElement fila = findRowContaining(FILAS_HISTORIAL, numeroOperacion);
    fila.findElement(BTN_VER_PDF).click();
    switchToNewTab();
}

public String obtenerTituloDelComprobante() {
    return getPageTitle(); // titulo de la pestaña del PDF
}

public void cerrarComprobante() {
    closeCurrentTabAndSwitchBack();
}

// En el Step:
// historialPage.abrirComprobante("OP-2026-001234");
// assertThat(historialPage.obtenerTituloDelComprobante()).contains("Comprobante");
// historialPage.cerrarComprobante();
```

---

## Metodos de pagina y scroll

### `waitForPageLoad()` — esperar carga completa de la pagina

Espera hasta que el navegador termine de cargar todos los recursos de la pagina.
Util despues de navegaciones a pantallas con muchos datos.

```java
public void navegarAReporteAnual() {
    clickElement(MENU_REPORTES);
    clickElement(OPCION_REPORTE_ANUAL);
    waitForPageLoad(); // espera que cargue toda la pagina pesada
    // ahora se puede interactuar con los datos
}
```

**Importante:** `waitForPageLoad()` espera que el HTML cargue, pero Angular puede
seguir renderizando componentes despues. Usa `waitUntilInvisible(SPINNER)` si el sistema
tiene un indicador de carga propio despues de la carga de pagina.

### `scrollToTop()` y `scrollToBottom()` — desplazarse en la pagina

```java
// Volver al inicio despues de revisar el final de un formulario largo:
public void volverAlInicioDelFormulario() {
    scrollToTop();
}

// Bajar al final para activar carga de mas datos (lazy loading):
public void cargarMasResultados() {
    scrollToBottom();
    // Muchos sistemas cargan mas filas cuando llegas al final de la pagina
}
```

### `getPageTitle()` — leer el titulo de la pestana

```java
// Verificar que se abrio la pantalla correcta:
public String obtenerTituloDeLaPagina() {
    return getPageTitle();
    // Retorna el texto del <title> HTML o el titulo que muestra la pestaña
}
```

---

## Tabla de referencia rapida — cual metodo usar

### Navegacion
| Necesito... | Metodo de BasePage |
|---|---|
| Ir a una URL | `navigateTo(url)` |
| Leer la URL actual | `getCurrentUrl()` |
| Esperar que la URL contenga texto | `waitUntilUrlContains(texto)` |

### Localizacion de elementos
| Necesito... | Metodo de BasePage |
|---|---|
| Encontrar un elemento (puede estar oculto) | `find(locator)` |
| Encontrar un elemento (debe ser visible) | `findVisible(locator)` |
| Encontrar multiples elementos (tabla/lista) | `findAll(locator)` |
| Contar cuantos elementos hay | `countElements(locator)` |

### Interaccion con elementos
| Necesito... | Metodo de BasePage |
|---|---|
| Hacer click | `clickElement(locator)` |
| Escribir en un campo | `write(locator, texto)` |
| Limpiar un campo | `clearField(locator)` |
| Escribir y presionar Enter | `writeAndPressEnter(locator, texto)` |
| Presionar tecla (Tab, Escape, F2...) | `pressKey(locator, Keys.XXX)` |
| Hacer hover sobre un elemento | `hoverOver(locator)` |
| Seleccionar opcion de `<select>` | `selectByText(locator, texto)` |
| Obtener opciones de `<select>` | `getDropdownValues(locator)` |
| Scroll al elemento | `scrollToElement(locator)` |

### Lectura de datos
| Necesito... | Metodo de BasePage |
|---|---|
| Leer el texto visible de un elemento | `getText(locator)` |
| Leer un atributo HTML (value, class, aria-*) | `getAttribute(locator, "atributo")` |

### Verificaciones de estado
| Necesito... | Metodo de BasePage |
|---|---|
| Verificar si es visible (5s de espera) | `isElementVisible(locator)` |
| Verificar si es visible (timeout custom) | `isElementVisible(locator, duration)` |
| Verificar si existe en DOM (sin esperar) | `isElementPresent(locator)` |
| Verificar si el elemento esta habilitado | `isElementEnabled(locator)` |
| Verificar si el elemento esta deshabilitado | `isElementDisabled(locator)` |

### Esperas y sincronizacion
| Necesito... | Metodo de BasePage |
|---|---|
| Esperar que un elemento desaparezca | `waitUntilInvisible(locator)` |
| Esperar que aparezca un texto | `waitUntilTextPresent(locator, texto)` |
| Esperar que desaparezca un texto | `waitUntilTextNotPresent(locator, texto)` |
| Esperar que un atributo cambie (class, aria-*) | `waitUntilAttributeContains(locator, attr, valor)` |
| Esperar que un elemento se habilite | `waitUntilEnabled(locator)` |
| Pausa tactica de 150ms (ultimo recurso) | `waitSmall()` |

### JavaScript y avanzado
| Necesito... | Metodo de BasePage |
|---|---|
| Ejecutar JavaScript | `executeJS(script, args...)` |
| Click JS (fallback cuando click nativo falla) | `jsClick(element)` |

### Alertas nativas del navegador (window.alert / confirm / prompt)
| Necesito... | Metodo de BasePage |
|---|---|
| Leer el texto de la alerta | `getAlertText()` |
| Aceptar la alerta (click OK) | `acceptAlert()` |
| Rechazar la alerta (click Cancelar) | `dismissAlert()` |
| Escribir en prompt y aceptar | `typeInAlert(texto)` |

### Carga de archivos (Excel, PDF, imagenes)
| Necesito... | Metodo de BasePage |
|---|---|
| Subir un archivo via input[type=file] | `uploadFile(locator, rutaAbsolutaDelArchivo)` |

### Descarga y verificacion de archivos
| Necesito... | Metodo de BasePage |
|---|---|
| Esperar que un archivo termine de descargarse | `waitUntilFileDownloaded(nombreParcial, timeout)` |
| Verificar si un archivo ya fue descargado | `isFileDownloaded(nombreParcial)` |
| Obtener la ruta completa del archivo descargado | `getDownloadedFilePath(nombreParcial)` |
| Eliminar archivos descargados (limpieza) | `deleteDownloadedFile(nombreParcial)` |

### Tablas y grids de datos
| Necesito... | Metodo de BasePage |
|---|---|
| Obtener todos los textos de una columna | `getColumnValues(locatorDeCeldas)` |
| Buscar la fila que tiene un texto especifico | `findRowContaining(locatorDeFilas, texto)` |
| Verificar si un texto existe en la tabla | `isTextPresentInTable(locatorDeFilas, texto)` |
| Contar cuantas filas tiene la tabla | `getTableRowCount(locatorDeFilas)` |

### Pestanas y ventanas del navegador
| Necesito... | Metodo de BasePage |
|---|---|
| Ir a la nueva pestana que se abrio (PDF, reporte) | `switchToNewTab()` |
| Volver a la pestana principal | `switchToMainTab()` |
| Cerrar la pestana actual y volver | `closeCurrentTabAndSwitchBack()` |

### Lectura de estado de pagina
| Necesito... | Metodo de BasePage |
|---|---|
| Leer el valor actual de un campo input | `getInputValue(locator)` |
| Leer el titulo de la pestana del navegador | `getPageTitle()` |
| Esperar que la pagina cargue completamente | `waitForPageLoad()` |
| Scroll al inicio de la pagina | `scrollToTop()` |
| Scroll al final de la pagina | `scrollToBottom()` |

---

## Los errores mas comunes al escribir Page Objects

### Error 1 — No extender BasePage

```java
// MAL — crea su propia infraestructura, duplica codigo, no es thread-safe
public class MiPage {
    private WebDriver driver;
    private WebDriverWait wait;

    public MiPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }
}

// BIEN
public class MiPage extends BasePage {
    public MiPage() { super(); }
}
```

### Error 2 — Usar driver directamente en lugar de metodos de BasePage

```java
// MAL — acceso directo al driver en el Page Object
public void hacerLogin(String user, String pass) {
    driver.findElement(By.id("usuario")).sendKeys(user);   // no limpia campo previo
    driver.findElement(By.id("clave")).sendKeys(pass);     // sin espera de visibilidad
    driver.findElement(By.id("btnLogin")).click();         // sin manejo de stale element
}

// BIEN — usa metodos de BasePage
public void hacerLogin(String user, String pass) {
    write(CAMPO_USUARIO, user);
    write(CAMPO_CLAVE, pass);
    clickElement(BOTON_INGRESAR);
}
```

### Error 3 — Thread.sleep en lugar de esperas explicitas

```java
// MAL — tiempo hardcodeado, falla en maquinas lentas, pasa en rapidas
public void esperarCargaDeReporte() {
    Thread.sleep(3000); // prohibido
}

// BIEN — espera la condicion real
public void esperarCargaDeReporte() {
    waitUntilInvisible(SPINNER_CARGA);          // espera que el spinner desaparezca
    waitUntilUrlContains("/reportes/resultado"); // confirma que cargo la pagina
}
```

### Error 4 — Assertions en el Page Object

```java
// MAL — el Page Object no decide si un estado es correcto o no
public void verificarLoginExitoso() {
    assertThat(isElementVisible(PANEL_PRINCIPAL))
        .as("El panel debe ser visible")
        .isTrue(); // esto va en el Step, no aqui
}

// BIEN — el Page Object reporta el estado
public boolean panelPrincipalEsVisible() {
    return isElementVisible(PANEL_PRINCIPAL);
}
// El Step hace: assertThat(loginPage.panelPrincipalEsVisible()).as("...").isTrue();
```

### Error 5 — Locators publicos o no estaticos

```java
// MAL — el locator es publico (un step podria usarlo directamente)
public By campoUsuario = By.id("usuario");

// MAL — el locator no es estatico (se crea una instancia por objeto)
private final By campoUsuario = By.id("usuario");

// BIEN — privado y estatico
private static final By CAMPO_USUARIO = By.id("usuario");
```

---

## Patron completo — un Page Object bien escrito

Este es el estandar completo que debes seguir:

```java
package pages;

import org.openqa.selenium.By;
import java.time.Duration;
import java.util.List;

/**
 * Page Object para la pantalla de transferencias entre cuentas.
 *
 * Encapsula todas las interacciones UI del modulo de transferencias.
 * Los steps nunca deben importar By, WebDriver, ni WebElement.
 */
public class TransferenciasPage extends BasePage {

    // =========================================================================
    // LOCATORS — privados, estaticos, constantes
    // =========================================================================
    private static final By CAMPO_CUENTA_DESTINO  = By.id("cuentaDestino");
    private static final By CAMPO_MONTO           = By.id("montoTransferencia");
    private static final By CAMPO_CONCEPTO        = By.id("concepto");
    private static final By BOTON_CONTINUAR       = By.id("btnContinuar");
    private static final By BOTON_CONFIRMAR       = By.id("btnConfirmar");
    private static final By MODAL_CONFIRMACION    = By.id("modalConfirmacion");
    private static final By NUMERO_OPERACION      = By.cssSelector(".numero-operacion");
    private static final By MENSAJE_EXITO         = By.cssSelector(".alert-success");
    private static final By MENSAJE_ERROR         = By.cssSelector(".alert-danger");
    private static final By SPINNER_PROCESANDO    = By.cssSelector(".spinner-procesando");

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================
    public TransferenciasPage() {
        super();
    }

    // =========================================================================
    // ACCIONES
    // =========================================================================

    public void ingresarDatosDeTransferencia(String cuentaDestino, String monto) {
        write(CAMPO_CUENTA_DESTINO, cuentaDestino);
        write(CAMPO_MONTO, monto);
        clickElement(BOTON_CONTINUAR);
    }

    public void confirmarTransferencia() {
        waitUntilInvisible(SPINNER_PROCESANDO); // esperar que Angular termine de validar
        clickElement(BOTON_CONFIRMAR);
        waitUntilInvisible(MODAL_CONFIRMACION); // esperar que cierre el modal
    }

    // =========================================================================
    // VERIFICACIONES
    // =========================================================================

    public boolean transferenciaRegistradaExitosamente() {
        return isElementVisible(MENSAJE_EXITO);
    }

    public boolean mensajeDeErrorVisible() {
        return isElementVisible(MENSAJE_ERROR);
    }

    public String obtenerNumeroDeOperacion() {
        return find(NUMERO_OPERACION).getText();
    }

    public String obtenerTextoDelError() {
        return findVisible(MENSAJE_ERROR).getText();
    }
}
```

---

## Referencia de documentos relacionados

- [SELENIUM_JAVA_PRACTICES.md](SELENIUM_JAVA_PRACTICES.md) — reglas de codificacion Java y Selenium
- [JAVA_DIRECTORY_STRUCTURE_STANDARD.md](JAVA_DIRECTORY_STRUCTURE_STANDARD.md) — donde va cada archivo
- [CUCUMBER_BDD_STYLE_GUIDE.md](CUCUMBER_BDD_STYLE_GUIDE.md) — como escribir los steps que usan estos Page Objects
- [docs/03-templates/PAGE_OBJECT_TEMPLATE.md](../03-templates/PAGE_OBJECT_TEMPLATE.md) — plantilla lista para copiar
- [src/test/java/pages/BasePage.java](../../src/test/java/pages/BasePage.java) — codigo fuente con documentacion completa
- [src/test/java/pages/LoginPage.java](../../src/test/java/pages/LoginPage.java) — ejemplo real funcionando

---

*Guia de BasePage v1.0 — QA Automation QA Automatizacion — 2026-03-15*
*Documento obligatorio antes de escribir cualquier Page Object nuevo.*
