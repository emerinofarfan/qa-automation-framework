# Las herramientas del framework y por qué las usamos

> Lee esto ANTES de `COMO_FUNCIONA_EL_FRAMEWORK.md`.
> No tiene código. Solo explicaciones en lenguaje humano.
> Si ya entiendes qué es Selenium, Cucumber y WebDriver, puedes saltarte este documento.

---

## El problema que resuelve la automatización

Como tester manual, cada vez que quieres verificar que el login funciona debes:
1. Abrir Chrome
2. Ir a la URL
3. Escribir usuario y contraseña
4. Hacer clic en "Ingresar"
5. Verificar que llegaste al panel principal
6. Cerrar el navegador

Si tienes 50 funcionalidades que verificar antes de cada release, eso son 50 repeticiones del mismo ritual. Si el equipo lanza 3 releases por semana, son 150 ejecuciones manuales semanales. Cansadas, propensas a errores humanos, y que no escalan.

**La automatización hace exactamente lo mismo, pero en código.** El código no se cansa, no se distrae, corre a las 2am si es necesario, y repite la misma verificación 500 veces con resultado idéntico.

El único costo es escribir ese código una vez. Después, el computador trabaja por ti.

---

## Las 6 herramientas y su rol

Piensa en el equipo de automatización como una orquesta. Cada instrumento tiene un rol específico. Ninguno reemplaza al otro.

```
┌─────────────────────────────────────────────────────────────────┐
│                    LO QUE TÚ ESCRIBES                           │
│                                                                 │
│   Cucumber/Gherkin  ─── describe QUÉ debe pasar (negocio)      │
│   Java              ─── describe CÓMO ejecutarlo (código)       │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                    LO QUE CONTROLA EL NAVEGADOR                 │
│                                                                 │
│   Selenium          ─── el "control remoto" del navegador       │
│   WebDriver/ChromeDriver ── el puente entre código y Chrome     │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                    LO QUE CONSTRUYE Y REPORTA                   │
│                                                                 │
│   Gradle            ─── el constructor del proyecto             │
│   Allure            ─── el generador de reportes               │
└─────────────────────────────────────────────────────────────────┘
```

---

## Herramienta 1: Selenium — el control remoto del navegador

**¿Qué es?**
Selenium es una librería de Java que permite controlar Chrome (o cualquier
navegador) desde código. Es como tener un control remoto de Chrome que
obedece instrucciones Java.

**¿Por qué existe?**
Antes de Selenium (años 2000), automatizar un navegador era extremadamente
difícil. Selenium estandarizó cómo los lenguajes de programación hablan con
los navegadores. Hoy es el estándar mundial para automatización web.

**¿Qué puede hacer?**
Exactamente lo que haría un humano frente al navegador:
- Abrir una URL
- Hacer click en un botón
- Escribir en un campo de texto
- Leer el texto de un elemento
- Tomar una captura de pantalla
- Navegar hacia atrás/adelante

**Lo que Selenium NO hace:**
- No decide qué probar (eso lo hace Cucumber/Gherkin)
- No genera reportes bonitos (eso lo hace Allure)
- No organiza el proyecto (eso lo hace Gradle)

**Analogía:** Selenium es como el brazo robótico en una fábrica. Es preciso
y no se cansa. Pero necesita instrucciones de alguien (Java) para saber
qué hacer, y necesita una herramienta física (ChromeDriver) para interactuar
con la máquina (Chrome).

---

## Herramienta 2: WebDriver y ChromeDriver — el puente

**¿Qué es WebDriver?**
WebDriver es el protocolo (el "idioma") que usa Selenium para comunicarse
con el navegador. Es un estándar definido por el W3C (el organismo que
define las reglas de la web).

Piénsalo como el protocolo HTTP: cuando tu navegador pide una página web,
usa HTTP. Cuando Selenium le da órdenes a Chrome, usa WebDriver.

**¿Qué es ChromeDriver?**
ChromeDriver es un programa ejecutable que Google provee gratuitamente.
Es el intérprete entre Selenium y Chrome:
- Selenium habla WebDriver (el estándar)
- Chrome habla su propio lenguaje interno
- ChromeDriver traduce entre los dos

```
Tu código Java
     │
     │ instrucción: "haz click en el botón"
     ▼
  Selenium
     │
     │ protocolo WebDriver (HTTP/JSON)
     ▼
 ChromeDriver  ←── programa que Google provee (se descarga automático)
     │
     │ lenguaje interno de Chrome
     ▼
  Chrome  ←── el navegador real que se abre en tu pantalla
```

**¿Por qué el framework descarga ChromeDriver automáticamente?**
El ChromeDriver debe ser de la misma versión que tu Chrome instalado.
Si Chrome se actualiza, ChromeDriver también debe actualizarse.
La librería `WebDriverManager` hace eso automáticamente al inicio de cada
ejecución. Tú no tienes que hacer nada.

**Analogía:** WebDriver es como el protocolo USB. ChromeDriver es el cable
USB específico para tu modelo de teléfono. Selenium usa ese cable para
comunicarse con Chrome (el teléfono).

---

## Herramienta 3: Cucumber y Gherkin — el lenguaje del negocio

**¿Qué es Cucumber?**
Cucumber es un framework de pruebas que permite escribir los escenarios
de test en lenguaje natural (casi como prosa) en lugar de código puro.

Esos escenarios escritos en lenguaje natural se llaman **Features**.
El lenguaje en que se escriben se llama **Gherkin**.

**¿Qué es Gherkin?**
Gherkin es un lenguaje con palabras clave específicas:
- **Feature:** el nombre de la funcionalidad que pruebas
- **Scenario:** una situación específica a verificar
- **Given:** el estado inicial (precondición)
- **When:** la acción que hace el usuario
- **Then:** el resultado esperado
- **And:** continúa el Given, When o Then anterior

```gherkin
Feature: Inicio de sesión

  Scenario: Login exitoso con credenciales válidas
    Given el usuario está en la pantalla de login
    When ingresa su usuario y contraseña correctos
    And hace clic en el botón Ingresar
    Then debe ver el panel principal del sistema
```

**¿Por qué usar Cucumber en lugar de código Java puro?**

Sin Cucumber, el test se escribiría así (solo código Java, ilegible para el negocio):
```java
driver.get("https://sistema.example.pe/auth");
driver.findElement(By.id("usuario")).sendKeys("admin");
driver.findElement(By.id("clave")).sendKeys("pass123");
driver.findElement(By.id("btnIngresar")).click();
Assert.assertTrue(driver.findElement(By.id("panel")).isDisplayed());
```

Con Cucumber, el mismo test se lee como en el ejemplo de arriba.
Un gerente, analista o cliente puede leerlo y entender qué prueba hace.
Eso hace que los tests sean también documentación viva del sistema.

**Analogía:** Gherkin es como un contrato legal escrito en español simple
en lugar de lenguaje jurídico. El contenido es el mismo, pero cualquiera puede leerlo.

---

## Herramienta 4: Java — el lenguaje de programación

**¿Por qué Java y no Python, JavaScript u otro lenguaje?**

Tres razones concretas para QA Automation:

1. **El sistema a probar está hecho en Java (Spring Boot).** Usar el mismo
   lenguaje facilita la colaboración con el equipo de desarrollo, que puede
   revisar y entender los tests.

2. **Herramientas bancarias enterprise usan Java.** Selenium, JUnit, Cucumber
   y las herramientas de reporte (Allure, JaCoCo) tienen soporte nativo en Java.

3. **El equipo puede crecer.** Un desarrollador Java puede contribuir al
   framework sin aprender un lenguaje nuevo.

**¿Cuánto Java necesitas saber para escribir tests?**

Solo los conceptos básicos:
- Una **clase** es como una plantilla (por ejemplo, `LoginPage` es la plantilla
  de la pantalla de login)
- Un **método** es una acción que hace esa clase (por ejemplo, `clickBotonIngresar()`)
- **Extends** significa "hereda de" (por ejemplo, `LoginPage extends BasePage`
  significa que LoginPage tiene todas las capacidades de BasePage más las suyas propias)
- Una **variable** guarda un valor (por ejemplo, `String usuario = "admin"`)
- Una **anotación** (las que empiezan con `@`) es una instrucción especial
  para el framework (por ejemplo, `@Smoke` le dice a Cucumber que este test
  es crítico)

No necesitas saber programación avanzada. Los tests se escriben con patrones
que se repiten. Después de tu primer page object, el segundo es casi igual.

---

## Herramienta 5: Gradle — el constructor del proyecto

**¿Qué es Gradle?**
Gradle es la herramienta que organiza, compila y ejecuta el proyecto Java.
Es como el "director de obra" del framework.

Cuando ejecutas `./gradlew test`, Gradle hace en orden:
1. Descarga las librerías que el proyecto necesita (Selenium, Cucumber, etc.)
2. Compila el código Java
3. Ejecuta los tests
4. Genera los reportes

**¿Por qué existe si Java puede correr solo?**
Un proyecto real necesita docenas de librerías externas (Selenium, Allure,
AssertJ, etc.). Sin Gradle tendrías que descargarlas manualmente una por una
y configurar cada una. Gradle lo hace solo leyendo el archivo `build.gradle`.

**¿Qué es el `./gradlew`?**
Es el "Gradle Wrapper": una versión de Gradle incluida en el proyecto que
garantiza que todos usan exactamente la misma versión. Así no hay diferencias
entre "en mi máquina funciona" y "en el servidor falla".

**Analogía:** Gradle es como el chef de cocina que sabe qué ingredientes
necesita el plato (librerías), los consigue (descarga), los prepara (compila)
y sirve el resultado (ejecuta los tests).

---

## Herramienta 6: Allure — el generador de reportes

**¿Qué es Allure?**
Allure es la herramienta que convierte los resultados crudos de los tests
(texto plano) en un reporte visual en HTML con gráficas, capturas de pantalla,
severidades y tendencias.

**¿Por qué importa el reporte?**
Cuando un test falla en el pipeline de CI, el desarrollador o el líder QA
no puede ver el navegador. Solo tienen el reporte. Allure muestra:
- Qué escenario falló y en qué paso exacto
- La captura de pantalla del momento del fallo
- La URL donde estaba el navegador cuando falló
- El tiempo que tardó cada escenario
- Tendencias: ¿este test estaba fallando antes también?

---

## El mapa completo del ecosistema

Ahora que conoces cada herramienta, aquí está cómo trabajan juntas
cuando ejecutas `./gradlew test`:

```
ANTES DE CORRER
  Gradle    → descarga Selenium, Cucumber, Allure si no están
  WDManager → descarga ChromeDriver si la versión no coincide con Chrome

CORRIENDO CADA ESCENARIO
  Cucumber  → lee el archivo .feature (Gherkin)
            → busca el método Java que corresponde a cada línea
            → llama a ese método
  Java/Steps → ejecuta el método (por ej. loginPage.clickBotonIngresar())
  Java/Pages → usa Selenium para interactuar con Chrome
  Selenium  → envía la instrucción por WebDriver
  ChromeDriver → traduce WebDriver a lenguaje de Chrome
  Chrome    → ejecuta la acción real (click, escritura, etc.)
  Allure    → captura el resultado de cada paso

AL TERMINAR
  Gradle    → consolida los resultados
  Allure    → genera el reporte HTML
  JaCoCo    → genera el reporte de cobertura
  Terminal  → muestra "BUILD SUCCESSFUL" o "BUILD FAILED"
```

---

## Los 3 locators que necesitas saber para encontrar elementos

Cuando Selenium quiere hacer clic en un botón, primero necesita encontrarlo
en la página. Usa **locators** (selectores) para eso.

Hay varios tipos, pero con estos 3 resuelves el 95% de los casos:

### 1. By.id — el más confiable
```java
By.id("btnIngresar")
```
Busca un elemento por su atributo `id` en el HTML:
```html
<button id="btnIngresar">Ingresar</button>
```
**Úsalo siempre que el elemento tenga un `id` único.**

### 2. By.cssSelector — el más flexible
```java
By.cssSelector("button[type='submit']")
By.cssSelector(".btn-primary")
By.cssSelector("#formLogin .error-message")
```
CSS selector es el mismo lenguaje que usan los diseñadores web para
dar estilo a los elementos. Ejemplos:
- `button[type='submit']` → cualquier botón con type=submit
- `.btn-primary` → cualquier elemento con la clase "btn-primary"
- `#formLogin` → el elemento con id="formLogin"

**Cómo encontrar el selector:**
1. Chrome → F12 → Inspector
2. Click derecho en el elemento → "Inspect"
3. Click derecho en la línea HTML resaltada → "Copy" → "Copy selector"

### 3. By.xpath — el último recurso
```java
By.xpath("//button[contains(text(),'Ingresar')]")
```
XPath permite navegar el árbol HTML. Es poderoso pero frágil: si cambia
la estructura HTML, el XPath puede romperse.
**Úsalo solo cuando id y cssSelector no son suficientes.**

### La regla de oro de los locators
```
id           → primera opción (más estable)
cssSelector  → segunda opción (flexible y readable)
xpath        → último recurso (frágil, evitar)
```

---

## Próximo paso

Con esto entiendes qué hace cada herramienta y por qué existe.
Ahora lee `COMO_FUNCIONA_EL_FRAMEWORK.md` para ver cómo se conectan
en los tres archivos que escribirás (Feature, Steps, Page Object).

---

*Guía de herramientas v1.0 — QA Automation QA Automatización — 2026-03-15*
