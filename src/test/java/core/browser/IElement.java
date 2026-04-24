package core.browser;

/**
 * Abstracción de un elemento web.
 *
 * <p>Encapsula las operaciones que se pueden realizar sobre un elemento,
 * independientemente de la herramienta de automatización subyacente.
 *
 * <p><b>Principio de Inversión de Dependencias:</b>
 * Los Page Objects interactúan con esta abstracción, no con WebElement de Selenium.
 *
 * <p><b>Ejemplo de uso:</b>
 * <pre>
 * IElement button = browser.findElement(Locators.byId("submit"));
 *
 * if (button.isDisplayed() && button.isEnabled()) {
 *     button.click();
 * }
 *
 * IElement input = browser.findElement(Locators.byName("username"));
 * input.clear();
 * input.sendKeys("NESA");
 *
 * String text = browser.findElement(Locators.byClass("message")).getText();
 * </pre>
 *
 * @since 2.0.0
 * @see SeleniumElement Implementación con Selenium WebElement
 */
public interface IElement {

    /**
     * Hace click en el elemento.
     *
     * <p>Espera automáticamente hasta que el elemento sea clickeable.
     */
    void click();

    /**
     * Envía texto al elemento (típicamente un input).
     *
     * @param text texto a enviar
     */
    void sendKeys(String text);

    /**
     * Limpia el contenido del elemento (típicamente un input).
     */
    void clear();

    /**
     * Obtiene el texto visible del elemento.
     *
     * @return texto del elemento
     */
    String getText();

    /**
     * Obtiene el valor de un atributo del elemento.
     *
     * @param attributeName nombre del atributo (ej: "value", "class", "href")
     * @return valor del atributo, o null si no existe
     */
    String getAttribute(String attributeName);

    /**
     * Obtiene el valor de una propiedad CSS del elemento.
     *
     * @param propertyName nombre de la propiedad CSS (ej: "color", "display")
     * @return valor de la propiedad
     */
    String getCssValue(String propertyName);

    /**
     * Verifica si el elemento está visible en la página.
     *
     * @return true si está visible, false en caso contrario
     */
    boolean isDisplayed();

    /**
     * Verifica si el elemento está habilitado (no disabled).
     *
     * @return true si está habilitado, false en caso contrario
     */
    boolean isEnabled();

    /**
     * Verifica si el elemento está seleccionado (checkbox/radio).
     *
     * @return true si está seleccionado, false en caso contrario
     */
    boolean isSelected();

    /**
     * Obtiene el tag name del elemento (ej: "input", "button", "div").
     *
     * @return tag name en minúsculas
     */
    String getTagName();

    /**
     * Envía una tecla especial al elemento (ENTER, TAB, ESC, etc).
     *
     * @param key tecla especial a enviar
     */
    void sendKey(SpecialKey key);

    /**
     * Hace scroll hasta que el elemento sea visible.
     */
    void scrollIntoView();

    /**
     * Hace hover sobre el elemento (mouse over).
     */
    void hover();

    /**
     * Hace doble click en el elemento.
     */
    void doubleClick();

    /**
     * Hace click derecho en el elemento.
     */
    void rightClick();

    /**
     * Obtiene las dimensiones del elemento (ancho, alto, posición).
     *
     * @return dimensiones del elemento
     */
    ElementDimensions getDimensions();

    /**
     * Espera hasta que el elemento cumpla una condición.
     *
     * @param condition condición a evaluar
     * @param timeoutSeconds tiempo máximo de espera
     * @return true si la condición se cumple, false si timeout
     */
    boolean waitUntil(java.util.function.Predicate<IElement> condition, int timeoutSeconds);

    /**
     * Encuentra un elemento hijo dentro de este elemento.
     *
     * @param locator estrategia de localización relativa
     * @return elemento hijo encontrado
     */
    IElement findElement(ILocator locator);

    /**
     * Encuentra todos los elementos hijos dentro de este elemento.
     *
     * @param locator estrategia de localización relativa
     * @return lista de elementos hijos
     */
    java.util.List<IElement> findElements(ILocator locator);

    /**
     * Teclas especiales que se pueden enviar a un elemento.
     */
    enum SpecialKey {
        ENTER,
        TAB,
        ESCAPE,
        BACKSPACE,
        DELETE,
        ARROW_UP,
        ARROW_DOWN,
        ARROW_LEFT,
        ARROW_RIGHT,
        HOME,
        END,
        PAGE_UP,
        PAGE_DOWN,
        F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12
    }

    /**
     * Dimensiones de un elemento (ancho, alto, posición X, posición Y).
     */
    interface ElementDimensions {
        int getWidth();
        int getHeight();
        int getX();
        int getY();
    }
}

