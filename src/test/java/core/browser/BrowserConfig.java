package core.browser;

import java.time.Duration;

/**
 * Configuración inmutable para crear un IBrowserDriver.
 *
 * <p>Usa el patrón Builder para configuración fluida y legible.
 *
 * <p><b>Ejemplo de uso:</b>
 * <pre>
 * BrowserConfig config = BrowserConfig.builder()
 *     .browser(BrowserType.CHROME)
 *     .headless(true)
 *     .implicitWait(Duration.ofSeconds(10))
 *     .pageLoadTimeout(Duration.ofSeconds(30))
 *     .maximizeWindow(true)
 *     .build();
 * </pre>
 *
 * <p><b>Estado actual:</b> Esta clase es un contrato de diseño preparado para
 * una futura integración con {@link IBrowserDriver}. En la versión actual del
 * framework la configuración del driver se gestiona en {@link utils.DriverFactory}.
 *
 * @since 2.0.0 (diseño; integración con IBrowserDriver pendiente de migración)
 */
public final class BrowserConfig {

    private final BrowserType browser;
    private final boolean headless;
    private final Duration implicitWait;
    private final Duration explicitWait;
    private final Duration pageLoadTimeout;
    private final boolean maximizeWindow;
    private final boolean screenshotOnFailure;
    private final String downloadDirectory;
    private final boolean acceptInsecureCerts;
    private final String remoteUrl;

    private BrowserConfig(Builder builder) {
        this.browser = builder.browser;
        this.headless = builder.headless;
        this.implicitWait = builder.implicitWait;
        this.explicitWait = builder.explicitWait;
        this.pageLoadTimeout = builder.pageLoadTimeout;
        this.maximizeWindow = builder.maximizeWindow;
        this.screenshotOnFailure = builder.screenshotOnFailure;
        this.downloadDirectory = builder.downloadDirectory;
        this.acceptInsecureCerts = builder.acceptInsecureCerts;
        this.remoteUrl = builder.remoteUrl;
    }

    // Getters
    public BrowserType getBrowser() { return browser; }
    public boolean isHeadless() { return headless; }
    public Duration getImplicitWait() { return implicitWait; }
    public Duration getExplicitWait() { return explicitWait; }
    public Duration getPageLoadTimeout() { return pageLoadTimeout; }
    public boolean isMaximizeWindow() { return maximizeWindow; }
    public boolean isScreenshotOnFailure() { return screenshotOnFailure; }
    public String getDownloadDirectory() { return downloadDirectory; }
    public boolean isAcceptInsecureCerts() { return acceptInsecureCerts; }
    public String getRemoteUrl() { return remoteUrl; }
    public boolean isRemote() { return remoteUrl != null && !remoteUrl.isEmpty(); }

    /**
     * Crea un builder con valores por defecto.
     *
     * @return builder para configuración fluida
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder para crear instancias de BrowserConfig de forma fluida.
     */
    public static class Builder {
        private BrowserType browser = BrowserType.CHROME;
        private boolean headless = false;
        private Duration implicitWait = Duration.ofSeconds(10);
        private Duration explicitWait = Duration.ofSeconds(20);
        private Duration pageLoadTimeout = Duration.ofSeconds(30);
        private boolean maximizeWindow = true;
        private boolean screenshotOnFailure = true;
        private String downloadDirectory = System.getProperty("user.dir") + "/build/downloads";
        private boolean acceptInsecureCerts = true;
        private String remoteUrl = null;

        private Builder() {}

        /**
         * Establece el tipo de browser.
         *
         * @param browser tipo de browser (CHROME, FIREFOX, EDGE, SAFARI)
         * @return este builder para llamadas encadenadas
         */
        public Builder browser(BrowserType browser) {
            this.browser = browser;
            return this;
        }

        /**
         * Establece si el browser se ejecuta en modo headless.
         *
         * @param headless true para headless, false para UI visible
         * @return este builder
         */
        public Builder headless(boolean headless) {
            this.headless = headless;
            return this;
        }

        /**
         * Establece el implicit wait.
         *
         * <p>Tiempo que el driver espera automáticamente al buscar elementos.
         *
         * @param implicitWait duración del implicit wait
         * @return este builder
         */
        public Builder implicitWait(Duration implicitWait) {
            this.implicitWait = implicitWait;
            return this;
        }

        /**
         * Establece el explicit wait por defecto.
         *
         * <p>Tiempo máximo que se espera para condiciones explícitas.
         *
         * @param explicitWait duración del explicit wait
         * @return este builder
         */
        public Builder explicitWait(Duration explicitWait) {
            this.explicitWait = explicitWait;
            return this;
        }

        /**
         * Establece el page load timeout.
         *
         * <p>Tiempo máximo para cargar una página completa.
         *
         * @param pageLoadTimeout duración del timeout
         * @return este builder
         */
        public Builder pageLoadTimeout(Duration pageLoadTimeout) {
            this.pageLoadTimeout = pageLoadTimeout;
            return this;
        }

        /**
         * Establece si la ventana se maximiza al iniciar.
         *
         * @param maximizeWindow true para maximizar
         * @return este builder
         */
        public Builder maximizeWindow(boolean maximizeWindow) {
            this.maximizeWindow = maximizeWindow;
            return this;
        }

        /**
         * Establece si se captura screenshot automáticamente en fallos.
         *
         * @param screenshotOnFailure true para captura automática
         * @return este builder
         */
        public Builder screenshotOnFailure(boolean screenshotOnFailure) {
            this.screenshotOnFailure = screenshotOnFailure;
            return this;
        }

        /**
         * Establece el directorio de descargas.
         *
         * @param downloadDirectory ruta completa del directorio
         * @return este builder
         */
        public Builder downloadDirectory(String downloadDirectory) {
            this.downloadDirectory = downloadDirectory;
            return this;
        }

        /**
         * Establece si se aceptan certificados inseguros (self-signed).
         *
         * @param acceptInsecureCerts true para aceptar
         * @return este builder
         */
        public Builder acceptInsecureCerts(boolean acceptInsecureCerts) {
            this.acceptInsecureCerts = acceptInsecureCerts;
            return this;
        }

        /**
         * Establece la URL de Selenium Grid/RemoteWebDriver.
         *
         * <p>Si se configura, el driver se crea como RemoteWebDriver.
         *
         * @param remoteUrl URL del hub (ej: http://localhost:4444/wd/hub)
         * @return este builder
         */
        public Builder remoteUrl(String remoteUrl) {
            this.remoteUrl = remoteUrl;
            return this;
        }

        /**
         * Construye la instancia inmutable de BrowserConfig.
         *
         * @return configuración inmutable
         */
        public BrowserConfig build() {
            return new BrowserConfig(this);
        }
    }

    /**
     * Tipos de browser soportados.
     */
    public enum BrowserType {
        CHROME("chrome"),
        FIREFOX("firefox"),
        EDGE("edge"),
        SAFARI("safari");

        private final String value;

        BrowserType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static BrowserType fromString(String text) {
            for (BrowserType b : BrowserType.values()) {
                if (b.value.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unknown browser type: " + text);
        }
    }
}

