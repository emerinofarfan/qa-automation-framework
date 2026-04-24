# Plantilla de Page Object — QA Automation QA Automatizacion

> **Como usar esta plantilla:**
> 1. Copia el bloque de codigo
> 2. Pega en `src/test/java/pages/NombrePantalla.java`
> 3. Reemplaza todo lo que esta entre `< >` con valores reales
> 4. Lee [SELENIUM_JAVA_PRACTICES.md](../02-governance/SELENIUM_JAVA_PRACTICES.md) y
>    [BASEPAGE_GUIDE.md](../02-governance/BASEPAGE_GUIDE.md) para entender cada metodo disponible
>
> **Regla de oro:** el Page Object encapsula los locators y acciones de UNA pantalla.
> Los steps no deben saber que existe `By`, `WebDriver`, ni ningun concepto de Selenium.

---

## Plantilla completa

```java
package pages;

import org.openqa.selenium.By;

/**
 * Page Object para la pantalla de <NombrePantalla>.
 *
 * Encapsula todos los locators y acciones de esta pantalla.
 * Los steps llaman metodos de esta clase; nunca usan Selenium directamente.
 */
public class <NombrePantalla>Page extends BasePage {

    // =========================================================================
    // LOCATORS — privados y estaticos (nunca exponer al exterior)
    // =========================================================================
    // Orden de preferencia para locators:
    //   1. By.id("id-unico")                → mas estable
    //   2. By.cssSelector("[data-testid='x']") → segundo mejor
    //   3. By.cssSelector(".clase-estable")  → tercera opcion
    //   4. By.xpath("//...")                 → solo como ultimo recurso

    private static final By CAMPO_<NOMBRE>   = By.id("<id-del-campo>");
    private static final By BOTON_<NOMBRE>   = By.id("<id-del-boton>");
    private static final By MENSAJE_<NOMBRE> = By.cssSelector("<selector-del-mensaje>");

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    public <NombrePantalla>Page() {
        super(); // inicializa driver, wait, fastWait desde BasePage
    }

    // =========================================================================
    // ACCIONES — metodos publicos orientados al negocio
    // =========================================================================
    // Los nombres deben ser legibles por el area de negocio.
    // Sin logica de negocio aqui: solo interacciones UI.
    // Sin assertions aqui: eso va en los Steps.

    /**
     * <Descripcion de lo que hace el metodo en lenguaje de negocio>.
     *
     * @param <parametro> <descripcion del parametro>
     */
    public void <accionDeNegocio>(String <parametro>) {
        write(CAMPO_<NOMBRE>, <parametro>);
        clickElement(BOTON_<NOMBRE>);
    }

    // =========================================================================
    // VERIFICACIONES — retornan boolean para que el step haga la assertion
    // =========================================================================
    // Nunca lanzar AssertionError desde aqui.
    // El Page Object dice "si/no". El Step decide si eso es un fallo.

    /**
     * @return true si <elemento> es visible en la pantalla
     */
    public boolean <elementoEsVisible>() {
        return isElementVisible(MENSAJE_<NOMBRE>);
    }

    /**
     * @return el texto del <elemento> para verificacion en el step
     */
    public String obtener<TextoDelElemento>() {
        return find(MENSAJE_<NOMBRE>).getText();
    }
}
```

---

## Checklist antes de hacer commit

- [ ] La clase extiende `BasePage` (no declara `WebDriver driver` propio)
- [ ] El constructor solo tiene `super()`, sin parametros
- [ ] Todos los locators son `private static final By`
- [ ] Los metodos publicos tienen nombres de negocio (no `clickBoton`, sino `confirmarPago`)
- [ ] Los metodos publicos NO usan `By.` — solo llaman metodos de BasePage
- [ ] Los metodos de verificacion retornan `boolean` o `String`, nunca lanzan `AssertionError`
- [ ] Ningun import de `org.assertj`, `org.junit`, ni assertions en este archivo
- [ ] Ningun `Thread.sleep()` — usar `waitSmall()` o waits de BasePage si es necesario

---

## Lo que NO debe ir en un Page Object

```java
// MAL — lógica de negocio en el Page Object
public boolean loginFueExitoso() {
    if (isElementVisible(PANEL_PRINCIPAL) && driver.getCurrentUrl().contains("/home")) {
        return true;
    }
    return false; // esto es una regla de negocio, no una accion UI
}

// BIEN — el Page Object reporta estado, el step evalua el negocio
public boolean panelPrincipalVisible() {
    return isElementVisible(PANEL_PRINCIPAL);
}
// En el Step: assertThat(loginPage.panelPrincipalVisible()).as("...").isTrue();
```

```java
// MAL — Selenium visible en el Page Object publico
public WebElement obtenerTablaResultados() {
    return driver.findElement(By.id("tabla"));
}
// Los steps no deben saber que existe WebElement

// BIEN — el Page Object encapsula
public boolean hayResultadosVisibles() {
    return isElementVisible(TABLA_RESULTADOS);
}
public int contarFilasEnTabla() {
    return driver.findElements(FILAS_TABLA).size();
}
```

```java
// MAL — constructor con parametros
public LoginPage(WebDriver driver) {
    this.driver = driver; // no extiende BasePage, duplica infraestructura
    this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
}

// BIEN — hereda toda la infraestructura de BasePage
public LoginPage() {
    super();
}
```

---

## Ejemplo real completo

```java
package pages;

import org.openqa.selenium.By;

/**
 * Page Object para la pantalla de busqueda de clientes.
 */
public class BusquedaClientePage extends BasePage {

    private static final By CAMPO_DNI          = By.id("txtDni");
    private static final By BOTON_BUSCAR       = By.cssSelector("button[type='submit']");
    private static final By TABLA_RESULTADOS   = By.id("tablaClientes");
    private static final By MENSAJE_SIN_RESULT = By.cssSelector(".no-results-message");
    private static final By NOMBRE_CLIENTE     = By.cssSelector(".cliente-nombre");

    public BusquedaClientePage() {
        super();
    }

    public void buscarPorDni(String dni) {
        write(CAMPO_DNI, dni);
        clickElement(BOTON_BUSCAR);
    }

    public boolean tablaResultadosVisible() {
        return isElementVisible(TABLA_RESULTADOS);
    }

    public boolean mensajeSinResultadosVisible() {
        return isElementVisible(MENSAJE_SIN_RESULT);
    }

    public String obtenerNombreDelPrimerResultado() {
        return find(NOMBRE_CLIENTE).getText();
    }
}
```

---

*Plantilla v2.0 — QA Automation QA Automatizacion — 2026-03-15*
