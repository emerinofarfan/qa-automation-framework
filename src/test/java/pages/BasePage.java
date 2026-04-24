package pages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.AutomationException;
import utils.DriverFactory;

import static utils.TimeoutConstants.*;

/**
 * Clase base de todos los Page Objects del framework.
 *
 * <p>Proporciona la infraestructura compartida de interacción con Selenium:
 * driver, waits, clicks con reintentos, helpers de scroll/JS, dropdowns
 * y warmup de certificados SSL para entornos de prueba con certs no confiables.
 *
 * <h3>Thread-safety (ejecución paralela)</h3>
 * <p>El driver se obtiene de {@link DriverFactory#getDriver()}, que usa
 * {@code ThreadLocal<WebDriver>} internamente. Cada hilo del pool de Cucumber
 * tiene su propio driver; los page objects no comparten estado del driver entre hilos.
 *
 * <h3>Estrategia de esperas</h3>
 * <ul>
 *   <li>{@link #wait} (20 s, polling 500 ms): espera estándar para operaciones de UI.</li>
 *   <li>{@code clickWait} (15 s): espera dedicada a clicks con 3 reintentos contra
 *       {@link StaleElementReferenceException}.</li>
 *   <li>Waits locales con {@code pollingEvery(100 ms)} creados en page objects
 *       individuales para condiciones que se cumplen en &lt;1 s (reduce hasta
 *       400 ms de latencia innecesaria por condición).</li>
 * </ul>
 *
 * <h3>Herencia</h3>
 * <p>Todos los page objects extienden esta clase y llaman a {@code super()}
 * en su constructor. Los subtipos pueden crear waits adicionales encima de la base.
 */
@SuppressWarnings({"null"})
public class BasePage {

    /** Logger for structured logging of page interactions */
    private static final Logger log = LoggerFactory.getLogger(BasePage.class);

    /**
     * Driver del hilo actual; obtenido de {@link DriverFactory} (ThreadLocal).
     * Accesible desde subclases mediante herencia protegida.
     */
    protected WebDriver driver;

    /** Wait estándar de 20 s con polling por defecto de 500 ms */
    protected WebDriverWait wait;

    /** Instancia reutilizable para clicks */
    private WebDriverWait clickWait;

    /**
     * Wait rápido optimizado para condiciones inmediatas (&lt;1s).
     * Polling de 100ms en lugar de 500ms → ahorra hasta 400ms por condición.
     * Ideal para modales, botones que cambian de estado, elementos de Angular.
     */
    protected FluentWait<WebDriver> fastWait;

    /**
     * Inicializa el driver, el wait estándar (20 s) y el wait de click (15 s)
     * para el hilo actual del pool de Cucumber.
     *
     * <p>Es llamado implícitamente desde el constructor de cada page object vía
     * {@code super()}. El driver ya debe haber sido inicializado por
     * {@link utils.DriverFactory#initDriver()} en el {@code @Before} de
     * {@link hooks.Hooks}; de lo contrario, {@code driver} será {@code null}
     * y las operaciones subsiguientes lanzarán {@link NullPointerException}.
     */
    public BasePage() {
        this.driver    = DriverFactory.getDriver();
        this.wait      = new WebDriverWait(driver, STANDARD);
        this.clickWait = new WebDriverWait(driver, CLICK);
        this.fastWait  = new WebDriverWait(driver, QUICK)
                            .pollingEvery(POLLING_FAST);
    }

    // =====================================================================
    // NAVEGACIÓN
    // =====================================================================

    /**
     * Navega el driver a la URL indicada.
     *
     * <p>Equivalente a escribir la URL en la barra del navegador: espera a que
     * el browser complete la carga inicial de la página antes de retornar
     * (comportamiento estándar de {@link WebDriver#get(String)}).
     * Angular puede continuar renderizando componentes después; usar
     * {@link #wait} para esperar elementos específicos tras la navegación.
     *
     * @param url URL completa a la que navegar (ej. {@code "https://host/app/auth"})
     */
    public void navigateTo(String url) {
        driver.get(url);
    }

    // =====================================================================
    // LOCALIZACIÓN DE ELEMENTOS
    // =====================================================================

    // =====================================================================
    // LECTURA DE DATOS DE ELEMENTOS
    // =====================================================================

    /**
     * Obtiene el texto visible de un elemento, esperando que sea <b>no vacío</b>.
     *
     * <p><b>Anti-flaky:</b> en Angular, el elemento puede existir en el DOM antes de
     * que el framework haya terminado de renderizar su contenido. Un {@code find().getText()}
     * inmediato retornaría cadena vacía {@code ""} aunque el texto llegue milisegundos después.
     * Este método espera hasta que {@code getText()} retorne un valor no vacío antes de
     * retornar al Page Object.
     *
     * <p>Si después de {@link utils.TimeoutConstants#STANDARD} segundos el texto sigue vacío,
     * retorna la cadena vacía en lugar de lanzar excepción — permite al step decidir
     * si ese es un estado válido o un fallo.
     *
     * @param locator estrategia de localización del elemento
     * @return texto visible del elemento; cadena vacía si nunca se pobló
     * @throws org.openqa.selenium.TimeoutException si el elemento no aparece en el DOM
     */
    protected String getText(By locator) {
        find(locator); // espera presencia en DOM
        try {
            wait.until(d -> {
                String t = d.findElement(locator).getText();
                return (t != null && !t.trim().isEmpty()) ? t : null;
            });
        } catch (TimeoutException ignored) {
            // El elemento existe pero su texto sigue vacío — lo retornamos tal cual
        }
        return find(locator).getText();
    }

    /**
     * Obtiene el valor de un atributo HTML de un elemento.
     *
     * <p>Útil para leer:
     * <ul>
     *   <li>{@code value} de inputs para verificar el contenido de un campo</li>
     *   <li>{@code class} para verificar estado activo/seleccionado</li>
     *   <li>{@code aria-expanded}, {@code aria-selected} en componentes Angular Material</li>
     *   <li>{@code data-testid}, {@code data-*} atributos personalizados</li>
     *   <li>{@code href}, {@code src}, {@code disabled} en elementos HTML</li>
     * </ul>
     *
     * @param locator   estrategia de localización del elemento
     * @param attribute nombre del atributo HTML a leer (ej. {@code "value"}, {@code "class"})
     * @return valor del atributo; {@code null} si el atributo no existe en el elemento
     * @throws org.openqa.selenium.TimeoutException si el elemento no aparece en el DOM
     */
    protected String getAttribute(By locator, String attribute) {
        return find(locator).getAttribute(attribute);
    }

    /**
     * Retorna la URL actual del navegador.
     *
     * <p>Útil en los Steps para assertions sobre navegación:
     * {@code assertThat(page.getCurrentUrl()).contains("/panel/operaciones")}.
     * También útil para diagnosticar fallas: la URL actual en el momento del error
     * indica en qué pantalla estaba el test cuando falló.
     *
     * @return URL completa actual de la barra de direcciones del navegador
     */
    protected String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    // =====================================================================
    // LOCALIZACIÓN MÚLTIPLE
    // =====================================================================

    /**
     * Localiza todos los elementos que coinciden con el locator, esperando
     * que al menos uno sea <b>visible</b>.
     *
     * <p>Esencial para interactuar con tablas, listas y colecciones de elementos.
     * Espera que al menos una coincidencia sea visible antes de retornar la lista completa.
     *
     * <p><b>Casos de uso típicos en banca:</b>
     * <pre>
     *   // Contar filas de una tabla
     *   int filas = findAll(By.cssSelector("table tbody tr")).size();
     *
     *   // Leer el texto de cada fila
     *   for (WebElement fila : findAll(FILAS_TABLA)) {
     *       String texto = fila.getText();
     *   }
     * </pre>
     *
     * @param locator estrategia de localización de los elementos
     * @return lista de {@link WebElement} visibles encontrados; nunca {@code null}
     * @throws org.openqa.selenium.TimeoutException si no hay elementos visibles
     *         dentro de {@link utils.TimeoutConstants#STANDARD}
     */
    protected List<WebElement> findAll(By locator) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        return driver.findElements(locator);
    }

    /**
     * Cuenta los elementos que coinciden con el locator en el DOM actual.
     *
     * <p>Retorna inmediatamente sin espera — equivalente a
     * {@code driver.findElements(locator).size()}. Útil para:
     * <ul>
     *   <li>Verificar cuántas filas tiene una tabla</li>
     *   <li>Confirmar que una lista tiene N elementos</li>
     *   <li>Detectar si hay elementos opcionales presentes</li>
     * </ul>
     *
     * <p><b>Atención:</b> si los elementos aún no han cargado, puede retornar 0.
     * Usar {@link #findAll(By)} si se necesita esperar que aparezcan primero.
     *
     * @param locator estrategia de localización de los elementos
     * @return número de elementos encontrados en el DOM (0 si no hay ninguno)
     */
    protected int countElements(By locator) {
        return driver.findElements(locator).size();
    }

    /**
     * Localiza un elemento esperando su <b>presencia en el DOM</b>
     * (no necesariamente visible ni interactuable).
     *
     * <p>Usa {@link ExpectedConditions#presenceOfElementLocated}, que retorna en cuanto
     * el elemento existe en el DOM aunque esté oculto ({@code display:none},
     * {@code visibility:hidden} o fuera del viewport). Útil para leer atributos
     * o texto de elementos que Angular inserta antes de mostrarlos.
     *
     * <p><b>Cuándo usar {@code find} vs {@code findVisible}:</b>
     * <ul>
     *   <li>{@code find}: solo para leer atributos/texto de elementos que pueden
     *       estar ocultos (ej. leer {@code value} de un input hidden).</li>
     *   <li>{@code findVisible}: cuando vas a interactuar con el elemento
     *       (click, sendKeys) — garantiza que el usuario lo vería.</li>
     * </ul>
     *
     * @param locator estrategia de localización del elemento
     * @return el {@link WebElement} encontrado
     * @throws org.openqa.selenium.TimeoutException si no aparece en el DOM
     *         dentro de {@link utils.TimeoutConstants#STANDARD}
     */
    protected WebElement find(By locator) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    /**
     * Localiza un elemento esperando que sea <b>visible</b>:
     * presente en el DOM, con dimensiones &gt; 0 y sin ocultamiento CSS.
     *
     * <p>Usa {@link ExpectedConditions#visibilityOfElementLocated}, que verifica
     * presencia en DOM + {@code display} distinto de {@code none} +
     * {@code visibility} distinto de {@code hidden} + tamaño &gt; 0.
     * Necesario antes de cualquier interacción con el usuario (click, sendKeys).
     *
     * @param locator estrategia de localización del elemento
     * @return el {@link WebElement} visible
     * @throws org.openqa.selenium.TimeoutException si no es visible dentro
     *         de {@link utils.TimeoutConstants#STANDARD}
     */
    protected WebElement findVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    // =====================================================================
    // INTERACCIÓN CON ELEMENTOS
    // =====================================================================

    /**
     * Hace click en el primer elemento clickeable encontrado por {@code locator}.
     *
     * <p>Optimizado para Angular:
     * <ul>
     *   <li>Una sola instancia de {@code clickWait} (15 s) reutilizada entre intentos.</li>
     *   <li>{@code elementToBeClickable} garantiza presencia + visibilidad + habilitado
     *       en una sola condición (sin doble espera redundante).</li>
     *   <li>Scroll al centro antes del click evita interceptaciones por headers fijos.</li>
     *   <li>Tolerancia a {@link StaleElementReferenceException}: Angular re-renderiza
     *       el DOM en re-renders; hasta 3 reintentos con espera de refresh entre ellos.</li>
     *   <li>Tolerancia a {@link ElementClickInterceptedException}: si un spinner u overlay
     *       cubre el elemento (patrón común en Angular), espera y reintenta; en el último
     *       intento usa {@link #jsClick(WebElement)} como fallback automático.</li>
     * </ul>
     *
     * @param locator estrategia de localización del elemento a clickear
     * @throws AutomationException si el click falla tras 3 intentos
     */
    public void clickElement(By locator) {
        log.debug("Attempting to click element: {}", locator);

        for (int attempt = 0; attempt < MAX_CLICK_RETRIES; attempt++) {
            try {
                WebElement el = clickWait.until(ExpectedConditions.elementToBeClickable(locator));
                scrollIntoViewCenter(el);
                el.click();

                log.debug("Successfully clicked element: {} on attempt {}", locator, attempt + 1);
                return;

            } catch (StaleElementReferenceException e) {
                log.warn("Stale element on attempt {}/{} for: {}",
                    attempt + 1, MAX_CLICK_RETRIES, locator);

                if (attempt == MAX_CLICK_RETRIES - 1) {
                    log.error("Failed to click after {} attempts: {}", MAX_CLICK_RETRIES, locator, e);
                    throw AutomationException.clickFailed(locator.toString(), locator, e);
                }

                // Wait for Angular to complete DOM refresh before next attempt
                clickWait.until(ExpectedConditions.refreshed(
                        ExpectedConditions.presenceOfElementLocated(locator)));

            } catch (ElementClickInterceptedException e) {
                // Spinner, overlay o modal cubre el elemento momentáneamente.
                // Angular activa overlays durante operaciones asíncronas (~100-300ms).
                log.warn("Click interceptado en intento {}/{} para: {} — overlay presente",
                    attempt + 1, MAX_CLICK_RETRIES, locator);

                if (attempt == MAX_CLICK_RETRIES - 1) {
                    // Último intento: jsClick como fallback ante overlay persistente.
                    // JS click llega directo al DOM ignorando overlays visuales.
                    log.warn("Usando jsClick como fallback ante overlay persistente: {}", locator);
                    try {
                        jsClick(driver.findElement(locator));
                        log.debug("jsClick exitoso como fallback para: {}", locator);
                        return;
                    } catch (Exception ex) {
                        throw AutomationException.clickFailed(locator.toString(), locator, e);
                    }
                }
                // Espera que el overlay desaparezca antes del siguiente intento
                waitSmall();

            } catch (TimeoutException e) {
                log.error("Timeout waiting for clickable element: {}", locator, e);
                throw AutomationException.elementNotFound("clickElement", locator.toString(), locator, e);
            }
        }

        // Should never reach here due to throw in loop, but safety fallback
        log.error("Unexpected: exceeded retry loop without throwing exception for: {}", locator);
        throw AutomationException.clickFailed(locator.toString(), locator, null);
    }

    /**
     * Hace scroll para centrar el elemento en el viewport usando su locator.
     *
     * <p>Versión por locator de {@link #scrollIntoViewCenter(WebElement)}.
     * Útil cuando se necesita hacer scroll antes de una aserción visual
     * sin tener una referencia al {@link WebElement} previa.
     *
     * @param locator estrategia de localización del elemento a centrar
     */
    protected void scrollToElement(By locator) {
        scrollIntoViewCenter(find(locator));
    }

    /**
     * Escribe texto en un campo de entrada de forma segura para Angular.
     *
     * <p><b>Por qué NO usar {@code element.clear()}:</b> el método nativo de Selenium
     * {@code clear()} no dispara el evento {@code input} que Angular escucha para
     * actualizar su modelo reactivo ({@code ngModel}, {@code FormControl}). Como
     * resultado, el DOM muestra el campo vacío pero el modelo de Angular conserva
     * el valor anterior, causando que las validaciones fallen silenciosamente.
     *
     * <p><b>Solución anti-flaky:</b>
     * <ol>
     *   <li>{@code click()} → pone foco en el campo y dispara el evento {@code focus}</li>
     *   <li>{@code CTRL+A} → selecciona todo el contenido actual sin disparar {@code input}</li>
     *   <li>{@code sendKeys(value)} → reemplaza la selección Y dispara {@code input},
     *       por lo que Angular actualiza su modelo correctamente</li>
     * </ol>
     *
     * <p>Adicionalmente incluye reintentos ante {@link StaleElementReferenceException}
     * para tolerancia a re-renders de Angular entre la localización y la escritura.
     *
     * @param locator estrategia de localización del campo de texto
     * @param value   texto a ingresar
     * @throws AutomationException si el campo no es accesible tras los reintentos
     */
    protected void write(By locator, String value) {
        for (int attempt = 0; attempt < MAX_CLICK_RETRIES; attempt++) {
            try {
                WebElement el = findVisible(locator);
                el.click();                                          // foco + evento focus
                // input.select() vía JS: selecciona todo el texto sin eventos de teclado.
                // Más confiable que Keys.chord y Actions API en Chrome 120+ headless.
                executeJS("arguments[0].select();", el);
                el.sendKeys(value);                                  // reemplaza + dispara input
                log.debug("Wrote into field: {}", locator);
                return;
            } catch (StaleElementReferenceException e) {
                log.warn("Stale element on write attempt {}/{}: {}", attempt + 1, MAX_CLICK_RETRIES, locator);
                if (attempt == MAX_CLICK_RETRIES - 1) {
                    throw AutomationException.clickFailed(locator.toString(), locator, e);
                }
            }
        }
    }

    /**
     * Limpia el contenido de un campo de texto de forma segura para Angular.
     *
     * <p>Usa la misma estrategia anti-flaky que {@link #write}: {@code CTRL+A}
     * seguido de {@code DELETE}. Esto dispara el evento {@code input} que Angular
     * necesita para actualizar su modelo a vacío, a diferencia de {@code element.clear()}
     * que deja el modelo interno de Angular con el valor anterior.
     *
     * @param locator estrategia de localización del campo a limpiar
     */
    protected void clearField(By locator) {
        for (int attempt = 0; attempt < MAX_CLICK_RETRIES; attempt++) {
            try {
                WebElement el = findVisible(locator);
                el.click();
                executeJS("arguments[0].select();", el);
                el.sendKeys(Keys.DELETE);
                return;
            } catch (StaleElementReferenceException e) {
                log.warn("Stale element en clearField intento {}/{}: {}", attempt + 1, MAX_CLICK_RETRIES, locator);
                if (attempt == MAX_CLICK_RETRIES - 1) {
                    throw AutomationException.clickFailed(locator.toString(), locator, e);
                }
            }
        }
    }

    /**
     * Escribe texto en un campo y presiona la tecla Enter.
     *
     * <p>Patrón común en campos de búsqueda, filtros y autocompletados
     * que disparan la búsqueda al presionar Enter en lugar de requerir
     * un click en un botón separado.
     *
     * <p><b>Anti-flaky:</b> reutiliza la misma referencia al {@link WebElement}
     * para la escritura y el Enter — evita la {@link StaleElementReferenceException}
     * que ocurría al llamar {@code findVisible()} dos veces separadas.
     *
     * @param locator estrategia de localización del campo
     * @param value   texto a ingresar antes de presionar Enter
     */
    protected void writeAndPressEnter(By locator, String value) {
        for (int attempt = 0; attempt < MAX_CLICK_RETRIES; attempt++) {
            try {
                WebElement el = findVisible(locator);
                el.click();
                executeJS("arguments[0].select();", el);
                el.sendKeys(value);
                el.sendKeys(Keys.ENTER);
                return;
            } catch (StaleElementReferenceException e) {
                log.warn("Stale on writeAndPressEnter attempt {}/{}: {}", attempt + 1, MAX_CLICK_RETRIES, locator);
                if (attempt == MAX_CLICK_RETRIES - 1) {
                    throw AutomationException.clickFailed(locator.toString(), locator, e);
                }
            }
        }
    }

    /**
     * Envía una tecla especial a un elemento (Enter, Tab, Escape, F2, etc.).
     *
     * <p>Esencial para formularios Angular que responden a eventos de teclado:
     * <ul>
     *   <li>{@code Keys.TAB} — mover al siguiente campo y disparar validación</li>
     *   <li>{@code Keys.ENTER} — confirmar selección en autocomplete</li>
     *   <li>{@code Keys.ESCAPE} — cancelar modal o dropdown abierto</li>
     *   <li>{@code Keys.F2} — activar modo edición en grids</li>
     * </ul>
     *
     * @param locator estrategia de localización del elemento receptor
     * @param key     tecla a presionar (constante de {@link org.openqa.selenium.Keys})
     */
    protected void pressKey(By locator, org.openqa.selenium.Keys key) {
        findVisible(locator).sendKeys(key);
    }

    /**
     * Realiza hover (pasar el mouse) sobre un elemento.
     *
     * <p>Necesario para menús Angular Material ({@code mat-menu}) y otros
     * componentes que se despliegan con el evento {@code mouseenter}.
     * Sin hover, el menú nunca aparece y los subítems no son interactuables.
     *
     * <p><b>Ejemplo de uso:</b>
     * <pre>
     *   hoverOver(MENU_OPERACIONES);          // abre el menú
     *   clickElement(SUBMENU_TRANSFERENCIAS); // clic en el subítem
     * </pre>
     *
     * @param locator estrategia de localización del elemento sobre el que hacer hover
     */
    protected void hoverOver(By locator) {
        for (int attempt = 0; attempt < MAX_CLICK_RETRIES; attempt++) {
            try {
                new org.openqa.selenium.interactions.Actions(driver)
                        .moveToElement(findVisible(locator))
                        .perform();
                return;
            } catch (StaleElementReferenceException e) {
                log.warn("Stale element en hoverOver intento {}/{}: {}", attempt + 1, MAX_CLICK_RETRIES, locator);
                if (attempt == MAX_CLICK_RETRIES - 1) {
                    throw AutomationException.elementNotFound("hoverOver", locator.toString(), locator, e);
                }
            }
        }
    }

    // =====================================================================
    // VERIFICACIÓN DE PRESENCIA Y VISIBILIDAD
    // =====================================================================

    /**
     * Comprueba si un elemento está <b>habilitado</b> (no deshabilitado).
     *
     * <p>En banca, los botones de confirmación frecuentemente permanecen
     * deshabilitados hasta que todos los campos requeridos están completos.
     * Este método verifica que el elemento esté presente, visible Y habilitado
     * (sin atributo {@code disabled}).
     *
     * <p><b>Ejemplo de uso típico:</b>
     * <pre>
     *   public boolean botonConfirmarHabilitado() {
     *       return isElementEnabled(BOTON_CONFIRMAR);
     *   }
     *   // En el Step: assertThat(page.botonConfirmarHabilitado()).isTrue();
     * </pre>
     *
     * @param locator estrategia de localización del elemento
     * @return {@code true} si el elemento está visible y habilitado;
     *         {@code false} si está deshabilitado o no es visible
     */
    public boolean isElementEnabled(By locator) {
        try {
            WebElement el = new WebDriverWait(driver, QUICK)
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
            return el.isEnabled();
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Comprueba si un elemento está <b>deshabilitado</b>.
     *
     * <p>Complemento de {@link #isElementEnabled(By)} para verificar que
     * controles de seguridad estén activos: un campo de monto no debería
     * ser editable si el tipo de operación no lo permite.
     *
     * @param locator estrategia de localización del elemento
     * @return {@code true} si el elemento está visible pero deshabilitado;
     *         {@code false} si está habilitado o no es visible
     */
    public boolean isElementDisabled(By locator) {
        try {
            WebElement el = new WebDriverWait(driver, QUICK)
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
            return !el.isEnabled();
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Comprueba si un elemento es visible dentro de un timeout configurable.
     *
     * <p>A diferencia de {@link #findVisible}, no lanza excepción si el elemento
     * no aparece: retorna {@code false}. Diseñado para verificaciones condicionales
     * sin interrumpir el flujo del test (ej. detectar si un modal opcional aparece).
     *
     * @param locator estrategia de localización del elemento
     * @param timeout tiempo máximo de espera
     * @return {@code true} si el elemento es visible dentro del timeout;
     *         {@code false} si se agotó el tiempo sin encontrarlo
     */
    public boolean isElementVisible(By locator, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Comprueba si un elemento es visible con timeout de 5 s.
     *
     * <p>Sobrecarga de conveniencia de {@link #isElementVisible(By, Duration)}
     * para verificaciones rápidas en las que 5 s es más que suficiente.
     *
     * @param locator estrategia de localización del elemento
     * @return {@code true} si el elemento es visible dentro de 5 s
     */
    public boolean isElementVisible(By locator) {
        return isElementVisible(locator, QUICK);
    }

    /**
     * Comprueba <b>instantáneamente</b> si un elemento existe en el DOM, sin espera.
     *
     * <p>Usa {@link WebDriver#findElements(By)} que retorna lista vacía
     * (nunca lanza excepción) si no hay coincidencias. No bloquea el hilo —
     * retorna de inmediato — por lo que es seguro para fast-paths y lógica
     * condicional de alto rendimiento.
     *
     * <p><b>Atención:</b> un elemento puede estar en el DOM pero oculto
     * ({@code display:none}). Usar {@link #isElementVisible(By, Duration)} si se necesita
     * confirmar que el usuario lo puede ver o interactuar con él.
     *
     * @param locator estrategia de localización del elemento
     * @return {@code true} si existe al menos un elemento con ese locator en el DOM
     */
    protected boolean isElementPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    // =====================================================================
    // ESPERAS Y SINCRONIZACIÓN
    // =====================================================================

    /**
     * Espera hasta que el atributo de un elemento contenga el valor indicado.
     *
     * <p>Fundamental para aplicaciones Angular que comunican estado a través
     * de atributos HTML dinámicos en lugar de visibilidad de elementos:
     * <ul>
     *   <li>Esperar {@code class} contenga {@code "active"} tras seleccionar una pestaña</li>
     *   <li>Esperar {@code aria-expanded="true"} tras abrir un acordeón</li>
     *   <li>Esperar que {@code disabled} desaparezca del atributo {@code class}</li>
     *   <li>Esperar {@code data-status="approved"} tras una aprobación asíncrona</li>
     * </ul>
     *
     * <p><b>Ejemplo:</b>
     * <pre>
     *   // Esperar que la pestaña "Parámetros" quede activa
     *   waitUntilAttributeContains(PESTANA_PARAMETROS, "class", "active");
     *
     *   // Esperar que el acordeón se expanda
     *   waitUntilAttributeContains(PANEL_ACORDEON, "aria-expanded", "true");
     * </pre>
     *
     * @param locator    estrategia de localización del elemento
     * @param attribute  nombre del atributo HTML a monitorear
     * @param value      valor (o fragmento) que debe contener el atributo
     * @throws org.openqa.selenium.TimeoutException si el atributo no contiene el valor
     *         dentro de {@link utils.TimeoutConstants#STANDARD}
     */
    protected void waitUntilAttributeContains(By locator, String attribute, String value) {
        wait.until(ExpectedConditions.attributeContains(locator, attribute, value));
    }

    /**
     * Espera hasta que un elemento esté <b>habilitado</b> (no deshabilitado).
     *
     * <p>Patrón común en formularios bancarios donde el botón de confirmación
     * se activa solo cuando todos los campos requeridos han sido completados
     * correctamente. Sin esta espera, un click prematuro fallaría silenciosamente.
     *
     * <p><b>Ejemplo:</b>
     * <pre>
     *   ingresarDatosDelFormulario(monto, cuenta, concepto);
     *   waitUntilEnabled(BOTON_CONFIRMAR); // Angular valida y habilita
     *   clickElement(BOTON_CONFIRMAR);
     * </pre>
     *
     * @param locator estrategia de localización del elemento a esperar
     * @throws org.openqa.selenium.TimeoutException si el elemento no se habilita
     *         dentro de {@link utils.TimeoutConstants#STANDARD}
     */
    protected void waitUntilEnabled(By locator) {
        wait.until(driver -> {
            try {
                WebElement el = driver.findElement(locator);
                return el.isDisplayed() && el.isEnabled() ? el : null;
            } catch (org.openqa.selenium.NoSuchElementException
                     | StaleElementReferenceException e) {
                // NoSuchElementException: aún no existe en el DOM.
                // StaleElementReferenceException: Angular re-renderizó el botón
                // mientras esperábamos — retornar null para reintentar.
                return null;
            }
        });
    }

    /**
     * Espera hasta que el texto del elemento <b>deje de contener</b> el valor indicado.
     *
     * <p>Complemento de {@link #waitUntilTextPresent(By, String)}.
     * Útil para:
     * <ul>
     *   <li>Esperar que un estado cambie de "Pendiente" a otro valor</li>
     *   <li>Confirmar que un mensaje de carga ("Procesando...") desapareció</li>
     *   <li>Verificar que un error temporal fue resuelto por el sistema</li>
     * </ul>
     *
     * @param locator estrategia de localización del elemento cuyo texto se monitorea
     * @param text    texto que debe desaparecer del contenido del elemento
     * @throws org.openqa.selenium.TimeoutException si el texto sigue presente
     *         dentro de {@link utils.TimeoutConstants#STANDARD}
     */
    protected void waitUntilTextNotPresent(By locator, String text) {
        wait.until(ExpectedConditions.not(
                ExpectedConditions.textToBePresentInElementLocated(locator, text)));
    }



    /**
     * Espera breve de 500 ms para estabilización de UI.
     *
     * <p>Usado específicamente para esperar que animaciones CSS terminen o que
     * Angular procese completamente un cambio de estado antes de la siguiente
     * interacción. Casos de uso:
     * <ul>
     *   <li>Después de que aparece un modal para que el botón sea interactuable</li>
     *   <li>Después de expandir un acordeón para que los elementos hijos se rendericen</li>
     * </ul>
     *
    /**
     * Espera un período breve (táctica) cuando se cumplen estas condiciones:
     *
     * <ol>
     *   <li>La condición a esperar NO puede expresarse con {@code ExpectedCondition}</li>
     *   <li>El tiempo es predecible y corto (&lt;1 s)</li>
     *   <li>El uso excesivo de esperas explícitas crea complejidad innecesaria</li>
     * </ol>
     *
     * <p><b>Optimización Marzo 2026:</b> Reducido de 500ms a 150ms basado en análisis
     * de performance. Angular completa animaciones de modales en ~100ms en hardware moderno.
     */
    protected void waitSmall() {
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Espera interrumpida", e);
        }
    }

    // =====================================================================
    // DROPDOWNS (SELECT HTML NATIVO)
    // =====================================================================

    /**
     * Selecciona una opción de un {@code <select>} HTML nativo por su texto visible.
     *
     * <p>Espera la presencia del select en el DOM antes de interactuar.
     * El texto es recortado de espacios ({@code trim()}) para tolerancia
     * ante valores con espacios accidentales en el feature.
     *
     * <p><b>Nota:</b> este método es para {@code <select>} HTML nativos.
     * Los dropdowns Angular Material usan {@code mat-select} y requieren
     * un approach diferente (click en el trigger + click en la opción).
     *
     * @param locator estrategia de localización del {@code <select>}
     * @param text    texto exacto de la opción a seleccionar
     */
    public void selectByText(By locator, String text) {
        Select select = new Select(find(locator));
        select.selectByVisibleText(text.trim());
    }

    /**
     * Obtiene todos los textos visibles de las opciones de un {@code <select>} HTML.
     *
     * <p>Útil para validar que un dropdown contiene las opciones esperadas
     * o para construir aserciones sobre el contenido del selector.
     *
     * @param locator estrategia de localización del {@code <select>}
     * @return lista de textos visibles de todas las opciones, en orden de aparición
     */
    public List<String> getDropdownValues(By locator) {
        Select dropdown = new Select(find(locator));
        List<String> values = new ArrayList<>();
        for (WebElement option : dropdown.getOptions()) {
            values.add(option.getText());
        }
        return values;
    }

    // =====================================================================
    // JAVASCRIPT HELPERS
    // =====================================================================

    /**
     * Ejecuta un script JavaScript en el contexto de la página actual.
     *
     * <p>Wrapper de {@link JavascriptExecutor#executeScript(String, Object...)}
     * para evitar el cast repetitivo en subclases. Los argumentos son accesibles
     * en el script vía {@code arguments[0]}, {@code arguments[1]}, etc.
     *
     * @param script script JavaScript a ejecutar
     * @param args   argumentos opcionales pasados al script
     */
    protected void executeJS(String script, Object... args) {
        ((JavascriptExecutor) driver).executeScript(script, args);
    }

    /**
     * Hace scroll para centrar el elemento en el viewport.
     *
     * <p>Usa {@code scrollIntoView({block:'center'})} para posicionar el elemento
     * en el centro vertical. Esto previene que overlays (headers fijos, banners,
     * notificaciones) intercepten el click posterior al desplazarse justo encima.
     *
     * <p>Llamado automáticamente por {@link #clickElement} antes de cada click.
     * También útil antes de aserciones visuales sobre elementos fuera del viewport.
     *
     * @param element elemento a centrar en el viewport
     */
    protected void scrollIntoViewCenter(WebElement element) {
        executeJS("arguments[0].scrollIntoView({block:'center'});", element);
    }

    /**
     * Hace click en un elemento via JavaScript, saltando la verificación de
     * interceptación del click nativo de Selenium.
     *
     * <p>Úsalo como fallback cuando el click nativo lanza
     * {@link ElementClickInterceptedException}: ocurre cuando un overlay Angular
     * (spinner, tooltip, modal en animación) cubre momentáneamente el elemento.
     * El click JS llega directamente al elemento en el DOM sin importar overlays.
     *
     * <p><b>Precaución:</b> el click JS no dispara eventos de mouse
     * ({@code mousedown}, {@code mouseover}, {@code mouseup}). Úsalo solo
     * cuando el click nativo falle persistentemente, no como primera opción.
     *
     * @param element elemento sobre el que ejecutar el click via JS
     */
    protected void jsClick(WebElement element) {
        executeJS("arguments[0].click();", element);
    }


    // =====================================================================
    // WAIT HELPERS
    // =====================================================================

    /**
     * Espera hasta que la URL del browser contenga el fragmento indicado.
     *
     * <p>Útil para confirmar que Angular completó una navegación de ruta.
     * Por ejemplo: {@code waitUntilUrlContains("/parametros")} confirma que
     * el router de Angular aterrizó en la pantalla de parámetros.
     *
     * @param partial fragmento de URL a buscar (case-sensitive)
     * @throws org.openqa.selenium.TimeoutException si no aparece dentro de
     *         {@link utils.TimeoutConstants#STANDARD}
     */
    protected void waitUntilUrlContains(String partial) {
        wait.until(ExpectedConditions.urlContains(partial));
    }

    /**
     * Espera hasta que el elemento indicado desaparezca del viewport
     * (invisible o removido del DOM).
     *
     * <p>Útil para esperar que un modal/spinner cierre completamente antes
     * de continuar. Sin esta espera, el siguiente click podría ser interceptado
     * por el overlay del modal que aún está animando su cierre.
     *
     * @param locator locator del elemento que debe desaparecer
     * @throws org.openqa.selenium.TimeoutException si sigue visible dentro de
     *         {@link utils.TimeoutConstants#STANDARD}
     */
    protected void waitUntilInvisible(By locator) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /**
     * Espera hasta que el texto del elemento contenga el valor indicado.
     *
     * <p>Útil para validar mensajes de éxito o error que se actualizan
     * dinámicamente por Angular (ej. "Proceso completado", "Error al guardar").
     *
     * @param locator locator del elemento cuyo texto se verifica
     * @param text    texto que debe contener el elemento
     * @throws org.openqa.selenium.TimeoutException si el texto no aparece
     *         dentro de {@link utils.TimeoutConstants#STANDARD}
     */
    protected void waitUntilTextPresent(By locator, String text) {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    // =====================================================================
    // ALERTAS NATIVAS DEL NAVEGADOR (window.alert / confirm / prompt)
    // =====================================================================
    // Estas alertas son ventanas emergentes nativas del navegador generadas
    // por JavaScript (alert(), confirm(), prompt()). Son diferentes a los
    // modales de Angular/HTML — estos bloquean el navegador completamente
    // hasta que el usuario responde. Selenium las maneja via driver.switchTo().alert().

    /**
     * Lee el texto de una alerta nativa del navegador.
     *
     * <p>Espera hasta que la alerta aparezca y retorna su mensaje.
     * Útil para capturar el texto antes de aceptar o rechazar.
     *
     * <p><b>Ejemplo de uso:</b>
     * <pre>
     *   String mensaje = getAlertText();
     *   assertThat(mensaje).contains("¿Confirma la operación?");
     *   acceptAlert();
     * </pre>
     *
     * @return texto del mensaje de la alerta
     * @throws org.openqa.selenium.TimeoutException si no aparece alerta
     *         dentro de {@link utils.TimeoutConstants#STANDARD}
     */
    public String getAlertText() {
        return wait.until(ExpectedConditions.alertIsPresent()).getText();
    }

    /**
     * Acepta (click OK) una alerta nativa del navegador.
     *
     * <p>Para alertas de confirmación ({@code window.confirm()}), acepta
     * y retorna {@code true} al código JavaScript que la invocó.
     * Para alertas simples ({@code window.alert()}), simplemente las cierra.
     *
     * @throws org.openqa.selenium.TimeoutException si no aparece alerta
     *         dentro de {@link utils.TimeoutConstants#STANDARD}
     */
    public void acceptAlert() {
        wait.until(ExpectedConditions.alertIsPresent()).accept();
    }

    /**
     * Rechaza (click Cancelar) una alerta de confirmación nativa.
     *
     * <p>Para alertas de confirmación ({@code window.confirm()}), cancela
     * y retorna {@code false} al código JavaScript que la invocó.
     *
     * @throws org.openqa.selenium.TimeoutException si no aparece alerta
     *         dentro de {@link utils.TimeoutConstants#STANDARD}
     */
    public void dismissAlert() {
        wait.until(ExpectedConditions.alertIsPresent()).dismiss();
    }

    /**
     * Escribe texto en una alerta de tipo prompt y la acepta.
     *
     * <p>Para alertas de entrada ({@code window.prompt()}): escribe el texto
     * y hace click en OK. Si la alerta no es un prompt, el texto es ignorado
     * y se acepta igualmente.
     *
     * @param text texto a escribir en el campo del prompt
     * @throws org.openqa.selenium.TimeoutException si no aparece alerta
     *         dentro de {@link utils.TimeoutConstants#STANDARD}
     */
    public void typeInAlert(String text) {
        org.openqa.selenium.Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        alert.sendKeys(text);
        alert.accept();
    }

    // =====================================================================
    // CARGA DE ARCHIVOS (File Upload)
    // =====================================================================
    // En HTML, los campos de carga de archivos son <input type="file">.
    // Selenium los maneja enviando la ruta absoluta del archivo como texto.
    // El campo no necesita ser visible — Chrome acepta el sendKeys igualmente.

    /**
     * Sube un archivo a través de un campo {@code <input type="file">}.
     *
     * <p>Funciona enviando la ruta absoluta del archivo al input de carga.
     * No requiere que el campo sea visible — Chrome procesa el sendKeys
     * directamente sobre el elemento del DOM, incluso si está oculto.
     *
     * <p><b>Ejemplo de uso en un Page Object:</b>
     * <pre>
     *   private static final By INPUT_ARCHIVO_EXCEL = By.id("fileInputExcel");
     *
     *   public void cargarArchivoExcel(String rutaAbsoluta) {
     *       uploadFile(INPUT_ARCHIVO_EXCEL, rutaAbsoluta);
     *   }
     *   // En el step: page.cargarArchivoExcel("C:/archivos/clientes.xlsx");
     * </pre>
     *
     * <p><b>Nota importante en CI/CD:</b> la ruta debe existir en el runner.
     * Para CI, colocar los archivos de prueba en {@code src/test/resources/testdata/}
     * y construir la ruta con {@code System.getProperty("user.dir")}.
     *
     * @param locator      locator del {@code <input type="file">}
     * @param absolutePath ruta absoluta del archivo en el sistema de archivos
     */
    public void uploadFile(By locator, String absolutePath) {
        log.debug("Uploading file: {} to element: {}", absolutePath, locator);
        find(locator).sendKeys(absolutePath);
    }

    // =====================================================================
    // DESCARGA Y VERIFICACIÓN DE ARCHIVOS (Excel, PDF)
    // =====================================================================
    // Chrome guarda los archivos descargados en el directorio configurado
    // en BrowserConfig (default: build/downloads/).
    // Mientras el archivo se descarga, Chrome crea un .crdownload temporal.
    // Solo cuando desaparece el .crdownload el archivo está completo.

    /**
     * Retorna el directorio donde Chrome guarda los archivos descargados.
     * Coincide con el {@code downloadDirectory} configurado en BrowserConfig.
     */
    private static String getDownloadDirectory() {
        return System.getProperty("user.dir") + "/build/downloads";
    }

    /**
     * Espera hasta que un archivo aparezca en el directorio de descargas.
     *
     * <p>Detecta el archivo buscando por nombre parcial o completo, ignorando
     * archivos {@code .crdownload} y {@code .tmp} (descargas en progreso de Chrome).
     * Solo retorna cuando el archivo está completamente descargado.
     *
     * <p><b>Ejemplo de uso:</b>
     * <pre>
     *   clickElement(BOTON_EXPORTAR_EXCEL);
     *   waitUntilFileDownloaded("reporte_clientes", Duration.ofSeconds(30));
     *   // El archivo "reporte_clientes_2026-03-15.xlsx" ya está en build/downloads/
     * </pre>
     *
     * @param fileNamePattern fragmento del nombre del archivo a esperar
     *                        (ej. "reporte_clientes" encuentra "reporte_clientes_2026.xlsx")
     * @param timeout         tiempo máximo de espera para la descarga
     * @throws org.openqa.selenium.TimeoutException si el archivo no aparece
     *         dentro del timeout indicado
     */
    public void waitUntilFileDownloaded(String fileNamePattern, Duration timeout) {
        log.debug("Waiting for file download: '{}' in {}", fileNamePattern, getDownloadDirectory());
        long endTime = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < endTime) {
            if (isFileDownloaded(fileNamePattern)) {
                log.debug("File downloaded successfully: '{}'", fileNamePattern);
                return;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Espera de descarga interrumpida", e);
            }
        }
        throw new org.openqa.selenium.TimeoutException(
            "Archivo '" + fileNamePattern + "' no se descargo en " + timeout.getSeconds()
            + " segundos. Directorio: " + getDownloadDirectory());
    }

    /**
     * Verifica inmediatamente si un archivo ya fue descargado completamente.
     *
     * <p>Busca en el directorio de descargas un archivo cuyo nombre contenga
     * {@code fileNamePattern}. Ignora archivos {@code .crdownload} y {@code .tmp}
     * que Chrome crea durante la descarga.
     *
     * @param fileNamePattern fragmento del nombre del archivo a buscar
     * @return {@code true} si existe al menos un archivo descargado que coincide;
     *         {@code false} si no existe o aún está en progreso
     */
    public boolean isFileDownloaded(String fileNamePattern) {
        java.io.File dir = new java.io.File(getDownloadDirectory());
        if (!dir.exists()) return false;
        java.io.File[] matches = dir.listFiles((d, name) ->
            name.contains(fileNamePattern)
            && !name.endsWith(".crdownload")
            && !name.endsWith(".tmp"));
        return matches != null && matches.length > 0;
    }

    /**
     * Obtiene la ruta absoluta del archivo descargado para inspeccionarlo.
     *
     * <p>Útil cuando se necesita verificar el contenido del archivo descargado
     * (por ejemplo, validar que un Excel tiene las columnas correctas con una
     * librería como Apache POI).
     *
     * @param fileNamePattern fragmento del nombre del archivo a buscar
     * @return ruta absoluta del archivo encontrado
     * @throws RuntimeException si el archivo no existe en el directorio de descargas
     */
    public String getDownloadedFilePath(String fileNamePattern) {
        java.io.File dir = new java.io.File(getDownloadDirectory());
        java.io.File[] matches = dir.listFiles((d, name) ->
            name.contains(fileNamePattern)
            && !name.endsWith(".crdownload")
            && !name.endsWith(".tmp"));
        if (matches == null || matches.length == 0) {
            throw new RuntimeException(
                "Archivo '" + fileNamePattern + "' no encontrado en: " + getDownloadDirectory());
        }
        return matches[0].getAbsolutePath();
    }

    /**
     * Elimina los archivos descargados que coincidan con el patron.
     *
     * <p>Llamar en el {@code @After} de Hooks o al inicio del escenario para
     * evitar que descargas de tests anteriores interfieran con las verificaciones
     * del test actual.
     *
     * @param fileNamePattern fragmento del nombre del archivo a eliminar
     */
    public void deleteDownloadedFile(String fileNamePattern) {
        java.io.File dir = new java.io.File(getDownloadDirectory());
        java.io.File[] matches = dir.listFiles((d, name) -> name.contains(fileNamePattern));
        if (matches != null) {
            for (java.io.File f : matches) {
                log.debug("Deleting downloaded file: {}", f.getAbsolutePath());
                f.delete();
            }
        }
    }

    // =====================================================================
    // TABLAS Y GRIDS DE DATOS
    // =====================================================================
    // Las tablas son fundamentales en banca: historiales, listados de clientes,
    // resultados de búsqueda, reportes. Estos métodos simplifican la interacción
    // con tablas HTML (<table>, <tr>, <td>) y grids de Angular Material.

    /**
     * Obtiene el texto de todas las celdas que coinciden con el locator.
     *
     * <p>Usa el locator para seleccionar todas las celdas de una columna
     * y retorna sus textos como lista. Útil para verificar el contenido
     * completo de una columna o buscar un valor específico.
     *
     * <p><b>Ejemplo:</b>
     * <pre>
     *   private static final By COLUMNA_ESTADO = By.cssSelector("table tbody tr td:nth-child(3)");
     *
     *   public List&lt;String&gt; obtenerEstadosDeLaTabla() {
     *       return getColumnValues(COLUMNA_ESTADO);
     *       // Retorna: ["Aprobado", "Pendiente", "Rechazado", "Aprobado"]
     *   }
     * </pre>
     *
     * @param cellsLocator locator que selecciona todas las celdas de la columna
     * @return lista de textos de cada celda, en orden de aparición
     */
    public List<String> getColumnValues(By cellsLocator) {
        List<String> values = new ArrayList<>();
        for (WebElement cell : findAll(cellsLocator)) {
            values.add(cell.getText().trim());
        }
        return values;
    }

    /**
     * Busca y retorna la primera fila de una tabla que contenga el texto indicado.
     *
     * <p>Útil para encontrar un registro específico en una tabla y luego
     * hacer clic en su botón de acción (editar, ver detalle, aprobar).
     *
     * <p><b>Ejemplo:</b>
     * <pre>
     *   private static final By FILAS_TABLA    = By.cssSelector("table tbody tr");
     *   private static final By BTN_VER_DETALLE = By.cssSelector("button.btn-detalle");
     *
     *   public void verDetalleDelCliente(String dni) {
     *       WebElement fila = findRowContaining(FILAS_TABLA, dni);
     *       fila.findElement(BTN_VER_DETALLE).click();
     *   }
     * </pre>
     *
     * @param rowsLocator locator que selecciona todas las filas ({@code <tr>})
     * @param searchText  texto a buscar dentro de cualquier celda de la fila
     * @return el {@link WebElement} de la primera fila que contiene el texto
     * @throws org.openqa.selenium.NoSuchElementException si ninguna fila contiene el texto
     */
    public WebElement findRowContaining(By rowsLocator, String searchText) {
        for (WebElement row : findAll(rowsLocator)) {
            if (row.getText().contains(searchText)) {
                return row;
            }
        }
        throw new org.openqa.selenium.NoSuchElementException(
            "Ninguna fila de la tabla contiene el texto: '" + searchText + "'");
    }

    /**
     * Verifica si alguna fila de la tabla contiene el texto indicado.
     *
     * <p><b>Anti-flaky:</b> usa {@link #findAll(By)} en lugar de
     * {@code driver.findElements()} directamente. {@code findAll()} espera
     * hasta que al menos una fila sea visible antes de buscar el texto —
     * evita falsos negativos cuando la tabla aún está cargando sus datos.
     *
     * <p><b>Ejemplo:</b>
     * <pre>
     *   public boolean clienteApareceEnResultados(String dni) {
     *       return isTextPresentInTable(FILAS_TABLA, dni);
     *   }
     *   // En el step: assertThat(page.clienteApareceEnResultados("12345678")).isTrue();
     * </pre>
     *
     * @param rowsLocator locator que selecciona todas las filas de la tabla
     * @param searchText  texto a buscar en cualquier celda de cualquier fila
     * @return {@code true} si alguna fila contiene el texto;
     *         {@code false} si la tabla no carga o no se encuentra el texto
     */
    public boolean isTextPresentInTable(By rowsLocator, String searchText) {
        try {
            return findAll(rowsLocator).stream()
                    .anyMatch(row -> row.getText().contains(searchText));
        } catch (TimeoutException e) {
            log.warn("Table rows not visible within timeout for locator: {}", rowsLocator);
            return false;
        }
    }

    /**
     * Cuenta el número de filas de datos en una tabla (sin contar el encabezado).
     *
     * <p><b>Anti-flaky:</b> usa {@link #findAll(By)} que espera a que las filas
     * sean visibles antes de contarlas — a diferencia de {@link #countElements(By)}
     * que retorna 0 inmediatamente si la tabla aún está cargando.
     *
     * <p>Para tablas que pueden estar legítimamente vacías, usar
     * {@link #countElements(By)} si no quieres esperar el timeout completo.
     *
     * <p><b>Ejemplo:</b>
     * <pre>
     *   public int obtenerCantidadDeResultados() {
     *       return getTableRowCount(FILAS_TABLA);
     *   }
     *   // En el step: assertThat(page.obtenerCantidadDeResultados()).isGreaterThan(0);
     * </pre>
     *
     * @param rowsLocator locator de las filas de datos (excluyendo encabezados)
     * @return número de filas visibles; 0 si la tabla no carga dentro del timeout
     */
    public int getTableRowCount(By rowsLocator) {
        try {
            return findAll(rowsLocator).size();
        } catch (TimeoutException e) {
            log.warn("Table rows not visible for count. Returning 0 for: {}", rowsLocator);
            return 0;
        }
    }

    // =====================================================================
    // PESTAÑAS Y VENTANAS DEL NAVEGADOR
    // =====================================================================
    // Algunos flujos bancarios abren PDFs o reportes en una nueva pestaña.
    // Chrome abre el archivo en una nueva ventana/pestaña del driver.
    // Se debe cambiar el "foco" del driver a esa nueva pestaña para interactuar.

    /**
     * Cambia el foco del driver a la pestaña más recientemente abierta.
     *
     * <p>Espera hasta que exista al menos una pestaña adicional a la actual.
     * Útil cuando un botón de "Ver PDF" o "Descargar reporte" abre el archivo
     * en una nueva pestaña del navegador.
     *
     * <p><b>Ejemplo de uso:</b>
     * <pre>
     *   clickElement(BOTON_VER_PDF);      // abre PDF en nueva pestaña
     *   switchToNewTab();                  // driver se mueve a la nueva pestaña
     *   String titulo = getPageTitle();    // leer título del PDF
     *   closeCurrentTabAndSwitchBack();   // cerrar PDF y volver
     * </pre>
     *
     * @throws org.openqa.selenium.TimeoutException si no se abre una nueva pestaña
     *         dentro de {@link utils.TimeoutConstants#STANDARD}
     */
    public void switchToNewTab() {
        String currentHandle = driver.getWindowHandle();
        wait.until(d -> d.getWindowHandles().size() > 1);
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(currentHandle)) {
                driver.switchTo().window(handle);
                log.debug("Switched to new tab: {}", handle);
                return;
            }
        }
    }

    /**
     * Vuelve el foco del driver a la pestaña principal (la primera abierta).
     *
     * <p>Usar después de terminar de interactuar con una pestaña secundaria
     * (PDF, reporte, ventana de confirmación) para regresar al flujo principal.
     */
    public void switchToMainTab() {
        String firstHandle = driver.getWindowHandles().iterator().next();
        driver.switchTo().window(firstHandle);
        log.debug("Switched back to main tab");
    }

    /**
     * Cierra la pestaña activa y vuelve a la pestaña principal.
     *
     * <p>Combina {@code driver.close()} con {@link #switchToMainTab()}.
     * Útil al final de flujos que abren una pestaña temporal (PDF, confirmación).
     */
    public void closeCurrentTabAndSwitchBack() {
        driver.close();
        switchToMainTab();
    }

    // =====================================================================
    // LECTURA DE CAMPOS Y ESTADO DE LA PÁGINA
    // =====================================================================

    /**
     * Obtiene el valor actual de un campo de formulario ({@code <input>}, {@code <textarea>}).
     *
     * <p>Diferente de {@link #getText(By)}, que lee el texto visible del elemento.
     * {@code getInputValue} lee el atributo {@code value} — el contenido del campo
     * de texto que el usuario puede editar.
     *
     * <p><b>Cuándo usar cuál:</b>
     * <pre>
     *   getText(locator)       → para <span>, <p>, <td>, <label>: texto visible
     *   getInputValue(locator) → para <input>, <textarea>: valor del campo editable
     * </pre>
     *
     * @param locator estrategia de localización del campo de formulario
     * @return valor actual del campo; cadena vacía si el campo está vacío
     */
    public String getInputValue(By locator) {
        String value = getAttribute(locator, "value");
        return value != null ? value : "";
    }

    /**
     * Retorna el título de la pestaña actual del navegador.
     *
     * <p>Útil para verificar que se abrió la pantalla correcta cuando el
     * título del tab refleja la sección activa de la aplicación.
     *
     * @return texto del título del tab del navegador
     */
    public String getPageTitle() {
        return driver.getTitle();
    }

    /**
     * Espera hasta que la página complete su carga (incluyendo JavaScript).
     *
     * <p>Verifica {@code document.readyState === "complete"}, que Angular
     * reporta cuando el DOM y los recursos iniciales han cargado.
     * No garantiza que componentes Angular asíncronos hayan terminado —
     * para eso usar {@link #waitUntilInvisible(By)} sobre el spinner de carga.
     *
     * <p>Útil después de navegaciones en sistemas con páginas pesadas
     * (reportes con grandes volúmenes de datos, pantallas de parámetros complejos).
     *
     * @throws org.openqa.selenium.TimeoutException si la página no termina de cargar
     *         dentro de {@link utils.TimeoutConstants#LONG}
     */
    public void waitForPageLoad() {
        new WebDriverWait(driver, LONG).until(d ->
            ((JavascriptExecutor) d)
                .executeScript("return document.readyState")
                .equals("complete"));
    }

    /**
     * Hace scroll al inicio de la página (posición 0,0).
     *
     * <p>Útil en formularios largos donde se necesita volver al inicio
     * para interactuar con elementos del encabezado o la barra de navegación
     * después de haber scrolleado hacia abajo.
     */
    public void scrollToTop() {
        executeJS("window.scrollTo(0, 0);");
    }

    /**
     * Hace scroll al final de la página.
     *
     * <p>Útil para:
     * <ul>
     *   <li>Activar carga diferida (lazy loading) de tablas largas</li>
     *   <li>Llegar a botones de acción al final de formularios extensos</li>
     *   <li>Verificar pie de página o totales al final de un reporte</li>
     * </ul>
     */
    public void scrollToBottom() {
        executeJS("window.scrollTo(0, document.body.scrollHeight);");
    }

    // =====================================================================
    // MÉTODOS ANTI-FLAKY AVANZADOS
    // =====================================================================
    // Métodos de alto nivel que combinan espera + acción + verificación
    // para escenarios donde las primitivas básicas son insuficientes.

    /**
     * Espera hasta que el texto visible de un elemento sea <b>no vacío</b>.
     *
     * <p>Complemento explícito de {@link #getText(By)} para casos donde la
     * espera del texto no vacío es una precondición crítica del test, no
     * solo un detalle de implementación. Usar en Steps cuando el texto
     * de un elemento es el resultado visible de una operación asíncrona:
     *
     * <pre>
     *   // Paso 1: el sistema calcula el saldo disponible (operación async)
     *   clickElement(BOTON_CALCULAR_SALDO);
     *   // Paso 2: esperar explícitamente que el resultado aparezca antes de leerlo
     *   waitUntilTextNotEmpty(CAMPO_SALDO_DISPONIBLE);
     *   String saldo = getText(CAMPO_SALDO_DISPONIBLE);
     * </pre>
     *
     * @param locator estrategia de localización del elemento cuyo texto se espera
     * @throws org.openqa.selenium.TimeoutException si el texto sigue vacío
     *         dentro de {@link utils.TimeoutConstants#STANDARD}
     */
    protected void waitUntilTextNotEmpty(By locator) {
        wait.until(d -> {
            try {
                String text = d.findElement(locator).getText();
                return (text != null && !text.trim().isEmpty()) ? text : null;
            } catch (org.openqa.selenium.NoSuchElementException e) {
                return null;
            }
        });
    }

    /**
     * Espera hasta que al menos {@code minCount} elementos coincidan con el locator.
     *
     * <p>Fundamental para tablas y listas que cargan datos asincrónamente.
     * Evita el falso negativo de contar filas justo cuando Angular aún no
     * ha terminado de renderizar los resultados de una búsqueda o filtrado.
     *
     * <p><b>Ejemplo — esperar que la búsqueda retorne resultados:</b>
     * <pre>
     *   clickElement(BOTON_BUSCAR);
     *   waitUntilCountAtLeast(FILAS_TABLA, 1); // espera que haya al menos 1 resultado
     *   assertThat(page.obtenerCantidadDeResultados()).isGreaterThan(0);
     * </pre>
     *
     * <p><b>Ejemplo — verificar paginación:</b>
     * <pre>
     *   waitUntilCountAtLeast(FILAS_TABLA, 10); // página de 10 elementos cargada
     * </pre>
     *
     * @param locator   locator que selecciona los elementos a contar
     * @param minCount  número mínimo de elementos visibles a esperar
     * @throws org.openqa.selenium.TimeoutException si no se alcanza {@code minCount}
     *         dentro de {@link utils.TimeoutConstants#STANDARD}
     */
    protected void waitUntilCountAtLeast(By locator, int minCount) {
        wait.until(d -> {
            List<WebElement> elements = d.findElements(locator);
            long visibleCount = elements.stream()
                    .filter(WebElement::isDisplayed)
                    .count();
            return visibleCount >= minCount ? elements : null;
        });
    }

    /**
     * Hace click en un elemento y espera que aparezca un elemento resultado.
     *
     * <p>Patrón anti-flaky para acciones que desencadenan una transición de pantalla
     * o aparición de un elemento nuevo. Evita el "click y hacer assert inmediato"
     * que falla cuando Angular aún no ha procesado la respuesta del servidor.
     *
     * <p><b>Ejemplo — clic en "Buscar" y esperar que la tabla aparezca:</b>
     * <pre>
     *   public void buscarYEsperarResultados(String dni) {
     *       write(CAMPO_DNI, dni);
     *       clickAndWaitForElement(BOTON_BUSCAR, TABLA_RESULTADOS);
     *   }
     * </pre>
     *
     * <p><b>Ejemplo — clic en "Confirmar" y esperar mensaje de éxito:</b>
     * <pre>
     *   public void confirmarOperacionYEsperarExito() {
     *       clickAndWaitForElement(BOTON_CONFIRMAR, MENSAJE_EXITO);
     *   }
     * </pre>
     *
     * @param clickTarget     locator del elemento a clickear
     * @param expectedElement locator del elemento que debe aparecer tras el click
     * @throws org.openqa.selenium.TimeoutException si el elemento esperado no aparece
     *         dentro de {@link utils.TimeoutConstants#STANDARD}
     */
    protected void clickAndWaitForElement(By clickTarget, By expectedElement) {
        clickElement(clickTarget);
        findVisible(expectedElement);
        log.debug("Clicked {} and confirmed {} appeared", clickTarget, expectedElement);
    }

    /**
     * Escribe en un campo y verifica que el valor fue aceptado por Angular.
     *
     * <p>Versión defensiva de {@link #write(By, String)} para campos críticos
     * de alta carga (montos, cuentas, DNIs) donde una escritura fallida causaría
     * falso positivo. Verifica que el atributo {@code value} del campo refleja
     * el texto ingresado después de la escritura.
     *
     * <p>Si el valor no coincide tras la escritura, reintenta hasta
     * {@link utils.TimeoutConstants#MAX_CLICK_RETRIES} veces antes de fallar.
     *
     * <p><b>Cuándo usar {@code writeAndVerify} vs {@code write}:</b>
     * <ul>
     *   <li>{@code write}: para campos normales de formulario</li>
     *   <li>{@code writeAndVerify}: para campos de monto, cuenta, DNI, IBAN —
     *       donde un valor incorrecto podría pasar desapercibido y el test
     *       daría falso positivo</li>
     * </ul>
     *
     * @param locator estrategia de localización del campo
     * @param value   texto a ingresar y verificar
     * @throws AutomationException si después de los reintentos el valor del campo
     *         no coincide con {@code value}
     */
    protected void writeAndVerify(By locator, String value) {
        for (int attempt = 0; attempt < MAX_CLICK_RETRIES; attempt++) {
            write(locator, value);
            String actual = getInputValue(locator);
            if (value.equals(actual)) {
                log.debug("writeAndVerify confirmed value '{}' in field: {}", value, locator);
                return;
            }
            log.warn("writeAndVerify mismatch attempt {}/{}: expected='{}' actual='{}' field={}",
                    attempt + 1, MAX_CLICK_RETRIES, value, actual, locator);
        }
        String finalValue = getInputValue(locator);
        throw AutomationException.invalidFormat(
            locator.toString(), value,
            String.format("Valor actual tras %d intentos: '%s'. "
                + "Verificar si el campo tiene validación de formato o máscara de entrada.",
                MAX_CLICK_RETRIES, finalValue));
    }

    // =====================================================================
    // SSL WARMUP — PÁGINA WEB
    // =====================================================================

    /**
     * Acepta el certificado SSL inválido de una URL de aplicación web.
     *
     * <p>En entornos de prueba, el servidor puede tener un certificado
     * autofirmado o caducado. Chrome bloquea la navegación con una página de
     * advertencia. Este método detecta esa página y la omite vía la secuencia
     * estándar de Chrome: "Avanzado" → "Continuar de todos modos" ({@code proceed-link}),
     * o el comando de bypass {@code thisisunsafe} si los botones no están disponibles.
     *
     * <p>Para la API backend (XHR desde Angular), usar {@link #warmupApiSsl(String)}.
     *
     * @param url URL de la aplicación web con certificado SSL a aceptar
     */
    public void warmupSslFor(String url) {
        driver.get(url);

        boolean sslPage =
                driver.getPageSource().toLowerCase().contains("err_cert")
                        || driver.getTitle().toLowerCase().contains("not private");

        if (sslPage) {
            try {
                By btnAdvanced = By.id("details-button");
                By btnProceed  = By.id("proceed-link");

                if (isElementVisible(btnAdvanced, INSTANT)) {
                    clickElement(btnAdvanced);
                }
                if (isElementVisible(btnProceed, INSTANT)) {
                    clickElement(btnProceed);
                } else {
                    new Actions(driver).sendKeys("thisisunsafe").perform();
                }
            } catch (Exception ignored) {
                new Actions(driver).sendKeys("thisisunsafe").perform();
            }
        }
    }

    // =====================================================================
    // SSL WARMUP — API BACKEND (DEPRECADO - MARZO 2026)
    // =====================================================================

    /**
     * Acepta el certificado SSL inválido de la API backend en el browser.
     *
     * <p><b>⚠️ DEPRECADO:</b> Ya no es necesario cuando la infraestructura de QA
     * usa certificados válidos. Este método agregaba overhead innecesario de ~4 segundos por test.
     *
     * <p><b>Problema original que resolvía:</b> cuando el API backend usaba HTTPS con un
     * certificado no confiable en el entorno de pruebas. Al hacer click en
     * "INGRESAR", Angular realizaba un XHR al API; si el certificado no estaba
     * aceptado en el browser, Chrome bloqueaba el XHR con
     * {@code ERR_CERT_AUTHORITY_INVALID} → el login fallaba con error de red.
     *
     * <p><b>Razón de la deprecación:</b>
     * <ul>
     *   <li>La infraestructura de QA ahora usa certificados válidos</li>
     *   <li>Eliminaba overhead de 4+ segundos por test</li>
     *   <li>Mejora performance de suite completa en ~40 segundos (10 tests)</li>
     * </ul>
     *
     * <p><b>Migración:</b> Eliminar todas las llamadas a este método de {@link hooks.Hooks}.
     * Si en el futuro se necesita nuevamente, habilitar mediante feature flag en configuración.
     *
     * @param apiUrl URL del endpoint de la API backend (ej. URL de cifrado de clave)
     * @deprecated Desde marzo 2026. La web ya no usa certificados inseguros.
     *             Eliminar llamadas de Hooks y configuración.
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public void warmupApiSsl(String apiUrl) {
        try {
            driver.get(apiUrl);

            boolean sslError =
                    driver.getPageSource().toLowerCase().contains("err_cert")
                            || driver.getTitle().toLowerCase().contains("not private");

            if (sslError) {
                try {
                    By btnAdvanced = By.id("details-button");
                    By btnProceed  = By.id("proceed-link");

                    if (isElementVisible(btnAdvanced, INSTANT)) {
                        clickElement(btnAdvanced);
                    }
                    if (isElementVisible(btnProceed, INSTANT)) {
                        clickElement(btnProceed);
                    } else {
                        new Actions(driver).sendKeys("thisisunsafe").perform();
                    }
                } catch (Exception ignored) {
                    new Actions(driver).sendKeys("thisisunsafe").perform();
                }
            }
        } catch (Exception ignored) {}
    }
}
