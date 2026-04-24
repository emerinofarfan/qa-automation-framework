# Patrones de Diseño del Framework QA

> Documentación obligatoria para QA de todos los niveles.
> Entender estos patrones evita el 80% de los errores más comunes en el equipo.

---

## Por qué existe este documento

El framework no usa un solo patrón — usa seis.
Un QA que solo conoce Page Object Model tomará decisiones incorrectas al escribir código:
llamará a Selenium directamente en los steps, usará variables estáticas, duplicará lógica de setup.
Este documento explica qué patrón resuelve qué problema y cómo interactúas con él según tu rol.

---

## Los seis patrones del framework

| # | Patrón | Clase principal | Quién lo usa activamente |
|---|--------|----------------|--------------------------|
| 1 | Page Object Model (POM) | `pages/LoginPage.java` | Todo QA que escriba tests |
| 2 | Facade | `pages/BasePage.java` | Todo QA (sin saberlo) |
| 3 | Interceptor (Hooks) | `hooks/Hooks.java` | Arquitectura QA solamente |
| 4 | Factory Method | `utils/DriverFactory.java`, `utils/AutomationException.java` | QA Senior / Arquitectura |
| 5 | Builder | `models/UsuarioTestData.java` | QA que gestione datos complejos |
| 6 | ThreadLocal | Interno en `DriverFactory` | Arquitectura QA solamente |

---

## Patrón 1 — Page Object Model (POM)

### Qué problema resuelve

Sin POM, cada step llama a Selenium directamente. Si el ID de un botón cambia,
hay que buscar y corregir en decenas de steps. Con POM, el locator vive en un
solo lugar: la clase Page. Un cambio en un solo archivo corrige toda la suite.

### Cómo funciona en este framework

```
Feature (lenguaje negocio)
    → Step (orquesta el flujo)
        → Page Object (interactúa con la UI)
            → BasePage (métodos Selenium seguros)
```

```java
// Step — solo orquesta, nunca llama Selenium
@Cuando("hace clic en el botón de ingresar")
public void haceClicEnBotonIngresar() {
    loginPage.presionarBotonIngresar();   // delega al Page Object
}

// Page Object — encapsula locators y acciones
public class LoginPage extends BasePage {
    private static final By BOTON_INGRESAR = By.cssSelector("button[type='submit']");

    public void presionarBotonIngresar() {
        clickElement(BOTON_INGRESAR);     // usa BasePage, nunca driver.findElement()
    }
}
```

### Regla crítica

```java
// ❌ NUNCA en un Step — viola POM
@Cuando("hace clic en el botón de ingresar")
public void haceClicEnBotonIngresar() {
    driver.findElement(By.cssSelector("button[type='submit']")).click();
}

// ✅ SIEMPRE — el Step llama al Page Object
@Cuando("hace clic en el botón de ingresar")
public void haceClicEnBotonIngresar() {
    loginPage.presionarBotonIngresar();
}
```

### Señal de que lo estás haciendo mal

Si ves `driver.findElement()` o `By.` dentro de un archivo de `steps/`, el patrón está roto.
El paquete `steps/` nunca debe importar `org.openqa.selenium.By`.

---

## Patrón 2 — Facade (BasePage)

### Qué problema resuelve

Selenium tiene una API compleja: `WebDriverWait`, `ExpectedConditions`, `FluentWait`,
manejo de `StaleElementReferenceException`, reintentos, screenshots en fallo.
Si cada Page Object implementara esa lógica, habría código duplicado y tests frágiles.

`BasePage` es la **fachada**: esconde toda la complejidad de Selenium detrás de
métodos simples que cualquier QA puede usar sin conocer los detalles internos.

### Cómo funciona en este framework

```java
// Lo que tú escribes en tu Page Object:
public void presionarBotonIngresar() {
    clickElement(BOTON_INGRESAR);   // simple
}

// Lo que BasePage hace internamente (tú no ves esto):
// 1. Espera hasta 20s que el elemento sea clickeable
// 2. Si hay StaleElementReferenceException, reintenta hasta 3 veces
// 3. Si falla, captura screenshot automáticamente
// 4. Lanza AutomationException con contexto (locator, acción, causa raíz)
```

### Métodos disponibles en BasePage

| Método | Cuándo usarlo |
|--------|---------------|
| `write(By, String)` | Escribir en un campo de texto |
| `clickElement(By)` | Hacer clic con reintentos automáticos |
| `findVisible(By)` | Obtener elemento visible (con espera) |
| `findAll(By)` | Obtener lista de elementos |
| `countElements(By)` | Contar filas de una tabla |
| `isElementVisible(By)` | Verificar si un elemento está visible (5s) |
| `isElementVisible(By, Duration)` | Verificar con timeout personalizado |
| `getText(By)` | Leer el texto de un elemento |
| `getAttributeValue(By, String)` | Leer un atributo HTML |

Referencia completa: [`docs/02-governance/BASEPAGE_GUIDE.md`](BASEPAGE_GUIDE.md)

### Regla crítica

```java
// ❌ NUNCA — saltas la fachada, pierdes reintentos, waits y screenshots
public void presionarBotonIngresar() {
    driver.findElement(BOTON_INGRESAR).click();
}

// ✅ SIEMPRE — la fachada gestiona todo
public void presionarBotonIngresar() {
    clickElement(BOTON_INGRESAR);
}
```

### Señal de que lo estás haciendo mal

Si ves `driver.findElement()` o `driver.findElements()` dentro de un archivo de `pages/`,
estás saltando la fachada. Usa siempre los métodos heredados de BasePage.

---

## Patrón 3 — Interceptor / Hooks

### Qué problema resuelve

Cada escenario necesita las mismas acciones antes y después de ejecutarse:
abrir el browser, navegar a la URL, capturar screenshot si falla, cerrar el driver.
Sin interceptores, cada step repetiría ese código. Con Hooks, se define una sola vez
y Cucumber lo aplica automáticamente a todos los escenarios.

### Cómo funciona en este framework

```
@Before (orden 0)         → abre Chrome, navega a BASE_URL, configura waits
    Escenario Given/When/Then
@AfterStep (orden 999)    → enmascara datos sensibles en el reporte Allure
@After (orden 0)          → captura screenshot si el escenario falló, cierra driver
```

### Regla crítica para el QA Ejecutor

**Nunca pongas setup o teardown en los steps.** El Hook ya lo hace.

```java
// ❌ NUNCA en un Step
@Dado("el usuario abre el sistema")
public void elUsuarioAbreElSistema() {
    driver = new ChromeDriver();          // ya lo hace Hooks.setUp()
    driver.get("https://app.example.pe"); // ya lo hace Hooks.setUp()
}

// ✅ CORRECTO — el Given asume que el browser ya está abierto
@Dado("el usuario abre el sistema")
public void elUsuarioAbreElSistema() {
    loginPage.verificarPaginaCargada();   // solo valida el estado
}
```

### Señal de que lo estás haciendo mal

Si ves `new ChromeDriver()`, `driver.get(url)` o `driver.quit()` dentro de `steps/`,
el setup está duplicado. Borra esas líneas — Hooks ya las ejecuta.

---

## Patrón 4 — Factory Method

### Qué problema resuelve

Algunos objetos son complejos de crear correctamente (el WebDriver necesita opciones
de Chrome, proxy corporativo, timeout, headless según el ambiente).
El Factory Method oculta esa complejidad detrás de un método estático simple.

### Dónde aparece en este framework

**`DriverFactory`** — crea el WebDriver por ti:

```java
// Lo que tú nunca haces:
WebDriver driver = new ChromeDriver(options);  // ❌ complejo y frágil

// Lo que Hooks llama por ti:
DriverFactory.initDriver();    // ✅ fábrica con toda la configuración correcta
WebDriver driver = DriverFactory.getDriver();  // ✅ obtiene el driver del hilo actual
```

**`AutomationException`** — crea excepciones con contexto rico:

```java
// Lo que tú haces en un Page Object cuando algo falla:
throw AutomationException.timeout("esperarPanel", "Panel de resultados", locator, e);

// El mensaje que aparece en el reporte:
// [TIMEOUT] esperarPanel en 'Panel de resultados'
//   Locator : By.cssSelector: .panel-resultados
//   Causa   : Element not visible after 30 seconds
```

### Regla para el QA Ejecutor

- Nunca instancies `DriverFactory` con `new` — el constructor es privado por diseño.
- Usa los factory methods de `AutomationException` en lugar de `throw new RuntimeException(e)`.

---

## Patrón 5 — Builder

### Qué problema resuelve

Cuando un test necesita 4 o más datos sobre el mismo objeto (nombre, perfil, estado, rol),
pasar todos como parámetros separados al step genera métodos ilegibles.
El Builder permite crear objetos de datos de forma fluida, con solo los campos necesarios.

### Cuándo usarlo

- Cuando **3 o más steps del mismo escenario** comparten los mismos datos de prueba.
- Cuando los datos del objeto son opcionales o varían entre escenarios.

```java
// ❌ Sin Builder — paso de parámetros inmanejable
loginPage.registrarUsuario("Ana García", "Analista QA", true, "Lima", "ana@example.pe");

// ✅ Con Builder — legible y extensible
UsuarioTestData usuario = UsuarioTestData.builder()
        .nombre("Ana García")
        .perfil("Analista QA")
        .activo(true)
        .build();

loginPage.registrarUsuario(usuario);
```

### Cuándo NO usarlo

- Si el step solo pasa 1 o 2 datos: usa parámetros directos, el Builder es innecesario.
- Si los datos vienen de un DataTable: usa `asMaps()` directamente, sin Builder intermedio.

### Cómo crear tu propio modelo de datos

Copia la estructura de [`src/test/java/models/UsuarioTestData.java`](../../src/test/java/models/UsuarioTestData.java)
y adapta los campos al dominio de tu módulo.

---

## Patrón 6 — ThreadLocal (solo para entenderlo)

### Qué problema resuelve

El framework corre 3 escenarios en paralelo. Si hubiera una sola instancia de `WebDriver`
compartida entre los 3 hilos, los escenarios se interferirían entre sí.
`ThreadLocal` garantiza que **cada hilo tiene su propio `WebDriver`** completamente aislado.

### Por qué te importa como QA Ejecutor

No tienes que implementar nada. Pero sí tienes que respetar una regla que deriva de este patrón:

```java
// ❌ NUNCA — variable estática = compartida entre hilos = fallos en paralelo
public class LoginPage extends BasePage {
    private static String ultimoMensaje;   // Bug de paralelismo
}

// ✅ SIEMPRE — campo de instancia = cada escenario tiene su propio valor
public class LoginPage extends BasePage {
    // no hay campos de instancia aquí excepto locators (que son final static)
}

// ✅ SIEMPRE — en Steps, el estado compartido entre Given/When/Then
//    usa campos de instancia (no static)
public class LoginSteps {
    private String mensajeCapturado;       // Ok: campo de instancia
}
```

### Señal de que lo estás haciendo mal

Si ves `static String`, `static boolean` o cualquier campo `static` mutable
en `pages/` o `steps/`, hay un bug de paralelismo latente.
Los únicos `static` válidos en pages son los locators (`static final By`).

---

## Resumen: qué toca cada rol

| Patrón | QA Ejecutor | QA Senior / Referente Técnico | QA Arquitectura |
|--------|---------------------|-----------|-----------------|
| Page Object Model | Crea pages y steps | Revisa calidad | Define estándar |
| Facade (BasePage) | Usa sus métodos, nunca driver directo | Reporta gaps | Mantiene BasePage |
| Interceptor (Hooks) | No lo toca | Propone nuevos hooks | Define lifecycle |
| Factory Method | Usa AutomationException | Extiende fábricas | Diseña nuevas |
| Builder | Usa builder() en steps | Crea nuevos modelos | Define política models/ |
| ThreadLocal | Respeta regla de no-static | Detecta violaciones | Gestiona pool |

---

## Los tres errores más costosos que evitan estos patrones

### Error 1 — Llamar Selenium en Steps (viola POM + Facade)
```java
// Consecuencia: si el ID cambia, buscas el error en 20 archivos
@Cuando("hace clic en ingresar")
public void haceClicEnIngresar() {
    driver.findElement(By.id("btnLogin")).click();  // ❌
}
```

### Error 2 — Variables static en Steps o Pages (viola ThreadLocal)
```java
// Consecuencia: en paralelo, el escenario B sobreescribe el dato del escenario A
public class LoginSteps {
    private static String resultado;  // ❌ Bug de paralelismo
}
```

### Error 3 — Setup/teardown en Steps (viola Interceptor)
```java
// Consecuencia: el driver se abre dos veces, la URL se navega dos veces
@Dado("el sistema está disponible")
public void elSistemaEstaDisponible() {
    driver.get("https://app.example.pe");  // ❌ Hooks ya lo hace
}
```

---

## Lectura complementaria

- [`BASEPAGE_GUIDE.md`](BASEPAGE_GUIDE.md) — todos los métodos de BasePage disponibles
- [`SELENIUM_JAVA_PRACTICES.md`](SELENIUM_JAVA_PRACTICES.md) — reglas técnicas de paralelismo
- [`src/test/java/examples/EjemploPatronesSteps.java`](../../src/test/java/examples/EjemploPatronesSteps.java) — catálogo de patrones ejecutables
- [`src/test/java/models/UsuarioTestData.java`](../../src/test/java/models/UsuarioTestData.java) — ejemplo de Builder para datos de prueba

---

*Documento de gobernanza QA — QA Automation. Versión 1.0.*
