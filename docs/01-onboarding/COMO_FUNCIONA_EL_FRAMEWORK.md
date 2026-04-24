# Cómo funciona el framework — Guía para QA sin experiencia en automatización

> Lee esto antes de tocar cualquier archivo de código.
> Si entiendes esta página, entiendes el 80% del framework.

---

## La pregunta más importante: ¿qué hace la automatización?

Simula exactamente lo que haría un tester humano frente al navegador:

```
Humano:  Abre Chrome → Va a la URL → Escribe usuario → Escribe clave → Click en "Ingresar" → Ve si entró
Código:  driver.get(url) → write(campo, valor) → clickElement(boton) → assertThat(panel).isTrue()
```

La diferencia: el código lo hace en 5 segundos, sin cansarse, las 24 horas del día.

---

## Las 3 piezas que debes conocer

Todo test automatizado en este framework tiene exactamente 3 piezas:

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│   FEATURE (.feature)        STEPS (.java)        PAGE (.java)       │
│                                                                     │
│   "Qué debe pasar"    →    "Cómo ejecutarlo"  →  "Dónde está en     │
│   (lenguaje negocio)       (código Java)          la pantalla"      │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Pieza 1: El Feature — "qué debe pasar"

**Archivo:** `src/test/resources/features/`
**Extensión:** `.feature`
**Lenguaje:** Gherkin (lenguaje natural, en español)

El feature describe el comportamiento que quieres validar.
Lo escribe el QA junto con el negocio. No es código Java.

```gherkin
@Smoke
Feature: Autenticación de usuario

  Scenario: Ingreso exitoso con credenciales válidas
    Given el usuario navega al portal de la aplicación
    When ingresa usuario y clave válidos del ambiente de prueba
    And hace clic en el botón de ingresar
    Then debe visualizarse el panel principal del sistema
```

**Regla de oro:** Si alguien del negocio no puede leerlo y entenderlo, está mal escrito.

---

## Pieza 2: El Step — "cómo ejecutarlo"

**Archivo:** `src/test/java/steps/`
**Extensión:** `.java`
**Lenguaje:** Java

El step conecta cada línea del feature con código Java.
Cada `Given`, `When`, `Then` del feature tiene su método correspondiente aquí.

```java
// Esta anotación debe coincidir EXACTAMENTE con el texto del feature
@Cuando("hace clic en el botón de ingresar")
public void haceClicEnBotonIngresar() {
    loginPage.presionarBotonIngresar();   // Llama al Page Object
}
```

**Regla de oro:** El step solo coordina. No habla con Selenium directamente.
Si necesitas `By.id(...)` en un step, algo está mal.

---

## Pieza 3: El Page Object — "dónde está en la pantalla"

**Archivo:** `src/test/java/pages/`
**Extensión:** `.java`
**Lenguaje:** Java (hereda de `BasePage`)

El Page Object sabe dos cosas:
1. **Dónde están los elementos** (los locators: IDs, CSS, XPath)
2. **Cómo interactuar con ellos** (click, escribir, verificar)

```java
public class LoginPage extends BasePage {

    // DÓNDE está el botón en la pantalla
    private static final By BOTON_INGRESAR = By.cssSelector("button[type='submit']");

    // CÓMO interactuar con él
    public void presionarBotonIngresar() {
        clickElement(BOTON_INGRESAR);  // BasePage hace el click con reintentos
    }
}
```

**Regla de oro:** Si el ID del botón cambia en el sistema, solo cambias el locator
aquí. El feature y el step no necesitan modificarse.

---

## El flujo completo (de arriba hacia abajo)

```
Demo.feature
    │
    │  Scenario: Ingreso exitoso...
    │    Given el usuario navega al portal...
    │    When ingresa usuario y clave válidos...
    │    And hace clic en el botón de ingresar
    │    Then debe visualizarse el panel principal
    │
    ▼
DemoLoginSteps.java
    │
    │  @Dado("el usuario navega al portal...")
    │  → loginPage.navigateTo(ConfigManager.getBaseUrl())
    │
    │  @Cuando("ingresa usuario y clave válidos...")
    │  → loginPage.ingresarUsuario(...)
    │  → loginPage.ingresarClave(...)
    │
    │  @Cuando("hace clic en el botón de ingresar")
    │  → loginPage.presionarBotonIngresar()
    │
    │  @Entonces("debe visualizarse el panel principal")
    │  → assertThat(loginPage.panelPrincipalVisible()).isTrue()
    │
    ▼
LoginPage.java
    │
    │  navigateTo(url)           → driver.get(url)
    │  ingresarUsuario(usuario)  → write(CAMPO_USUARIO, usuario)
    │  ingresarClave(clave)      → write(CAMPO_CLAVE, clave)
    │  presionarBotonIngresar()      → clickElement(BOTON_INGRESAR)
    │  panelPrincipalVisible()   → isElementVisible(PANEL_PRINCIPAL)
    │
    ▼
BasePage.java (no lo tocas, ya está hecho)
    │
    │  write(locator, valor)     → findVisible(locator).sendKeys(valor)
    │  clickElement(locator)     → espera + scroll + click + 3 reintentos
    │  isElementVisible(locator) → true/false sin lanzar excepción
    │
    ▼
Selenium WebDriver (no lo tocas, ya está configurado)
    │
    └─ Controla Chrome realmente: abre páginas, clicks, inputs, screenshots
```

---

## Los tags: para qué sirven

Los tags (`@Smoke`, `@Regression`, etc.) controlan **cuándo corre cada test**:

| Tag | Cuándo corre | Para qué |
|---|---|---|
| `@Smoke` | En cada Merge Request | Valida que lo básico funciona |
| `@Regression` | En staging y producción | Valida toda la suite completa |
| `@Negativo` | Con `@Regression` | Valida que los errores se manejan bien |
| `@Destructive` | Al final, sin paralelo | Tests que crean o borran datos |
| `@UIValidation` | Con `@Regression` | Solo valida la interfaz visual |

**En el pipeline de Joel:** cuando alguien hace un Merge Request, solo corren los `@Smoke`.
Si pasan, el MR puede mergearse. Si fallan, se bloquea.

---

## Lo que NO necesitas entender al inicio

- Cómo funciona `ThreadLocal` (es para ejecución paralela, ya está hecho)
- Qué hace `@Before` y `@After` en Hooks.java (ya está hecho, captura screenshots)
- Cómo configura Chrome el `DriverFactory` (ya está hecho)
- Qué hace JaCoCo o Allure internamente (generan reportes solos)

Empieza creando features, steps y pages. El resto ya está construido.

---

## Los 3 archivos que crearás para cada funcionalidad

```
src/test/resources/features/    NombreFuncionalidad.feature   ← tú escribes
src/test/java/steps/            NombreFuncionalidadSteps.java ← tú escribes
src/test/java/pages/            NombreFuncionalidadPage.java  ← tú escribes
```

Los templates están en `docs/03-templates/`. Cópialos y adáptalos.

---

## Próximo paso

Lee `docs/01-onboarding/DIA1_CHECKLIST.md` para saber exactamente
qué hacer en tus primeras 2 horas.
