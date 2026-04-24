/*
 * Copyright (c) 2026 Caja Piura - Automatización QA
 * Este código es confidencial y está protegido.
 */
package steps;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import pages.LoginPage;
import utils.ConfigManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Steps de Cucumber para la funcionalidad de autenticación — Demo del framework.
 *
 * <h3>Reglas de diseño de steps (aplica a toda la suite)</h3>
 * <ol>
 *   <li><b>Un step = una acción o una verificación.</b> Si un step hace dos
 *       cosas, divídelo en dos.</li>
 *   <li><b>Los steps no conocen Selenium.</b> Toda interacción va al Page Object.
 *       El step solo orquesta llamadas al PO y afirma resultados.</li>
 *   <li><b>Usa AssertJ para afirmaciones</b> ({@code assertThat(...).isTrue()}).
 *       Más legible que JUnit assertions y produce mensajes de error claros.</li>
 *   <li><b>Parámetros en los steps = datos del negocio,</b> no selectores de UI
 *       (ej. "usuario_invalido", no "By.id('user')").</li>
 * </ol>
 *
 * <h3>Cómo copiar este patrón</h3>
 * <ol>
 *   <li>Crea {@code steps/MiFuncionalidadSteps.java} siguiendo esta estructura.</li>
 *   <li>Instancia tu Page Object en el campo de instancia (no en {@code @Before}).</li>
 *   <li>Cada método anotado con {@code @Dado}, {@code @Cuando}, {@code @Entonces}
 *       corresponde exactamente a una línea del feature.</li>
 *   <li>Ver: {@code docs/03-templates/STEP_DEFINITION_TEMPLATE.md}</li>
 * </ol>
 */
public class DemoLoginSteps {

    /**
     * Page Object de login.
     * Se instancia una vez por escenario gracias al ciclo de vida de Cucumber:
     * cada escenario crea una nueva instancia de este steps class.
     */
    private final LoginPage loginPage = new LoginPage();

    // =========================================================================
    // GIVEN — precondiciones
    // =========================================================================

    /**
     * Navega al portal de la aplicación usando la URL configurada en config.properties
     * o en la variable de entorno {@code TEST_BASE_URL}.
     */
    @Dado("el usuario navega al portal de la aplicación")
    public void elUsuarioNavegaAlPortal() {
        loginPage.navigateTo(ConfigManager.getBaseUrl());
    }

    // =========================================================================
    // WHEN — acciones del usuario
    // =========================================================================

    /**
     * Ingresa las credenciales del ambiente de prueba.
     * Las credenciales se leen desde variables de entorno o config.properties
     * (nunca hardcodeadas aquí). Ver {@link ConfigManager#getTestUsername()}.
     */
    @Cuando("ingresa usuario y clave válidos del ambiente de prueba")
    public void ingresaCredencialesValidas() {
        loginPage.ingresarUsuario(ConfigManager.getTestUsername());
        loginPage.ingresarClave(ConfigManager.getTestPassword());
    }

    /**
     * Ingresa credenciales específicas (para escenarios negativos o parametrizados).
     *
     * @param usuario nombre de usuario a ingresar
     * @param clave   contraseña a ingresar
     */
    @Cuando("ingresa el usuario {string} y la clave {string}")
    public void ingresaCredencialesEspecificas(String usuario, String clave) {
        loginPage.ingresarUsuario(usuario);
        loginPage.ingresarClave(clave);
    }

    /**
     * Hace clic en el botón de ingresar para enviar el formulario.
     */
    @Cuando("hace clic en el botón de ingresar")
    public void haceClicEnBotonIngresar() {
        loginPage.presionarBotonIngresar();
    }

    // =========================================================================
    // THEN — verificaciones
    // =========================================================================

    /**
     * Verifica que el panel principal del sistema es visible tras el login exitoso.
     */
    @Entonces("debe visualizarse el panel principal del sistema")
    public void debeVisualizarsePanelPrincipal() {
        assertThat(loginPage.panelPrincipalVisible())
                .as("El panel principal del sistema debe ser visible tras el login exitoso")
                .isTrue();
    }

    /**
     * Verifica que el mensaje de error de autenticación es visible tras credenciales incorrectas.
     */
    @Entonces("debe mostrarse un mensaje de error de autenticación")
    public void debeMostrarseErrorAutenticacion() {
        assertThat(loginPage.mensajeErrorVisible())
                .as("Debe mostrarse un mensaje de error cuando las credenciales son incorrectas")
                .isTrue();
    }
}
