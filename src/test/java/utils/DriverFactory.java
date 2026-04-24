package utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Fábrica de WebDriver con soporte completo para ejecución paralela.
 *
 * <h3>Thread-safety</h3>
 * <ul>
 *   <li>{@link #driverThreadLocal} garantiza que cada hilo del pool de Cucumber tiene
 *       su propia instancia de Chrome; los hilos no comparten estado del driver.</li>
 *   <li>{@link #WDM_LOCK} serializa la primera llamada a {@code WebDriverManager.setup()}
 *       para evitar condiciones de carrera cuando múltiples hilos arrancan simultáneamente
 *       y el binario de chromedriver todavía no está en caché.</li>
 * </ul>
 *
 * <h3>Ciclo de vida por escenario</h3>
 * <ol>
 *   <li>{@code @Before} en {@link hooks.Hooks} → {@link #initDriver()} crea el driver.</li>
 *   <li>Steps del escenario acceden al driver vía {@link #getDriver()} a través de BasePage.</li>
 *   <li>{@code @After}  en {@link hooks.Hooks} → {@link #quitDriver()} cierra y limpia.</li>
 * </ol>
 *
 * <p>Esta clase no puede ser instanciada (constructor privado).
 */
public class DriverFactory {

    /**
     * Almacén de drivers indexado por hilo.
     * Cada hilo del pool de Cucumber tiene su propia entrada; no hay sincronización
     * necesaria al leer/escribir el driver dentro de un mismo hilo.
     */
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    /**
     * Monitor para serializar la llamada a {@code WebDriverManager.setup()}.
     *
     * <p>Cuando N hilos arrancan simultáneamente, todos llaman a {@code setup()} a la vez.
     * Si el binario no está en caché, varios intentarían descargarlo/verificarlo al mismo
     * tiempo → posibles corrupción del archivo o errores de red. El lock fuerza ejecución
     * secuencial solo en ese punto; una vez que el binario está en caché, las llamadas
     * subsiguientes son casi instantáneas (lectura del sistema de archivos).
     */
    private static final Object WDM_LOCK = new Object();

    /** Constructor privado: clase de utilidades estáticas, no instanciable. */
    private DriverFactory() {}

    // =====================================================================
    // CICLO DE VIDA DEL DRIVER
    // =====================================================================

    /**
     * Inicializa un nuevo ChromeDriver para el hilo actual si no existe ya uno.
     *
     * <p>El check {@code driverThreadLocal.get() == null} previene la creación de
     * múltiples drivers en el mismo hilo si {@code initDriver()} se llamara dos veces
     * por error (idempotente).
     *
     * <p><b>ChromeOptions aplicadas:</b>
     * <ul>
     *   <li>SSL: {@code setAcceptInsecureCerts} + flags → acepta certs autofirmados en
     *       navegación Y en XHR (llamadas al API backend con cert no confiable).</li>
     *   <li>Estabilidad: sin notificaciones, infobars ni sandbox.</li>
     *   <li>Inicio rápido: sin extensiones, sync, apps predeterminadas ni first-run
     *       → ahorra ~1-1.5 s por instancia de Chrome.</li>
     * </ul>
     */
    public static void initDriver() {

        if (driverThreadLocal.get() != null) return; // ya existe driver en este hilo

        // Usar ChromeDriver pre-instalado en el sistema (imagen Docker CI).
        // Evita que WebDriverManager descargue el binario desde internet en cada run
        // (la descarga puede tardar 15-20 min en runners con acceso limitado a internet).
        String chromedriverPath = System.getenv("CHROMEDRIVER_PATH");
        if (chromedriverPath != null && !chromedriverPath.isBlank()
                && new java.io.File(chromedriverPath).exists()) {
            System.setProperty("webdriver.chrome.driver", chromedriverPath);
        } else {
            // Fallback: WebDriverManager para entornos locales sin CHROMEDRIVER_PATH
            synchronized (WDM_LOCK) {
                WebDriverManager.chromedriver().setup();
            }
        }

        ChromeOptions options = new ChromeOptions();

        // ─── SSL / Certificados inseguros ─────────────────────────────────
        // setAcceptInsecureCerts(true): acepta certs inválidos en driver.get()
        // --ignore-certificate-errors: ignora errores de cert a nivel de proceso
        // --allow-insecure-localhost: específico para dominios localhost con cert inválido
        // --allow-running-insecure-content: acepta contenido mixto (XHR a API con bad cert)
        options.setAcceptInsecureCerts(true);
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--allow-insecure-localhost");
        options.addArguments("--allow-running-insecure-content");

        // ─── Estabilidad ──────────────────────────────────────────────────
        options.addArguments("--disable-notifications");  // sin popups de permisos
        // --disable-infobars eliminado: removido en Chrome 117+, causa InvalidArgumentException
        options.addArguments("--start-maximized");        // ventana maximizada = más elementos visibles
        options.addArguments("--disable-dev-shm-usage"); // evita crashes en /dev/shm limitado
        options.addArguments("--no-sandbox");             // necesario en entornos sin privilegios

        // ─── Inicio rápido (ahorra ~1-1.5 s por instancia de Chrome) ──────
        // Omite procesos de Chrome que no son necesarios para automatización:
        options.addArguments("--disable-extensions");              // no cargar extensiones
        options.addArguments("--no-first-run");                    // omitir wizard de primer inicio
        options.addArguments("--disable-default-apps");            // no instalar apps predeterminadas
        options.addArguments("--disable-sync");                    // sin sincronización de cuenta Google
        // Reduce flooding de mensajes IPC entre browser y renderer:
        // mejora la respuesta de sendKeys en formularios Angular bajo carga paralela.
        options.addArguments("--disable-ipc-flooding-protection");

        // ─── Directorio de descargas ──────────────────────────────────────
        // Redirige las descargas de Chrome al directorio build/downloads/ del proyecto.
        // Debe coincidir con BasePage.getDownloadDirectory() para que waitUntilFileDownloaded()
        // busque en el lugar correcto. Sin esta configuración, Chrome descarga al directorio
        // del sistema (~Downloads) y los métodos de verificación de archivos fallan siempre.
        String downloadDir = System.getProperty("user.dir") + "/build/downloads";
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadDir.replace("/", java.io.File.separator));
        prefs.put("download.prompt_for_download", false);       // sin diálogo de confirmación
        prefs.put("download.directory_upgrade", true);          // crear directorio si no existe
        prefs.put("safebrowsing.enabled", true);                // evita bloqueos de seguridad en descargas
        prefs.put("plugins.always_open_pdf_externally", true);  // descargar PDFs en lugar de abrirlos
        options.setExperimentalOption("prefs", prefs);

        // ─── Headless: automático en CI, opcional en local ────────────────
        // GitLab CI siempre define la variable de entorno CI=true.
        // En local se puede forzar con: set BROWSER_HEADLESS=true (Windows)
        //                                export BROWSER_HEADLESS=true (Linux/Mac)
        //
        // Headless reduce ~30-40% RAM/CPU por instancia (200-300 MB vs 400-600 MB).
        // Obligatorio para parallelism >= 4 en runners sin pantalla o con RAM limitada.
        // Prioridad de activación headless (de mayor a menor):
        // 1. CI=true  → GitLab define esto automáticamente en todos sus jobs.
        // 2. BROWSER_HEADLESS=true → override manual en local o scripts.
        // 3. browser.headless=true en config.properties → configuración por proyecto.
        boolean headless = "true".equalsIgnoreCase(System.getenv("CI"))
                || "true".equalsIgnoreCase(System.getenv("BROWSER_HEADLESS"))
                || ConfigManager.isHeadless();
        if (headless) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
        }

        ChromeDriver driver = new ChromeDriver(options);

        // ─── Page load timeout explícito ──────────────────────────────────
        // Chrome default = 300 s (demasiado largo). Con 15 s fallamos rápido
        // si la URL no es accesible desde el runner (red interna no alcanzable).
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(15));

        driverThreadLocal.set(driver);
    }

    /**
     * Devuelve el WebDriver del hilo actual.
     *
     * <p>Retorna {@code null} si {@link #initDriver()} no fue llamado aún para este
     * hilo (situación normal antes del {@code @Before} del primer escenario).
     *
     * @return driver del hilo actual, o {@code null} si no fue inicializado
     */
    public static WebDriver getDriver() {
        return driverThreadLocal.get();
    }

    /**
     * Cierra el ChromeDriver del hilo actual y limpia el ThreadLocal.
     *
     * <p>El bloque {@code finally} garantiza que el ThreadLocal siempre se limpia,
     * incluso si {@code driver.quit()} lanza excepción (por ejemplo, si Chrome
     * se cerró externamente o crasheó). Sin la limpieza, el siguiente escenario
     * en el mismo hilo encontraría un driver muerto y fallaría con un error
     * confuso en lugar de crear uno nuevo.
     */
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) return;

        try {
            driver.quit();
        } catch (Exception ignored) {
            // Chrome puede haber terminado prematuramente (crash, kill externo).
            // Ignorar el error de comunicación con el proceso ya muerto.
        } finally {
            // Limpiar siempre: el próximo initDriver() en este hilo creará uno nuevo.
            driverThreadLocal.remove();
        }
    }
}
