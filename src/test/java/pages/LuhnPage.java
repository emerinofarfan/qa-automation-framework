/*
 * Copyright (c) 2026 Caja Piura - Automatización QA
 * Este código es confidencial y está protegido.
 */
package pages;

import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Page Object de la aplicación de verificación Luhn (app piloto de automatización).
 *
 * <p>La app expone dos funciones principales:
 * <ol>
 *   <li><b>Calcular dígito verificador</b>: el usuario ingresa un número base (sin
 *       dígito verificador) y la app calcula cuál debe ser el dígito final.</li>
 *   <li><b>Validar número completo</b>: el usuario ingresa un número completo y la
 *       app indica si es válido o inválido según el algoritmo de Luhn.</li>
 * </ol>
 *
 * <h3>Datos de prueba Luhn estándar (RFC)</h3>
 * <ul>
 *   <li>Número base: {@code 7992739871} → dígito verificador: {@code 3}</li>
 *   <li>Número completo válido: {@code 79927398713}</li>
 *   <li>Número completo inválido: {@code 79927398710}</li>
 * </ul>
 */
public class LuhnPage extends BasePage {

    private static final Logger log = LoggerFactory.getLogger(LuhnPage.class);

    // =========================================================================
    // LOCATORS — sección "Calcular dígito verificador"
    // =========================================================================

    /** Campo de entrada del número base (sin dígito verificador). */
    private static final By INPUT_NUMERO_CALC = By.cssSelector("input[type='text']");

    /**
     * Botón que dispara el cálculo del dígito verificador.
     * Usa contains() para tolerar variantes con/sin tilde ("Digito" / "Dígito").
     */
    private static final By BOTON_CALCULAR =
            By.xpath("//button[contains(., 'Digito') or contains(., 'Dígito')]");

    /**
     * Span que muestra el dígito verificador calculado.
     * Estrategia: hermano del span con texto "Dígito de verificación" dentro del mismo div.
     * El span del dígito tiene display:inline-flex y fondo gradiente (sin id ni clase).
     */
    private static final By RESULTADO_DIGITO =
            By.xpath("//span[normalize-space()='Dígito de verificación']/following-sibling::span");

    // =========================================================================
    // LOCATORS — sección "Validar número completo"
    // =========================================================================

    /** Tab "Validar número" — hay que hacer clic aquí para cambiar de sección. */
    private static final By TAB_VALIDAR = By.xpath("//button[normalize-space()='Validar número']");

    /**
     * Campo de entrada del número completo (con dígito verificador).
     * El tab "Validar número" muestra un único input[type='text']; mismo selector que en "Calcular".
     */
    private static final By INPUT_NUMERO_VALIDAR = By.cssSelector("input[type='text']");

    /** Botón de acción que ejecuta la validación del número completo. */
    private static final By BOTON_VALIDAR = By.xpath("//button[normalize-space()='Validar']");

    /**
     * Span que muestra el resultado de la validación ("✓ Válido" o "✗ Inválido").
     * Estrategia: hermano del span con texto "Resultado" dentro del mismo div de resultados.
     * El span del resultado tiene border-radius:9999px (pill badge), sin id ni clase.
     */
    private static final By RESULTADO_VALIDACION =
            By.xpath("//span[normalize-space()='Resultado']/following-sibling::span");

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    /** Crea instancia de LuhnPage e inicializa driver y waits desde {@link BasePage}. */
    public LuhnPage() {
        super();
    }

    // =========================================================================
    // ACCIONES — sección calcular
    // =========================================================================

    /**
     * Ingresa el número base en el campo de cálculo del dígito verificador.
     *
     * @param numeroParcial número sin dígito verificador (ej. "7992739871")
     */
    public void ingresarNumeroParcial(String numeroParcial) {
        log.debug("Ingresando número parcial para calcular: {}", numeroParcial);
        write(INPUT_NUMERO_CALC, numeroParcial);
    }

    /** Hace clic en el botón "Calcular" para obtener el dígito verificador. */
    public void presionarCalcular() {
        log.debug("Presionando botón Calcular");
        clickElement(BOTON_CALCULAR);
    }

    // =========================================================================
    // ACCIONES — sección validar
    // =========================================================================

    /**
     * Cambia al tab "Validar número" e ingresa el número completo en el campo de validación.
     *
     * <p>La app abre en el tab "Calcular" por defecto; es necesario hacer clic
     * en el tab "Validar número" antes de interactuar con su formulario.
     *
     * @param numeroCompleto número con dígito verificador incluido (ej. "79927398713")
     */
    public void ingresarNumeroCompleto(String numeroCompleto) {
        log.debug("Abriendo tab Validar número");
        clickElement(TAB_VALIDAR);
        log.debug("Ingresando número completo para validar: {}", numeroCompleto);
        write(INPUT_NUMERO_VALIDAR, numeroCompleto);
    }

    /** Hace clic en el botón "Validar" para obtener el resultado de validación. */
    public void presionarValidar() {
        log.debug("Presionando botón Validar");
        clickElement(BOTON_VALIDAR);
    }

    // =========================================================================
    // VERIFICACIONES
    // =========================================================================

    /**
     * Retorna el texto del dígito verificador calculado.
     *
     * @return texto del elemento resultado (ej. "3"); vacío si aún no aparece
     */
    public String obtenerDigitoCalculado() {
        String texto = getText(RESULTADO_DIGITO).trim();
        log.debug("Dígito verificador obtenido: '{}'", texto);
        return texto;
    }

    /**
     * Verifica si el resultado de validación indica que el número es válido.
     *
     * @return {@code true} si el texto del resultado contiene "válido" (sin distinguir mayúsculas
     *         y sin considerar si también contiene "inválido")
     */
    public boolean esNumeroValido() {
        String texto = getText(RESULTADO_VALIDACION).toLowerCase().trim();
        log.debug("Texto resultado validación: '{}'", texto);
        // "válido" o "numero válido", pero NO "inválido"
        return texto.contains("válido") && !texto.contains("inválido");
    }
}
