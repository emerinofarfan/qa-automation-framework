package core.browser;

/**
 * Abstracción de una estrategia de localización de elementos.
 *
 * <p>Desacopla los Page Objects de la sintaxis específica de Selenium (By),
 * permitiendo migrar a otras herramientas sin cambiar el código.
 *
 * <p><b>Ejemplo de uso:</b>
 * <pre>
 * // En Page Object
 * private final ILocator btnSubmit = Locators.byId("submit");
 * private final ILocator txtUsername = Locators.byName("username");
 * private final ILocator lblMessage = Locators.byXpath("//div[@class='message']");
 *
 * // Uso
 * browser.findElement(btnSubmit).click();
 * browser.findElement(txtUsername).sendKeys("NESA");
 * String message = browser.findElement(lblMessage).getText();
 * </pre>
 *
 * @since 2.0.0
 * @see Locators Utility class con factory methods
 */
public interface ILocator {

    /**
     * Obtiene la estrategia de localización.
     *
     * @return tipo de estrategia (ID, XPATH, CSS, etc.)
     */
    LocatorType getType();

    /**
     * Obtiene el valor del locator (ej: "submit", "//div[@id='main']").
     *
     * @return valor del locator
     */
    String getValue();

    /**
     * Obtiene una descripción legible del locator para logs.
     *
     * @return descripción (ej: "By ID: submit", "By XPath: //div[@class='message']")
     */
    String getDescription();

    /**
     * Tipos de estrategias de localización soportadas.
     */
    enum LocatorType {
        /** Por atributo id */
        ID,

        /** Por atributo name */
        NAME,

        /** Por atributo class */
        CLASS_NAME,

        /** Por tag name (ej: input, button) */
        TAG_NAME,

        /** Por selector CSS */
        CSS_SELECTOR,

        /** Por XPath */
        XPATH,

        /** Por texto del link */
        LINK_TEXT,

        /** Por texto parcial del link */
        PARTIAL_LINK_TEXT,

        /** Por selector personalizado (framework-specific) */
        CUSTOM
    }
}

