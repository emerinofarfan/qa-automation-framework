package core.browser;

/**
 * Factory class para crear locators de forma fluida.
 *
 * <p>Proporciona métodos estáticos para crear instancias de {@link ILocator}
 * sin exponer la implementación concreta.
 *
 * <p><b>Ejemplo de uso:</b>
 * <pre>
 * // En Page Objects
 * private final ILocator btnSubmit = Locators.byId("submit");
 * private final ILocator txtUsername = Locators.byName("username");
 * private final ILocator lblError = Locators.byClass("error-message");
 * private final ILocator divMain = Locators.byXpath("//div[@id='main']");
 * private final ILocator inputs = Locators.byCss("input[type='text']");
 *
 * // Uso
 * browser.findElement(btnSubmit).click();
 * browser.findElement(txtUsername).sendKeys("user");
 * </pre>
 *
 * @since 2.0.0
 */
public final class Locators {

    private Locators() {
        // Utility class - no instanciable
    }

    /**
     * Crea un locator por atributo ID.
     *
     * <p><b>Ejemplo:</b> {@code Locators.byId("submit")}
     * equivale a {@code <button id="submit">}
     *
     * @param id valor del atributo id
     * @return locator por ID
     */
    public static ILocator byId(String id) {
        return new SimpleLocator(ILocator.LocatorType.ID, id);
    }

    /**
     * Crea un locator por atributo name.
     *
     * <p><b>Ejemplo:</b> {@code Locators.byName("username")}
     * equivale a {@code <input name="username">}
     *
     * @param name valor del atributo name
     * @return locator por name
     */
    public static ILocator byName(String name) {
        return new SimpleLocator(ILocator.LocatorType.NAME, name);
    }

    /**
     * Crea un locator por atributo class.
     *
     * <p><b>Ejemplo:</b> {@code Locators.byClass("error-message")}
     * equivale a {@code <div class="error-message">}
     *
     * @param className valor del atributo class
     * @return locator por class
     */
    public static ILocator byClass(String className) {
        return new SimpleLocator(ILocator.LocatorType.CLASS_NAME, className);
    }

    /**
     * Crea un locator por tag name.
     *
     * <p><b>Ejemplo:</b> {@code Locators.byTag("button")}
     * encuentra todos los elementos {@code <button>}
     *
     * @param tagName nombre del tag HTML
     * @return locator por tag name
     */
    public static ILocator byTag(String tagName) {
        return new SimpleLocator(ILocator.LocatorType.TAG_NAME, tagName);
    }

    /**
     * Crea un locator por selector CSS.
     *
     * <p><b>Ejemplo:</b> {@code Locators.byCss("input[type='text']")}
     *
     * @param cssSelector selector CSS válido
     * @return locator por CSS selector
     */
    public static ILocator byCss(String cssSelector) {
        return new SimpleLocator(ILocator.LocatorType.CSS_SELECTOR, cssSelector);
    }

    /**
     * Crea un locator por XPath.
     *
     * <p><b>Ejemplo:</b> {@code Locators.byXpath("//div[@id='main']//button[1]")}
     *
     * @param xpath expresión XPath válida
     * @return locator por XPath
     */
    public static ILocator byXpath(String xpath) {
        return new SimpleLocator(ILocator.LocatorType.XPATH, xpath);
    }

    /**
     * Crea un locator por texto exacto del link.
     *
     * <p><b>Ejemplo:</b> {@code Locators.byLinkText("Iniciar Sesión")}
     * encuentra {@code <a href="#">Iniciar Sesión</a>}
     *
     * @param linkText texto exacto del link
     * @return locator por link text
     */
    public static ILocator byLinkText(String linkText) {
        return new SimpleLocator(ILocator.LocatorType.LINK_TEXT, linkText);
    }

    /**
     * Crea un locator por texto parcial del link.
     *
     * <p><b>Ejemplo:</b> {@code Locators.byPartialLinkText("Iniciar")}
     * encuentra {@code <a href="#">Iniciar Sesión</a>}
     *
     * @param partialLinkText texto parcial del link
     * @return locator por partial link text
     */
    public static ILocator byPartialLinkText(String partialLinkText) {
        return new SimpleLocator(ILocator.LocatorType.PARTIAL_LINK_TEXT, partialLinkText);
    }

    /**
     * Implementación simple de ILocator.
     */
    private static class SimpleLocator implements ILocator {
        private final LocatorType type;
        private final String value;

        public SimpleLocator(LocatorType type, String value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public LocatorType getType() {
            return type;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String getDescription() {
            return "By " + type + ": " + value;
        }

        @Override
        public String toString() {
            return getDescription();
        }
    }
}

