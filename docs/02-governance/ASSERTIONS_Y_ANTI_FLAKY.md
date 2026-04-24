# Assertions y Tests Anti-Flaky — Guia para QA

> **Para quien es este documento:**
> Para cualquier QA que escriba steps de Cucumber en este framework.
> Lee esto antes de escribir tu primer `assertThat`.
>
> **Objetivo:** que cada assertion del framework diga exactamente
> que fallo, donde fallo, y por que — sin necesidad de inspeccionar logs ni screenshots.

---

## Que es un test flaky y por que es peligroso en banca

Un test **flaky** es un test que a veces pasa y a veces falla sin que el codigo cambie.

En banca, un test flaky tiene dos efectos negativos graves:

1. **El equipo pierde confianza en el suite.** Cuando los QAs dicen "ese test siempre falla por nada", empiezan a ignorar fallos reales porque asumen que son flakiness. Eso es exactamente cuando un bug llega a produccion.

2. **El pipeline se desestabiliza.** Si hay que relanzar pipelines 3 veces hasta que pasen, el ciclo de entrega se duplica y los desarrolladores dejan de confiar en QA.

**Regla del framework:** un test flaky es un BUG de automatizacion. Se trata igual que un bug funcional: se prioriza, se asigna y se corrige antes de avanzar con casos nuevos.

---

## Como escribir assertions correctas

### La libreria obligatoria: AssertJ

El framework usa **AssertJ** para todas las afirmaciones. No uses `assertTrue()` de JUnit ni `assertEquals()`. AssertJ produce mensajes de error mas descriptivos y tiene una API encadenada muy legible.

```java
// MAL — JUnit assertion: mensaje de error inutil
assertTrue(page.panelPrincipalVisible());
// Resultado al fallar: "expected: <true> but was: <false>"
// ¿Que fallo? ¿Cual panel? No sabes nada.

// BIEN — AssertJ con .as(): mensaje autoexplicativo
assertThat(page.panelPrincipalVisible())
    .as("Panel principal debe ser visible tras login exitoso con credenciales validas")
    .isTrue();
// Resultado al fallar: "Panel principal debe ser visible tras login exitoso
//                       con credenciales validas => expected: true but was: false"
```

---

### La regla del `.as()` — SIEMPRE obligatorio

**Cada `assertThat` debe tener un `.as()` con un mensaje descriptivo.** Sin `.as()`, cuando el test falle en CI/CD a las 2am, el oncall no sabra que ocurrio.

El mensaje del `.as()` debe responder: **"¿Que debia ser verdad en este punto del flujo?"**

```java
// Patron obligatorio:
assertThat(<valor_real>)
    .as("<descripcion del negocio: que debia ser verdad y en que contexto>")
    .<condicion>();
```

**Ejemplos buenos de mensajes `.as()`:**

```java
// Verifica navegacion
assertThat(page.getCurrentUrl())
    .as("Tras login exitoso, la URL debe contener /home (el usuario debe estar en el panel principal)")
    .contains("/home");

// Verifica texto
assertThat(page.obtenerSaldoDisponible())
    .as("El saldo disponible debe mostrar S/ 2,500.00 tras la consulta de cuenta ahorros 001-001234")
    .isEqualTo("S/ 2,500.00");

// Verifica visibilidad
assertThat(page.mensajeTransferenciaExitosaVisible())
    .as("El mensaje de exito debe aparecer tras confirmar transferencia de S/ 500 a cuenta BCP")
    .isTrue();

// Verifica cantidad de filas
assertThat(page.obtenerCantidadDeResultados())
    .as("La busqueda por DNI '12345678' debe retornar al menos 1 resultado en la tabla")
    .isGreaterThan(0);
```

---

### Las assertions mas usadas en banca

```java
// Booleanos — visibilidad, habilitacion
assertThat(page.mensajeExitoVisible()).as("...").isTrue();
assertThat(page.mensajeErrorVisible()).as("...").isFalse();
assertThat(page.botonConfirmarHabilitado()).as("...").isTrue();
assertThat(page.campoMontoDeshabilitado()).as("...").isTrue();

// Textos — saldos, nombres, estados
assertThat(page.obtenerSaldo()).as("...").isEqualTo("S/ 1,250.00");
assertThat(page.obtenerNombreCliente()).as("...").contains("Juan");
assertThat(page.obtenerEstadoOperacion()).as("...").isEqualToIgnoringCase("aprobado");
assertThat(page.obtenerMensaje()).as("...").startsWith("Transferencia exitosa");

// URL — navegacion
assertThat(page.getCurrentUrl()).as("...").contains("/transferencias");
assertThat(page.getCurrentUrl()).as("...").doesNotContain("/login");

// Numeros — cantidad de filas, resultados
assertThat(page.obtenerCantidadDeResultados()).as("...").isGreaterThan(0);
assertThat(page.obtenerCantidadDeResultados()).as("...").isEqualTo(5);
assertThat(page.obtenerCantidadDeResultados()).as("...").isBetween(1, 20);

// Listas — columnas de tablas
assertThat(page.obtenerEstadosDeLaTabla()).as("...").containsOnly("Aprobado");
assertThat(page.obtenerEstadosDeLaTabla()).as("...").doesNotContain("Rechazado");

// Tipo de dato para comparar montos
assertThat(page.obtenerMontoNumerico()).as("...").isGreaterThanOrEqualTo(100.0);
```

---

### Como verificar multiples condiciones en un solo Then

En banca, un `Then` puede necesitar verificar varias cosas. Usa SoftAssertions de AssertJ para acumular todos los fallos en lugar de parar en el primero:

```java
// SIN SoftAssertions — para en el primer fallo, oculta el resto
@Entonces("la transferencia debe completarse exitosamente")
public void laTransferenciaDebeCompletarse() {
    assertThat(page.mensajeExitoVisible()).as("Mensaje de exito").isTrue();
    assertThat(page.numeroOperacionVisible()).as("Numero de operacion").isTrue(); // si lo anterior falla, esto no se evalua
    assertThat(page.saldoActualizadoVisible()).as("Saldo actualizado").isTrue();
}

// CON SoftAssertions — evalua todo y reporta todos los fallos juntos
@Entonces("la transferencia debe completarse exitosamente")
public void laTransferenciaDebeCompletarse() {
    org.assertj.core.api.SoftAssertions soft = new org.assertj.core.api.SoftAssertions();

    soft.assertThat(page.mensajeExitoVisible())
        .as("El mensaje 'Transferencia exitosa' debe ser visible")
        .isTrue();

    soft.assertThat(page.numeroOperacionVisible())
        .as("El numero de operacion debe ser visible en pantalla")
        .isTrue();

    soft.assertThat(page.saldoActualizadoVisible())
        .as("El saldo disponible debe reflejar el debito de la transferencia")
        .isTrue();

    soft.assertAll(); // lanza AssertionError con TODOS los fallos juntos
}
```

Usar `SoftAssertions` cuando el `Then` verifica el estado final de una operacion compleja (como una transferencia o apertura de cuenta) donde todos los atributos del resultado son importantes.

---

## Las 8 causas principales de flakiness y como eliminarlas

### 1. Hacer assertions antes de que Angular termine de renderizar

**Patron flaky:**
```java
// En el Step — flaky: Angular puede no haber actualizado el saldo aun
clickElement(BOTON_CONSULTAR);
assertThat(page.obtenerSaldo()).as("Saldo").isEqualTo("S/ 2,500.00");
```

**Causa:** Angular hace la llamada al backend de forma asincrona. El click dispara la peticion, pero el step hace el assert inmediatamente. A veces la respuesta llega antes del assert (pasa), a veces no (falla).

**Solucion — esperar un elemento que confirme que Angular termino:**
```java
// En el Page Object — esperar que el saldo cargue antes de retornarlo
public String obtenerSaldo() {
    waitUntilTextNotEmpty(CAMPO_SALDO);   // espera que el texto no este vacio
    return getText(CAMPO_SALDO);
}

// O esperar que el spinner desaparezca:
public String obtenerSaldo() {
    waitUntilInvisible(SPINNER_CARGA);    // espera que el loading desaparezca
    return getText(CAMPO_SALDO);
}
```

---

### 2. Usar `Thread.sleep()` como espera

**Patron flaky:**
```java
// PROHIBIDO — Thread.sleep es la causa numero uno de flakiness
clickElement(BOTON_BUSCAR);
Thread.sleep(3000); // "espero 3 segundos por si acaso"
assertThat(page.tablaResultadosVisible()).isTrue();
```

**Por que es flaky:** en un servidor lento (CI/CD bajo carga, paralelismo), 3 segundos pueden no ser suficientes. En un servidor rapido, desperdicias 3 segundos por nada.

**Solucion — espera explicita por el evento que importa:**
```java
// CORRECTO — espera exactamente hasta que la tabla sea visible
public void buscarCliente(String dni) {
    write(CAMPO_DNI, dni);
    clickAndWaitForElement(BOTON_BUSCAR, TABLA_RESULTADOS); // espera que la tabla aparezca
}
```

```
┌─────────────────────────────────────────────────────────────┐
│  REGLA ABSOLUTA: Thread.sleep() está PROHIBIDO en el         │
│  código de tests. La única excepción es waitSmall() de       │
│  BasePage (150ms), usable solo para estabilización de        │
│  animaciones CSS cuando NO existe una condición explícita.   │
└─────────────────────────────────────────────────────────────┘
```

---

### 3. Escribir en campos sin verificar que Angular registró el valor

**Patron flaky:**
```java
// FLAKY — element.clear() no dispara el evento input de Angular
public void ingresarMonto(String monto) {
    WebElement campo = driver.findElement(CAMPO_MONTO);
    campo.clear();       // Angular NO ve este evento
    campo.sendKeys(monto); // Angular VE este evento, pero el modelo puede quedar corrupto
}
```

**Causa:** `element.clear()` no dispara el evento `input` de Angular. El modelo reactivo del formulario puede quedarse con el valor anterior, el boton "Confirmar" permanece deshabilitado aunque el campo se vea lleno, y el test pasa visualmente pero falla funcionalmente.

**Solucion — usar el metodo `write()` del framework (ya corregido):**
```java
// CORRECTO — write() usa CTRL+A + sendKeys que si dispara el evento input
public void ingresarMonto(String monto) {
    write(CAMPO_MONTO, monto);
}

// Para campos criticos (monto, cuenta, DNI) — verifica que el valor fue aceptado:
public void ingresarMontoDeTransferencia(String monto) {
    writeAndVerify(CAMPO_MONTO, monto); // escribe y verifica que el campo acepto el valor
    waitUntilEnabled(BOTON_CONFIRMAR);  // espera que Angular habilite el boton
}
```

---

### 4. Contar filas de tabla sin esperar que carguen

**Patron flaky:**
```java
// FLAKY — la tabla puede estar cargando cuando se ejecuta esto
public int obtenerCantidadDeResultados() {
    return driver.findElements(FILAS_TABLA).size(); // retorna 0 si aun carga
}
```

**Solucion:**
```java
// CORRECTO — getTableRowCount() espera que las filas sean visibles
public int obtenerCantidadDeResultados() {
    return getTableRowCount(FILAS_TABLA); // usa findAll() que espera
}

// Para el step — espera explicita si el conteo es el punto de verificacion:
@Entonces("la busqueda debe retornar {int} resultados")
public void laBusquedaDebeRetornar(int cantidad) {
    waitUntilCountAtLeast(FILAS_TABLA, 1); // primero espera que haya al menos 1
    assertThat(page.obtenerCantidadDeResultados())
        .as("La busqueda debe retornar exactamente " + cantidad + " resultados")
        .isEqualTo(cantidad);
}
```

---

### 5. Hacer click en elementos tapados por overlays (spinners, modales en animacion)

**Cobertura automatica de BasePage (Marzo 2026):**
```java
// clickElement() ya maneja ElementClickInterceptedException internamente:
// Intento 1: overlay presente → waitSmall() y reintenta
// Intento 2: overlay presente → waitSmall() y reintenta
// Intento 3: overlay persiste → jsClick() como fallback automatico
clickElement(BOTON_CONFIRMAR); // nunca lanza ElementClickInterceptedException al caller
```

**Cuando ANTICIPAR el overlay explicitamente (recomendado para operaciones lentas):**
```java
// Si la operacion anterior tarda varios segundos, anticipar la espera
// es mas rapido que esperar los 3 reintentos automaticos de clickElement:
public void confirmarSegundaOperacion() {
    waitUntilInvisible(SPINNER_PROCESANDO);  // ahorra 2 reintentos innecesarios
    clickElement(BOTON_CONFIRMAR);
}
```

---

### 6. StaleElementReferenceException por re-renders de Angular

**Patron flaky:**
```java
// FLAKY — Angular puede re-renderizar el DOM entre el find y el click
WebElement boton = driver.findElement(BOTON_GUARDAR);
// ... Angular re-renderiza aqui ...
boton.click(); // StaleElementReferenceException
```

**Solucion — nunca guardes referencias a WebElement en variables locales del Page Object. Usa siempre los metodos del framework:**
```java
// CORRECTO — clickElement() reintenta hasta 3 veces ante StaleElement
public void guardarCambios() {
    clickElement(BOTON_GUARDAR); // maneja StaleElement internamente
}

// Si necesitas una referencia para multiples operaciones en la MISMA accion:
public void escribirYConfirmar(String valor) {
    // write() ya maneja StaleElement internamente
    write(CAMPO_VALOR, valor);
    // clickElement() ya maneja StaleElement internamente
    clickElement(BOTON_ACEPTAR);
    // No guardes references entre llamadas separadas
}
```

**Regla:** nunca guardes un `WebElement` en un campo de instancia ni entre llamadas de metodo. Los elementos son validos solo durante la interaccion inmediata.

---

### 7. Tests que dependen del orden de ejecucion

**Patron flaky:**
```java
// FLAKY — el Escenario B asume que el Escenario A ya creo el cliente
// Si Cucumber ejecuta B antes de A (o en paralelo), falla

// Escenario A: Crear cliente
// Escenario B: Buscar el cliente creado por A
```

**Solucion — cada escenario es completamente independiente:**
```java
// CORRECTO — el Escenario B crea sus propios datos de prueba
// Si el cliente ya existe, el Background lo maneja; si no, lo crea en el Given

// Escenario B: Buscar cliente
// Given el cliente con DNI "12345678" existe en el sistema  <- datos propios
// When busco por DNI "12345678"
// Then aparece en los resultados
```

---

### 8. Locators inestables que cambian con deploy

**Patron flaky:**
```java
// FLAKY — el XPath con posicion numerica falla si alguien agrega un elemento
private static final By BOTON_GUARDAR = By.xpath("//div[3]/button[2]");

// FLAKY — clase CSS generada automaticamente por Angular
private static final By CAMPO_NOMBRE = By.cssSelector(".mat-input-element.ng-valid.ng-dirty");
```

**Solucion — orden de estabilidad de locators:**
```
1. By.id("id-unico")                    → mas estable, cambia solo si el dev lo cambia
2. By.cssSelector("[data-testid='xxx']") → segundo mejor, atributo dedicado a QA
3. By.name("nombre-campo")              → estable para formularios HTML nativos
4. By.cssSelector(".clase-semantica")   → aceptable si la clase es de negocio, no generada
5. By.xpath("//boton[@type='submit']")  → solo si no hay alternativa mas especifica
6. By.xpath("//div[3]/button[2]")       → NUNCA — posicional, rompe con cualquier cambio
```

**Pedir a desarrollo que agregue `data-testid` en elementos criticos es valido y recomendado.** Es una practica estandar en banca moderna.

---

## Patrones anti-flaky listos para copiar

### Patron 1 — Accion + espera del resultado (el mas importante)

```java
// En el Page Object:
public void buscarClientePorDni(String dni) {
    write(CAMPO_DNI, dni);
    clickAndWaitForElement(BOTON_BUSCAR, TABLA_RESULTADOS);
    // NO retorna hasta que la tabla sea visible
}

// En el Step:
@Cuando("busca al cliente con DNI {string}")
public void buscaAlClienteConDni(String dni) {
    page.buscarClientePorDni(dni); // ya incluye la espera
}

@Entonces("debe aparecer en los resultados")
public void debeAparecerEnLosResultados() {
    assertThat(page.clienteApareceEnResultados(dni))
        .as("El cliente debe aparecer en la tabla tras la busqueda")
        .isTrue();
    // NO necesitas Thread.sleep aqui — la busqueda ya espero la tabla
}
```

---

### Patron 2 — Formulario con validacion Angular (boton deshabilitado)

```java
// En el Page Object:
public void ingresarDatosDeTransferencia(String cuenta, String monto, String concepto) {
    write(CAMPO_CUENTA_DESTINO, cuenta);
    write(CAMPO_MONTO, monto);
    write(CAMPO_CONCEPTO, concepto);
    // Angular valida el formulario y habilita el boton
    waitUntilEnabled(BOTON_SIGUIENTE);
}

public void continuarAlSiguientePaso() {
    clickAndWaitForElement(BOTON_SIGUIENTE, PANEL_CONFIRMACION);
}

// En el Step:
@Cuando("ingresa los datos de la transferencia y avanza")
public void ingresaDatosYAvanza(DataTable datos) {
    Map<String, String> d = datos.asMap();
    page.ingresarDatosDeTransferencia(d.get("cuenta"), d.get("monto"), d.get("concepto"));
    page.continuarAlSiguientePaso();
}
```

---

### Patron 3 — Verificacion de tabla con datos dinamicos

```java
// En el Page Object:
public boolean operacionApareceEnHistorial(String numeroOperacion) {
    // waitUntilCountAtLeast espera que haya filas antes de buscar en ellas
    waitUntilCountAtLeast(FILAS_HISTORIAL, 1);
    return isTextPresentInTable(FILAS_HISTORIAL, numeroOperacion);
}

public List<String> obtenerEstadosDeOperaciones() {
    return getColumnValues(COLUMNA_ESTADO_HISTORIAL);
}

// En el Step:
@Entonces("la operacion {string} debe aparecer en el historial")
public void laOperacionDebeAparecerEnHistorial(String numOp) {
    assertThat(page.operacionApareceEnHistorial(numOp))
        .as("La operacion " + numOp + " debe aparecer en el historial de movimientos")
        .isTrue();
}

@Entonces("todas las operaciones deben estar en estado Aprobado")
public void todasLasOperacionesDebEstarAprobadas() {
    assertThat(page.obtenerEstadosDeOperaciones())
        .as("Todas las operaciones del historial deben tener estado Aprobado")
        .containsOnly("Aprobado");
}
```

---

### Patron 4 — Verificacion de descarga de archivo

```java
// En el Page Object:
public void exportarReporteDeClientes() {
    deleteDownloadedFile("reporte_clientes");     // limpiar descarga anterior
    clickElement(BOTON_EXPORTAR);
    waitUntilFileDownloaded("reporte_clientes", Duration.ofSeconds(30));
}

// En el Step:
@Entonces("el reporte debe descargarse correctamente")
public void elReporteDebeDescargarse() {
    assertThat(page.reporteDescargadoExitosamente())
        .as("El archivo reporte_clientes.xlsx debe estar en la carpeta de descargas")
        .isTrue();
}
```

---

### Patron 5 — Verificar campo de monto critico

```java
// En el Page Object — para montos usa writeAndVerify para mayor seguridad:
public void ingresarMontoDeTransferencia(String monto) {
    writeAndVerify(CAMPO_MONTO, monto);
    // writeAndVerify escribe y verifica que el campo acepto el valor exacto
    // si el campo tiene mascara de formato (ej: "1,000.00" en vez de "1000")
    // el test fallara con mensaje claro en lugar de continuar con dato incorrecto
}

// En el Step:
@Cuando("ingresa el monto {string}")
public void ingresaElMonto(String monto) {
    page.ingresarMontoDeTransferencia(monto);
}
```

---

## Checklist anti-flaky antes de hacer commit

```
□  Cada assertThat tiene un .as() con descripcion en español de negocio
□  No hay Thread.sleep() en ningun step ni page object (solo waitSmall() de BasePage)
□  Los clicks que navegan a otra pantalla usan clickAndWaitForElement()
   o tienen waitUntilInvisible(SPINNER) o findVisible(ELEMENTO_RESULTADO) despues
□  Los campos de formulario usan write() o writeAndVerify() (nunca element.clear())
□  Las verificaciones de tabla usan getTableRowCount() o isTextPresentInTable()
   (nunca driver.findElements().size() directamente)
□  Los textos dinamicos de Angular usan waitUntilTextNotEmpty() si son criticos
□  Los locators no tienen posicion numerica (//div[3]/button[2])
□  Los locators no usan clases CSS generadas automaticamente por Angular
□  Cada escenario tiene sus propios datos de prueba (no depende de otro escenario)
□  Los steps no guardan referencias a WebElement entre llamadas
□  No hay logica de espera en los Steps (toda espera va en el Page Object)
```

---

## Que hacer cuando un test es flaky

Si un test falla intermitentemente, sigue este proceso antes de aumentar timeouts al azar:

```
1. REPRODUCIR — ejecutar el test 5 veces seguidas. ¿Cuantas veces falla?
   - Siempre falla → no es flaky, es un bug real
   - 1 de 5 → flakiness leve, probablemente una espera faltante
   - 3 de 5 → flakiness severa, buscar la causa raiz

2. LEER EL MENSAJE DE ERROR — el mensaje de AssertJ debe decirte que esperaba
   vs que obtuvo. Si el mensaje es vago, eso tambien es un bug (del assertion).

3. IDENTIFICAR LA CAUSA usando esta tabla:
   ┌────────────────────────────────────┬───────────────────────────────────┐
   │ Error                              │ Causa probable                    │
   ├────────────────────────────────────┼───────────────────────────────────┤
   │ expected: true but was: false      │ Espera insuficiente antes assert  │
   │ (elemento visible)                 │ → agregar waitUntilVisible/etc.   │
   ├────────────────────────────────────┼───────────────────────────────────┤
   │ StaleElementReferenceException     │ Angular re-renderizo el DOM       │
   │                                    │ → usar metodos de BasePage        │
   ├────────────────────────────────────┼───────────────────────────────────┤
   │ ElementClickInterceptedException   │ Overlay encima del elemento       │
   │                                    │ → waitUntilInvisible(SPINNER)     │
   ├────────────────────────────────────┼───────────────────────────────────┤
   │ TimeoutException en clickElement   │ Elemento no clickeable            │
   │                                    │ → waitUntilEnabled o scroll       │
   ├────────────────────────────────────┼───────────────────────────────────┤
   │ expected: "S/ 500.00" but was: ""  │ Angular no actualizo el texto     │
   │                                    │ → waitUntilTextNotEmpty           │
   ├────────────────────────────────────┼───────────────────────────────────┤
   │ expected: 5 rows but was: 0        │ Tabla aun cargando al hacer count │
   │                                    │ → waitUntilCountAtLeast(locator,1)│
   └────────────────────────────────────┴───────────────────────────────────┘

4. CORREGIR LA CAUSA RAIZ — no aumentes el timeout si el problema es
   una espera en el lugar incorrecto. Busca el evento correcto a esperar.

5. VERIFICAR — correr el test 5 veces seguidas despues de la correccion.
   Solo si pasa las 5 veces puede considerarse resuelto.
```

---

## Lo que NUNCA debes hacer en un step

```java
// MAL — el step sabe de Selenium (viola la separacion de responsabilidades)
@Entonces("debe verse el panel")
public void debeVersElPanel() {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    WebElement panel = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("panel")));
    assertThat(panel.isDisplayed()).isTrue();
}

// BIEN — el step solo hace la asercion, el page object maneja Selenium
@Entonces("debe verse el panel")
public void debeVersElPanel() {
    assertThat(page.panelPrincipalVisible())
        .as("El panel principal debe ser visible tras el login")
        .isTrue();
}
```

```java
// MAL — assertion sin .as() → mensaje de error inutil al fallar en CI
assertThat(page.saldoVisible()).isTrue();

// BIEN — assertion con contexto completo
assertThat(page.saldoVisible())
    .as("El saldo de la cuenta debe ser visible en el panel tras consultar cuenta 001-123456")
    .isTrue();
```

```java
// MAL — comparar texto con trim() manual porque el texto tiene espacios
assertThat(page.obtenerEstado().trim()).isEqualTo("Aprobado");

// BIEN — el page object retorna texto limpio; los metodos getText() y getColumnValues()
// ya hacen .trim() internamente. Si el texto tiene espacios es un bug del page object.
assertThat(page.obtenerEstado()).isEqualTo("Aprobado");
```

---

*Documento v1.0 — QA Automation QA Automatizacion — 2026-03-15*
*Revision sugerida: cuando se incorporen nuevas versiones de AssertJ o nuevos patrones de Angular.*
