package hooks;

import io.cucumber.java.After;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.LoggerFactory;
import utils.DriverFactory;
import utils.SensitiveDataMasker;
import utils.TestExecutionSummary;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Hooks de Cucumber — ciclo de vida del WebDriver y captura de evidencia.
 *
 * <p>Se ejecuta automáticamente antes/después de cada escenario porque el
 * paquete {@code hooks} está declarado en el {@code glue} del Runner.
 *
 * <h3>Flujo por escenario</h3>
 * <ol>
 *   <li>{@link #setUp}: inicializa driver → navega al login.</li>
 *   <li>Cucumber ejecuta los steps del escenario.</li>
 *   <li>{@link #tearDown}: captura evidencia → cierra driver.</li>
 * </ol>
 *
 * <h3>⚠️ Warmup SSL Deprecado (Marzo 2026)</h3>
 * <p>El código de warmup SSL ha sido <b>deshabilitado</b> porque la infraestructura
 * de QA ya no usa certificados autofirmados. Esto mejora la performance eliminando
 * ~4 segundos de overhead por test.
 *
 * <p>Si en el futuro se necesita nuevamente, el código está comentado y puede
 * habilitarse mediante feature flag en configuración.
 */
public class Hooks {

    // DEPRECADO - Marzo 2026: La web ya no usa certificados inseguros
    // private static final String URL_API_SSL = ConfigManager.getApiSslUrl();

    // =====================================================================
    // SETUP — Antes de cada escenario
    // =====================================================================

    /**
     * Inicializa el WebDriver y navega a la URL de login.
     *
     * <p>También etiqueta el escenario en Allure con feature y severidad para
     * facilitar el análisis de resultados por categoría.
     *
     * <p><b>⚠️ Cambio Marzo 2026:</b> Se eliminó el warmup SSL del API porque
     * la infraestructura de QA ya no usa certificados autofirmados. Esto mejora
     * la performance eliminando ~4 segundos de overhead por test.
     *
     * <p><b>Mejora Marzo 2026:</b> La sincronización de WebDriver se gestiona
     * exclusivamente en {@link utils.DriverFactory} mediante {@code WDM_LOCK},
     * que serializa solo la descarga del binario de ChromeDriver. No hay
     * Thread.sleep en el ciclo de vida de los escenarios.
     */
    @Before(order = 0)
    public void setUp(Scenario scenario) {
        // ── Metadatos en Allure ──────────────────────────────────────────────
        etiquetarEnAllure(scenario);

        // ── Inicializar WebDriver ────────────────────────────────────────────
        // La sincronización de race conditions está en DriverFactory.WDM_LOCK.
        // NO usar synchronized ni Thread.sleep aquí: serializa todos los escenarios
        // y destruye el beneficio de parallelism=N.
        DriverFactory.initDriver();
        // La navegación a la URL inicial es responsabilidad de cada escenario
        // a través del step Background/Given ("el usuario navega al portal",
        // "el usuario accede a la calculadora Luhn", etc.).
        // Esto permite que distintas features apunten a distintas URLs
        // sin depender de una única base.url global.
    }

    // =====================================================================
    // TEARDOWN — Después de cada escenario
    // =====================================================================

    /**
     * Captura evidencia y cierra el driver.
     *
     * <ul>
     *   <li>En <b>fallos</b>: adjunta screenshot + URL al reporte Allure.</li>
     *   <li>En <b>éxitos</b>: adjunta screenshot final como constancia.</li>
     * </ul>
     *
     * <p>El {@code finally} garantiza que el driver siempre se cierra,
     * incluso si la captura lanza excepción.
     *
     * <p><b>Mejora Marzo 2026:</b> Se agregó limpieza de estado (cierre de modales)
     * antes de cerrar el driver. Esto evita que modales abiertos interfieran
     * con otros tests corriendo en paralelo.
     */
    @After(order = 0)
    public void tearDown(Scenario scenario) {
        try {
            capturarEvidencia(scenario);

            // ── Limpieza de estado bajo paralelización ────────────────────────
            // Cierra cualquier modal/popup abierto que pueda interferir con otros tests
            try {
                WebDriver driver = DriverFactory.getDriver();
                if (driver != null) {
                    // Enviar ESC para cerrar cualquier dialog/modal abierto.
                    // No usar Thread.sleep: driver.quit() al final limpia el estado.
                    new Actions(driver).sendKeys(Keys.ESCAPE).perform();
                }
            } catch (Exception e) {
                LoggerFactory.getLogger(Hooks.class)
                    .debug("Estado limpiado (o no había nada que limpiar)", e);
            }

            // Registrar resultado en TestExecutionSummary
            String featureName = extraerNombreFeature(scenario);
            if (scenario.isFailed()) {
                TestExecutionSummary.scenarioFailed(featureName, scenario.getName());
            } else if (scenario.getStatus().toString().equals("SKIPPED")) {
                TestExecutionSummary.scenarioSkipped(featureName, scenario.getName());
            } else {
                TestExecutionSummary.scenarioPassed(featureName, scenario.getName());
            }

        } finally {
            DriverFactory.quitDriver();
        }
    }

    /**
     * Extrae el nombre del feature del ID del escenario
     */
    private String extraerNombreFeature(Scenario scenario) {
        String[] partes = scenario.getId().split("[/:]");
        return partes.length > 1 ? partes[partes.length - 2].replace(".feature", "") : "unknown";
    }

    /**
     * Enmascara datos sensibles en parámetros del step actual en Allure.
     * Se ejecuta después de cada step para evitar exposición de secretos.
     */
    @AfterStep(order = 999)
    public void maskSensitiveDataInCurrentStep() {
        try {
            Allure.getLifecycle().updateStep(step ->
                    step.setParameters(SensitiveDataMasker.maskParameters(step.getParameters()))
            );
        } catch (RuntimeException ignored) {
            // Nunca afectar la ejecución funcional por metadata de reporte.
        }
    }

    // =====================================================================
    // WARMUP SSL — DEPRECADO (MARZO 2026)
    // =====================================================================
    // La infraestructura de QA ya NO usa certificados autofirmados.
    // Este código ha sido deshabilitado para eliminar overhead de ~4 segundos/test.
    // Si se necesita nuevamente, descomentar y habilitar mediante feature flag.
    // ══════════════════════════════════════════════════════════════════════════

    /*
    /**
     * Indica si el warmup SSL ya fue completado exitosamente en esta JVM.
     * {@code volatile} garantiza que la escritura en un hilo sea visible
     * inmediatamente a los demás hilos sin necesidad de entrar al bloque
     * {@code synchronized} en lecturas posteriores.
     */
    /*
    private static volatile boolean warmupDone = false;

    /**
     * Monitor que serializa el acceso al bloque de inicialización del warmup.
     * Solo el primer hilo que lo adquiere ejecuta el warmup; los demás esperan
     * y al entrar comprueban {@link #warmupDone} antes de volver a ejecutarlo.
     */
    /*
    private static final Object WARMUP_LOCK = new Object();

    /**
     * Executor de un único hilo daemon para encapsular la llamada al warmup
     * y forzar el timeout de {@link ConfigManager#getWarmupTimeout()} segundos.
     * Daemon = se destruye al terminar la JVM sin bloquear el shutdown.
     */
    /*
    private static final ExecutorService WARMUP_EXECUTOR =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "warmup-ssl");
                t.setDaemon(true);
                return t;
            });
    */

    // =====================================================================
    // MÉTODOS PRIVADOS
    // =====================================================================

    /**
     * Registra metadatos del escenario en el reporte Allure:
     * <ul>
     *   <li>{@code feature}: nombre del feature extraído del ID del escenario.</li>
     *   <li>{@code severity}: nivel asignado según los tags del escenario.</li>
     * </ul>
     */
    private void etiquetarEnAllure(Scenario scenario) {
        // El ID tiene formato "features/NombreFeature.feature:NombreEscenario"
        String[] partes = scenario.getId().split("[/:]");
        String feature  = partes.length > 1 ? partes[partes.length - 2] : "unknown";

        Allure.label("feature",  feature.replace(".feature", ""));
        Allure.label("severity", resolverSeveridad(scenario));
    }

    /**
     * Captura el estado visual del navegador y lo adjunta al reporte.
     * Maneja el caso en que el driver ya no esté disponible sin propagar error.
     */
    private void capturarEvidencia(Scenario scenario) {
        WebDriver driver = DriverFactory.getDriver();
        if (driver == null) return;

        try {
            byte[] screenshot = ((TakesScreenshot) driver)
                    .getScreenshotAs(OutputType.BYTES);

            String titulo = scenario.isFailed()
                    ? "Screenshot - FALLO: " + scenario.getName()
                    : "Screenshot - OK: "    + scenario.getName();

            // ── Allure ──────────────────────────────────────────────────────
            Allure.addAttachment(titulo, "image/png",
                    new ByteArrayInputStream(screenshot), "png");

            // ── Cucumber / ExtentReports ────────────────────────────────────
            scenario.attach(screenshot, "image/png", scenario.getName());

            // ── URL actual (útil para diagnosticar fallos de navegación) ────
            if (scenario.isFailed()) {
                String url = driver.getCurrentUrl();
                Allure.addAttachment("URL al fallo", "text/plain",
                        new ByteArrayInputStream(url.getBytes(StandardCharsets.UTF_8)), "txt");
                scenario.log("Fallo en URL: " + url);
            }

        } catch (Exception e) {
            // No propagar: el fallo real del escenario tiene prioridad
            registrarWarnAllure("No se pudo capturar evidencia: " + e.getMessage());
        }
    }

    /**
     * Asigna un nivel de severidad Allure según los tags del escenario.
     *
     * <table border="1">
     *   <tr><th>Tag</th><th>Severidad</th><th>Significado</th></tr>
     *   <tr><td>@Smoke</td><td>blocker</td><td>Crítico: bloquea el release</td></tr>
     *   <tr><td>@Regression</td><td>critical</td><td>Importante: regresión</td></tr>
     *   <tr><td>@Negativo</td><td>normal</td><td>Flujo de error esperado</td></tr>
     *   <tr><td>@UIValidation</td><td>minor</td><td>Validación de interfaz</td></tr>
     * </table>
     */
    private String resolverSeveridad(Scenario scenario) {
        for (String tag : scenario.getSourceTagNames()) {
            switch (tag.toLowerCase()) {
                case "@smoke":        return "blocker";
                case "@regression":   return "critical";
                case "@negativo":     return "normal";
                case "@uivalidation": return "minor";
                default:              break;
            }
        }
        return "normal";
    }

    /**
     * Adjunta un mensaje de advertencia al reporte Allure sin propagar excepción.
     *
     * @param mensaje texto descriptivo del aviso
     */
    private void registrarWarnAllure(String mensaje) {
        try {
            Allure.addAttachment("WARN - Hooks", "text/plain",
                    new ByteArrayInputStream(mensaje.getBytes(StandardCharsets.UTF_8)), "txt");
        } catch (Exception ignored) {}
    }
}
