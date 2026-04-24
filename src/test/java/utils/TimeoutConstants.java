package utils;

import java.time.Duration;

/**
 * Centralized timeout and polling constants for the automation framework.
 *
 * <h3>Purpose</h3>
 * <p>Eliminates magic numbers throughout the codebase and provides a single
 * source of truth for all timing-related values. This improves maintainability:
 * changing a timeout across the entire framework requires modifying only one constant.
 *
 * <h3>Usage Guidelines</h3>
 * <table border="1">
 *   <tr>
 *     <th>Constant</th>
 *     <th>Duration</th>
 *     <th>When to Use</th>
 *   </tr>
 *   <tr>
 *     <td>{@link #STANDARD}</td>
 *     <td>30s</td>
 *     <td>Default for most UI interactions (buttons, fields, dropdowns)</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #QUICK}</td>
 *     <td>5s</td>
 *     <td>Fast checks like modal detection, toast messages</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #INSTANT}</td>
 *     <td>3s</td>
 *     <td>Immediate presence checks without blocking (SSL buttons, overlays)</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #LONG}</td>
 *     <td>45s</td>
 *     <td>Heavy operations: page loads, batch calculations, report generation</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #CLICK}</td>
 *     <td>20s</td>
 *     <td>Click operations with built-in retry mechanism</td>
 *   </tr>
 * </table>
 *
 * <h3>Polling Intervals</h3>
 * <table border="1">
 *   <tr>
 *     <th>Constant</th>
 *     <th>Interval</th>
 *     <th>When to Use</th>
 *   </tr>
 *   <tr>
 *     <td>{@link #POLLING_FAST}</td>
 *     <td>100ms</td>
 *     <td>Conditions resolving in &lt;1s (login form, modal animations)</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #POLLING_STANDARD}</td>
 *     <td>500ms</td>
 *     <td>Normal UI operations (default Selenium polling)</td>
 *   </tr>
 * </table>
 *
 * <h3>Example Usage</h3>
 * <pre>{@code
 * import static utils.TimeoutConstants.*;
 *
 * public class LoginPage extends BasePage {
 *     public void waitForLoginForm() {
 *         FluentWait<WebDriver> fastWait = new WebDriverWait(driver, STANDARD)
 *                 .pollingEvery(POLLING_FAST);
 *
 *         fastWait.until(ExpectedConditions.visibilityOfElementLocated(LOGIN_FORM));
 *     }
 *
 *     public boolean isModalVisible() {
 *         return isElementVisible(MODAL, QUICK);  // 5s timeout
 *     }
 * }
 * }</pre>
 *
 * <h3>Performance Impact</h3>
 * <p>Using {@link #POLLING_FAST} (100ms) instead of {@link #POLLING_STANDARD} (500ms)
 * saves up to 400ms per wait condition when the condition resolves quickly. In a test
 * suite with 50 fast wait operations, this saves ~20 seconds total.
 *
 * @see pages.BasePage
 * @see org.openqa.selenium.support.ui.WebDriverWait
 */
public final class TimeoutConstants {

    /**
     * Private constructor prevents instantiation of this utility class.
     * Attempting to create an instance will throw an exception.
     */
    private TimeoutConstants() {
        throw new UnsupportedOperationException(
            "TimeoutConstants is a utility class and cannot be instantiated"
        );
    }

    // =========================================================================
    // TIMEOUT DURATIONS
    // =========================================================================

    /**
     * Standard timeout for most UI element interactions.
     *
     * <p><b>Duration:</b> 30 seconds (aumentado de 20s para soportar paralelización)
     *
     * <p><b>Nota Marzo 2026:</b> Aumentado a 30s para soportar ejecución paralela
     * (parallelism=3). Bajo paralelización, la competencia por recursos requiere
     * márgenes más amplios.
     *
     * <p><b>Use for:</b>
     * <ul>
     *   <li>Button clicks</li>
     *   <li>Input field interactions</li>
     *   <li>Dropdown selections</li>
     *   <li>Standard page element waits</li>
     * </ul>
     */
    public static final Duration STANDARD = Duration.ofSeconds(30);

    /**
     * Quick timeout for fast checks and lightweight operations.
     *
     * <p><b>Duration:</b> 5 seconds
     *
     * <p><b>Use for:</b>
     * <ul>
     *   <li>Modal detection</li>
     *   <li>Toast message appearance</li>
     *   <li>Quick presence validations</li>
     *   <li>Form validation messages</li>
     * </ul>
     */
    public static final Duration QUICK = Duration.ofSeconds(5);

    /**
     * Instant timeout for immediate presence checks without long blocking.
     *
     * <p><b>Duration:</b> 3 seconds
     *
     * <p><b>Use for:</b>
     * <ul>
     *   <li>SSL warning buttons (Advanced, Proceed)</li>
     *   <li>Optional overlay checks</li>
     *   <li>Conditional element presence (if-else logic)</li>
     * </ul>
     *
     * <p><b>Warning:</b> Do not use for critical elements that must be present.
     * This timeout is too short for elements that may take time to load.
     */
    public static final Duration INSTANT = Duration.ofSeconds(3);

    /**
     * Long timeout for heavy operations that require extended wait time.
     *
     * <p><b>Duration:</b> 45 seconds (aumentado de 30s para soportar paralelización)
     *
     * <p><b>Nota Marzo 2026:</b> Aumentado a 45s para operaciones pesadas bajo
     * ejecución paralela. Propuestas con muchos parámetros pueden tardar más.
     *
     * <p><b>Use for:</b>
     * <ul>
     *   <li>Full page loads with heavy data</li>
     *   <li>Batch calculation processes</li>
     *   <li>Report generation</li>
     *   <li>Large data grid rendering</li>
     *   <li>API calls with long response times</li>
     * </ul>
     */
    public static final Duration LONG = Duration.ofSeconds(45);

    /**
     * Click timeout allowing up to 3 retry attempts for stale elements.
     *
     * <p><b>Duration:</b> 20 seconds (aumentado de 15s para soportar paralelización)
     *
     * <p><b>Nota Marzo 2026:</b> Aumentado a 20s bajo paralelización. Con 3 threads
     * compitiendo por recursos, los clicks pueden tardar más en ser clickeables.
     *
     * <p><b>Use for:</b>
     * <ul>
     *   <li>Click operations in {@link pages.BasePage#clickElement(org.openqa.selenium.By)}</li>
     *   <li>Actions requiring retry logic for StaleElementReferenceException</li>
     * </ul>
     *
     * <p><b>Design Note:</b> This timeout is shorter than {@link #STANDARD} because
     * the click mechanism includes 3 retries with refresh waits between them.
     * Total effective wait time: 20s × 3 attempts = 60s maximum.
     */
    public static final Duration CLICK = Duration.ofSeconds(20);

    // =========================================================================
    // POLLING INTERVALS
    // =========================================================================

    /**
     * Fast polling interval for conditions that resolve quickly (&lt; 1 second).
     *
     * <p><b>Interval:</b> 100 milliseconds
     *
     * <p><b>Use for:</b>
     * <ul>
     *   <li>Login form detection</li>
     *   <li>Modal open/close animations</li>
     *   <li>Button enable/disable state changes</li>
     *   <li>Toast message appearance</li>
     * </ul>
     *
     * <p><b>Performance Benefit:</b> Saves up to 400ms per wait compared to
     * {@link #POLLING_STANDARD} when the condition resolves in 1-3 checks.
     */
    public static final Duration POLLING_FAST = Duration.ofMillis(100);

    /**
     * Standard polling interval for normal UI operations.
     *
     * <p><b>Interval:</b> 500 milliseconds (Selenium default)
     *
     * <p><b>Use for:</b>
     * <ul>
     *   <li>Page element waits</li>
     *   <li>Navigation completion checks</li>
     *   <li>Default wait operations</li>
     * </ul>
     *
     * <p><b>Note:</b> This is Selenium's default polling interval. Using it
     * explicitly makes the intent clear and allows easy adjustment if needed.
     */
    public static final Duration POLLING_STANDARD = Duration.ofMillis(500);

    // =========================================================================
    // RETRY CONFIGURATION
    // =========================================================================

    /**
     * Maximum number of retry attempts for click operations.
     *
     * <p><b>Value:</b> 3 attempts
     *
     * <p><b>Rationale:</b> Angular applications frequently re-render DOM elements.
     * Three attempts provide a good balance between resilience (handling transient
     * stale element issues) and fail-fast behavior (detecting real problems quickly).
     */
    public static final int MAX_CLICK_RETRIES = 3;

    /**
     * Delay between retry attempts for stale element recovery.
     *
     * <p><b>Duration:</b> 500 milliseconds
     *
     * <p><b>Use for:</b> Waiting for Angular to complete DOM refresh between
     * click retry attempts in {@link pages.BasePage#clickElement(org.openqa.selenium.By)}.
     */
    public static final Duration RETRY_DELAY = Duration.ofMillis(500);
}
