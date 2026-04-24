/*
 * Copyright (c) 2026 Caja Piura - Automatización QA
 * Este código es confidencial y está protegido.
 */
package steps;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import pages.LuhnPage;
import utils.ConfigManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Steps de Cucumber para la feature de verificación Luhn.
 *
 * <p>Orquesta las acciones de {@link LuhnPage} sin tocar Selenium directamente.
 * Sigue el mismo patrón que {@code DemoLoginSteps}: instancia única del PO
 * por escenario, parámetros del negocio, afirmaciones con AssertJ.
 *
 * <h3>App piloto</h3>
 * <p>Los tests apuntan a la URL configurada en {@link ConfigManager#getLuhnUrl()}.
 * En CI/CD configurar la variable {@code LUHN_BASE_URL} en GitLab CI/CD Settings.
 * En local: ajustar {@code luhn.url} en {@code config.properties}.
 */
public class LuhnVerificacionSteps {

    /** Page Object de la calculadora Luhn. */
    private final LuhnPage luhnPage = new LuhnPage();

    // =========================================================================
    // GIVEN — precondiciones
    // =========================================================================

    /**
     * Navega a la URL de la aplicación de verificación Luhn.
     * La URL se resuelve desde {@link ConfigManager#getLuhnUrl()}.
     */
    @Dado("el usuario accede a la calculadora Luhn")
    public void elUsuarioAccedeALaCalculadoraLuhn() {
        luhnPage.navigateTo(ConfigManager.getLuhnUrl());
    }

    // =========================================================================
    // WHEN — acciones del usuario
    // =========================================================================

    /**
     * Ingresa el número base (sin dígito verificador) en el campo de cálculo.
     *
     * @param numeroParcial número sin el último dígito (ej. "7992739871")
     */
    @Cuando("ingresa el número {string} para calcular el dígito verificador")
    public void ingresaElNumeroParcialParaCalcular(String numeroParcial) {
        luhnPage.ingresarNumeroParcial(numeroParcial);
    }

    /** Presiona el botón "Calcular" para obtener el dígito verificador. */
    @Y("presiona el botón calcular")
    public void presionaElBotonCalcular() {
        luhnPage.presionarCalcular();
    }

    /**
     * Ingresa el número completo (con dígito verificador) en el campo de validación.
     *
     * @param numeroCompleto número con dígito verificador (ej. "79927398713")
     */
    @Cuando("ingresa el número completo {string} para validar")
    public void ingresaElNumeroCompletoParaValidar(String numeroCompleto) {
        luhnPage.ingresarNumeroCompleto(numeroCompleto);
    }

    /** Presiona el botón "Validar" para verificar el número completo. */
    @Y("presiona el botón validar")
    public void presionaElBotonValidar() {
        luhnPage.presionarValidar();
    }

    // =========================================================================
    // THEN — verificaciones
    // =========================================================================

    /**
     * Verifica que el dígito verificador calculado coincide con el esperado.
     *
     * @param digitoEsperado dígito verificador esperado (ej. "3")
     */
    @Entonces("el dígito verificador calculado debe ser {string}")
    public void elDigitoVerificadorCalculadoDebeSer(String digitoEsperado) {
        assertThat(luhnPage.obtenerDigitoCalculado())
                .as("El dígito verificador calculado debe coincidir con el esperado según el algoritmo de Luhn")
                .isEqualTo(digitoEsperado);
    }

    /**
     * Verifica que el resultado de validación indica que el número es válido.
     */
    @Entonces("el número debe ser indicado como válido")
    public void elNumeroDebeSerIndicadoComoValido() {
        assertThat(luhnPage.esNumeroValido())
                .as("El número completo 79927398713 debe ser reconocido como válido por el algoritmo de Luhn")
                .isTrue();
    }
}
