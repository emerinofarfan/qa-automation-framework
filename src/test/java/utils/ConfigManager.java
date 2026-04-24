package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Lector centralizado de configuración del framework.
 *
 * <p>Lee {@code config.properties} desde el classpath de test
 * ({@code src/test/resources/}) al arrancar la JVM (bloque {@code static}).
 * Si el archivo no se encuentra o no se puede leer, cada método devuelve
 * un valor por defecto hardcodeado para que el framework funcione sin
 * necesidad de configuración explícita.
 *
 * <h3>Propiedades disponibles</h3>
 * <table border="1">
 *   <tr><th>Clave</th><th>Tipo</th><th>Por defecto</th><th>Descripción</th></tr>
 *   <tr><td>base.url</td><td>String</td><td>URL de auth TEST</td><td>URL de la aplicación web</td></tr>
 *   <tr><td>api.ssl.url</td><td>String</td><td>URL API SSL TEST</td><td>URL del endpoint de warmup SSL</td></tr>
 *   <tr><td>timeout.default</td><td>int</td><td>15</td><td>Timeout estándar (segundos)</td></tr>
 *   <tr><td>timeout.fast</td><td>int</td><td>5</td><td>Timeout corto para condiciones rápidas</td></tr>
 *   <tr><td>timeout.slow</td><td>int</td><td>30</td><td>Timeout largo para operaciones pesadas</td></tr>
 *   <tr><td>warmup.timeout</td><td>int</td><td>8</td><td>Límite del proceso de aceptación SSL</td></tr>
 *   <tr><td>browser.headless</td><td>boolean</td><td>false</td><td>Modo headless del browser</td></tr>
 * </table>
 *
 * <p>Esta clase no puede instanciarse (constructor privado, solo métodos estáticos).
 */
public class ConfigManager {

    private static final Properties PROPS = new Properties();

    /**
     * Nombre del archivo de configuración buscado en el classpath de test.
     */
    private static final String CONFIG_FILE = "config.properties";

    static {
        try (InputStream is = ConfigManager.class.getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                PROPS.load(is);
            }
            // Si el archivo no existe, PROPS queda vacío y cada getter usa su default.
        } catch (IOException e) {
            // No propagamos: el framework debe seguir funcionando con valores por defecto.
            System.err.println("[ConfigManager] No se pudo cargar " + CONFIG_FILE
                    + ": " + e.getMessage() + " — usando valores por defecto.");
        }
    }

    /** Constructor privado: clase de utilidades estáticas, no instanciable. */
    private ConfigManager() {}

    // =====================================================================
    // URLS
    // =====================================================================

    /**
     * URL principal de la aplicación web (pantalla de login).
     *
     * <p>Resuelve por capas de prioridad:
     * <ol>
     *   <li>Variable de entorno {@code TEST_BASE_URL} — recomendado para CI/CD.</li>
     *   <li>Propiedad del sistema {@code -Dbase.url=} — ejecución local por terminal.</li>
     *   <li>Clave {@code base.url} en {@code config.properties} — fallback local.</li>
     * </ol>
     *
     * @return URL de la aplicación; URL del entorno TEST si ninguna fuente la provee
     */
    public static String getBaseUrl() {
        String value = getCredential("TEST_BASE_URL", "base.url", "base.url");
        return value.isBlank() ? "https://example.local/auth" : value;
    }

    /**
     * URL del endpoint de la API backend usado para aceptar su certificado SSL.
     *
     * @return valor de {@code api.ssl.url}, o la URL del entorno TEST como fallback
     */
    public static String getApiSslUrl() {
        return PROPS.getProperty("api.ssl.url", "https://example.local/api/auth");
    }

    /**
     * URL de la aplicación de verificación Luhn (app piloto de automatización).
     *
     * <p>Resuelve por capas de prioridad:
     * <ol>
     *   <li>Variable de entorno {@code LUHN_BASE_URL} — recomendado para CI/CD.</li>
     *   <li>Propiedad del sistema {@code -Dluhn.url=} — ejecución local por terminal.</li>
     *   <li>Clave {@code luhn.url} en {@code config.properties} — fallback local.</li>
     * </ol>
     *
     * @return URL de la app Luhn; URL interna de desarrollo como fallback
     */
    public static String getLuhnUrl() {
        String value = getCredential("LUHN_BASE_URL", "luhn.url", "luhn.url");
        // Validar que el valor sea una URL real (comienza con http).
        // Si la variable de GitLab CI no está definida, YAML deja el literal
        // "${LUHN_BASE_URL}" como valor — no es una URL válida, usar fallback.
        if (value.isBlank() || !value.startsWith("http")) {
            return "https://portaldesar:8444/luhn-verification/";
        }
        return value;
    }

    // =====================================================================
    // TIMEOUTS
    // =====================================================================

    /**
     * Timeout estándar para esperas de UI normales (segundos).
     *
     * @return valor de {@code timeout.default}, o 15 como fallback
     */
    public static int getDefaultTimeout() {
        return parseIntSafe("timeout.default", 15);
    }

    /**
     * Timeout corto para condiciones que se cumplen casi inmediatamente (segundos).
     *
     * @return valor de {@code timeout.fast}, o 5 como fallback
     */
    public static int getFastTimeout() {
        return parseIntSafe("timeout.fast", 5);
    }

    /**
     * Timeout largo para operaciones pesadas como guardado o carga inicial (segundos).
     *
     * @return valor de {@code timeout.slow}, o 30 como fallback
     */
    public static int getSlowTimeout() {
        return parseIntSafe("timeout.slow", 30);
    }

    /**
     * Límite de tiempo esperado para el proceso de aceptación del certificado SSL (segundos).
     *
     * <p>El warmup abre la URL del API en el browser y acepta el cert autofirmado.
     * Con la secuencia "Avanzado → Continuar", el proceso completa en ≤ 8 s en condiciones normales.
     *
     * @return valor de {@code warmup.timeout}, o 8 como fallback
     */
    public static int getWarmupTimeout() {
        return parseIntSafe("warmup.timeout", 8);
    }

    // =====================================================================
    // BROWSER
    // =====================================================================

    /**
     * Indica si el browser debe arrancar en modo headless.
     *
     * <p>{@code true} recomendado para CI/CD o ejecución paralela con &ge;4 hilos:
     * reduce ~30% de RAM/CPU por instancia y elimina la competencia por recursos gráficos.
     * {@code false} para depuración local con ventana visible.
     *
     * @return valor de {@code browser.headless}, o {@code false} como fallback
     */
    public static boolean isHeadless() {
        return Boolean.parseBoolean(PROPS.getProperty("browser.headless", "false"));
    }

    // =====================================================================
    // UTILIDAD INTERNA
    // =====================================================================

    /**
     * Parsea un entero de las propiedades con fallback ante valor nulo o mal formado.
     *
     * @param key          clave de la propiedad
     * @param defaultValue valor devuelto si la clave no existe o no es un entero válido
     * @return el valor entero de la propiedad, o {@code defaultValue}
     */
    private static int parseIntSafe(String key, int defaultValue) {
        String value = PROPS.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("[ConfigManager] Valor inválido para '" + key + "': '"
                    + value + "' — usando " + defaultValue);
            return defaultValue;
        }
    }

    // =====================================================================
    // CREDENCIALES DE TEST
    // =====================================================================

    /**
     * Usuario de prueba resuelto por capas de prioridad:
     * <ol>
     *   <li>Variable de entorno {@code TEST_USERNAME} — recomendado para CI/CD.</li>
     *   <li>Propiedad del sistema {@code -Dtest.username=} — ejecución local por terminal.</li>
     *   <li>Clave {@code test.username} en {@code config.properties} — solo fallback local;
     *       ese archivo NO debe contener credenciales reales en repositorios compartidos.</li>
     * </ol>
     *
     * @return nombre de usuario; cadena vacía si ninguna fuente lo provee
     */
    public static String getTestUsername() {
        return getCredential("TEST_USERNAME", "test.username", "test.username");
    }

    /**
     * Contraseña de prueba resuelta por capas de prioridad:
     * <ol>
     *   <li>Variable de entorno {@code TEST_PASSWORD} — recomendado para CI/CD.</li>
     *   <li>Propiedad del sistema {@code -Dtest.password=} — ejecución local por terminal.</li>
     *   <li>Clave {@code test.password} en {@code config.properties} — solo fallback local;
     *       ese archivo NUNCA debe commitearse con credenciales reales.</li>
     * </ol>
     *
     * @return contraseña; cadena vacía si ninguna fuente lo provee
     */
    public static String getTestPassword() {
        return getCredential("TEST_PASSWORD", "test.password", "test.password");
    }

    /**
     * Lee una credencial aplicando prioridad:
     * variable de entorno → propiedad del sistema JVM → {@code config.properties}.
     *
     * @param envVar    nombre de la variable de entorno del SO
     * @param sysProp   nombre de la propiedad del sistema JVM ({@code -D...})
     * @param configKey clave en {@code config.properties}
     * @return valor encontrado, o cadena vacía si ninguna fuente lo provee
     */
    private static String getCredential(String envVar, String sysProp, String configKey) {
        String value = System.getenv(envVar);
        if (value != null && !value.isBlank()) return value;
        value = System.getProperty(sysProp);
        if (value != null && !value.isBlank()) return value;
        return PROPS.getProperty(configKey, "");
    }
}
