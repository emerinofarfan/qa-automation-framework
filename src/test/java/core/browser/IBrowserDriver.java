package core.browser;

/**
 * Abstracción del driver de automatización de browser.
 *
 * <p>Esta interfaz desacopla completamente el framework de Selenium,
 * permitiendo migrar a Playwright, WebDriverIO, o cualquier otra herramienta
 * sin cambiar el código de Page Objects y Steps.
 *
 * <p><b>Principio de Inversión de Dependencias (DIP):</b>
 * Las clases de alto nivel (Page Objects) dependen de esta abstracción,
 * no de la implementación concreta (Selenium).
 *
 * <p><b>Estado actual:</b> Esta interfaz es un contrato de diseño preparado para
 * una futura migración a Playwright u otra herramienta. En la versión actual del
 * framework el driver se gestiona directamente a través de
 * {@link utils.DriverFactory} y {@link pages.BasePage}.
 *
 * @since 2.0.0 (diseño; implementación pendiente de migración)
 */
public interface IBrowserDriver {

    /**
     * Navega a la URL especificada.
     *
     * @param url URL completa a navegar (ej: https://app.com/login)
     */
    void navigateTo(String url);

    /**
     * Obtiene la URL actual del browser.
     *
     * @return URL actual
     */
    String getCurrentUrl();

    /**
     * Obtiene el título de la página actual.
     *
     * @return título de la página
     */
    String getTitle();

    /**
     * Encuentra el primer elemento que coincide con el locator.
     *
     * @param locator estrategia de localización
     * @return elemento encontrado
     * @throws ElementNotFoundException si no se encuentra en el timeout configurado
     */
    IElement findElement(ILocator locator);

    /**
     * Encuentra todos los elementos que coinciden con el locator.
     *
     * @param locator estrategia de localización
     * @return lista de elementos (puede estar vacía)
     */
    java.util.List<IElement> findElements(ILocator locator);

    /**
     * Verifica si un elemento existe y es visible.
     *
     * @param locator estrategia de localización
     * @param timeoutSeconds tiempo máximo de espera
     * @return true si el elemento existe y es visible
     */
    boolean isElementVisible(ILocator locator, int timeoutSeconds);

    /**
     * Espera hasta que la URL cumpla la condición especificada.
     *
     * @param condition condición a evaluar (ej: url -> url.contains("/home"))
     * @param timeoutSeconds tiempo máximo de espera
     * @return true si la condición se cumple, false si timeout
     */
    boolean waitForUrl(java.util.function.Predicate<String> condition, int timeoutSeconds);

    /**
     * Ejecuta JavaScript en el contexto de la página actual.
     *
     * @param script código JavaScript a ejecutar
     * @param args argumentos para el script
     * @return resultado de la ejecución
     */
    Object executeScript(String script, Object... args);

    /**
     * Captura screenshot de la página actual.
     *
     * @param fileName nombre del archivo (sin extensión)
     * @return ruta completa del archivo guardado
     */
    String takeScreenshot(String fileName);

    /**
     * Maximiza la ventana del browser.
     */
    void maximizeWindow();

    /**
     * Cierra el browser y libera recursos.
     */
    void quit();

    /**
     * Refresca la página actual.
     */
    void refresh();

    /**
     * Navega hacia atrás en el historial.
     */
    void navigateBack();

    /**
     * Navega hacia adelante en el historial.
     */
    void navigateForward();

    /**
     * Obtiene el handle de la ventana actual.
     *
     * @return handle de la ventana
     */
    String getWindowHandle();

    /**
     * Cambia a una ventana específica.
     *
     * @param windowHandle handle de la ventana destino
     */
    void switchToWindow(String windowHandle);

    /**
     * Cambia al frame especificado.
     *
     * @param frameLocator locator del frame
     */
    void switchToFrame(ILocator frameLocator);

    /**
     * Cambia al contenido principal (sale de frames).
     */
    void switchToDefaultContent();

    /**
     * Acepta el alert/confirm actual.
     */
    void acceptAlert();

    /**
     * Rechaza el alert/confirm actual.
     */
    void dismissAlert();

    /**
     * Obtiene el texto del alert actual.
     *
     * @return texto del alert
     */
    String getAlertText();
}

