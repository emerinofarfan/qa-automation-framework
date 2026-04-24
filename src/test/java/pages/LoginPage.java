/*
 * Copyright (c) 2026 Caja Piura - Automatización QA
 * Este código es confidencial y está protegido.
 */
package pages;

import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Page Object de la pantalla de autenticación del sistema.
 *
 * <p>Encapsula todos los locators y acciones de la página de login.
 * Los steps de Cucumber consumen únicamente los métodos públicos de esta clase;
 * nunca interactúan con Selenium directamente.
 *
 * <h3>Cómo adaptar esta clase a otro sistema</h3>
 * <ol>
 *   <li>Abre la aplicación en Chrome y presiona F12 (DevTools).</li>
 *   <li>Usa el inspector de elementos para obtener los atributos {@code id},
 *       {@code name} o {@code class} de cada campo.</li>
 *   <li>Reemplaza los locators de la sección LOCATORS por los del sistema real.</li>
 *   <li>Ajusta los métodos públicos si la interacción cambia
 *       (ej. el login usa un modal en lugar de página completa).</li>
 * </ol>
 *
 * <h3>Herencia</h3>
 * <p>Extiende {@link BasePage}, que provee {@code find()}, {@code findVisible()},
 * {@code clickElement()}, {@code write()}, {@code isElementVisible()} y más.
 * No reimplementes lo que ya está en BasePage.
 */
public class LoginPage extends BasePage {

    private static final Logger log = LoggerFactory.getLogger(LoginPage.class);

    // =========================================================================
    // LOCATORS — Reemplaza estos valores con los del sistema real
    //
    // Cómo obtenerlos:
    //   By.id("x")          → <input id="x">
    //   By.name("x")        → <input name="x">
    //   By.cssSelector("x") → cualquier selector CSS válido
    //   By.xpath("//x")     → cuando id/name/css no son suficientes (último recurso)
    //
    // Convención: una constante privada por elemento, en SCREAMING_SNAKE_CASE
    // =========================================================================

    /** Campo de texto para el nombre de usuario */
    private static final By CAMPO_USUARIO   = By.id("username");

    /** Campo de texto para la contraseña */
    private static final By CAMPO_CLAVE     = By.id("password");

    /** Botón que envía el formulario de login */
    private static final By BOTON_INGRESAR  = By.cssSelector("button[type='submit']");

    /**
     * Mensaje de error que aparece cuando las credenciales son incorrectas.
     * Puede ser un div con clase de alerta o un span de error; ajusta según el sistema.
     */
    private static final By MENSAJE_ERROR   = By.cssSelector(".alert-danger, .error-message");

    /**
     * Elemento que confirma que el usuario ingresó correctamente al sistema.
     * Puede ser un menú lateral, un header con el nombre del usuario,
     * un dashboard, etc. Elige el más estable y único de la pantalla principal.
     */
    private static final By PANEL_PRINCIPAL = By.cssSelector(".dashboard, main[role='main']");

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    /**
     * Crea una instancia de LoginPage.
     * Llama a {@code super()} para inicializar driver y waits desde {@link BasePage}.
     */
    public LoginPage() {
        super();
    }

    // =========================================================================
    // ACCIONES — un método por acción de usuario, con nombre descriptivo
    // =========================================================================

    /**
     * Escribe el nombre de usuario en el campo correspondiente.
     *
     * @param usuario nombre de usuario a ingresar
     */
    public void ingresarUsuario(String usuario) {
        log.debug("Ingresando usuario: {}", usuario);
        write(CAMPO_USUARIO, usuario);
    }

    /**
     * Escribe la contraseña en el campo correspondiente.
     *
     * <p>El valor nunca se loguea en texto plano; {@link utils.SensitiveDataMasker}
     * lo enmascarará automáticamente en los reportes de Allure.
     *
     * @param clave contraseña a ingresar
     */
    public void ingresarClave(String clave) {
        log.debug("Ingresando clave: [ENMASCARADA]");
        write(CAMPO_CLAVE, clave);
    }

    /**
     * Presiona el botón de ingresar para enviar el formulario de autenticación.
     */
    public void presionarBotonIngresar() {
        log.debug("Haciendo clic en botón de ingresar");
        clickElement(BOTON_INGRESAR);
    }

    // =========================================================================
    // VERIFICACIONES — retornan boolean o el texto para que el step afirme
    // =========================================================================

    /**
     * Verifica que el panel principal del sistema es visible después del login.
     *
     * @return {@code true} si el panel principal está visible
     */
    public boolean panelPrincipalVisible() {
        return isElementVisible(PANEL_PRINCIPAL);
    }

    /**
     * Verifica que el mensaje de error de autenticación es visible.
     *
     * @return {@code true} si el mensaje de error está visible
     */
    public boolean mensajeErrorVisible() {
        return isElementVisible(MENSAJE_ERROR);
    }
}
