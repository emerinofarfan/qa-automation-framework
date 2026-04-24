package utils;

import org.openqa.selenium.By;

/**
 * Excepción de automatización con contexto estructurado.
 *
 * <p>Reemplaza a los {@link RuntimeException} genéricos en toda la capa de pages,
 * proporcionando mensajes que indican claramente qué acción falló, en qué parámetro
 * y con qué locator, sin necesidad de inspeccionar el stack trace completo.
 *
 * <p>Uso típico:
 * <pre>
 *   throw AutomationException.timeout("esperarModal", "Parámetro de Subproducto", btnOkSwal, e);
 *   throw AutomationException.elementNotFound("configurarRadio", "180 días", locator, e);
 * </pre>
 */
public class AutomationException extends RuntimeException {

    private final String accion;
    private final String contexto;
    private final String locatorStr;

    private AutomationException(String message, String accion, String contexto,
                                 String locatorStr, Throwable cause) {
        super(message, cause);
        this.accion     = accion;
        this.contexto   = contexto;
        this.locatorStr = locatorStr;
    }

    // =====================================================================
    // FACTORY METHODS
    // =====================================================================

    /** Fallo por timeout esperando un elemento o condición. */
    public static AutomationException timeout(String accion, String contexto,
                                              By locator, Throwable cause) {
        String msg = String.format(
            "[TIMEOUT] %s en '%s'%n  Locator : %s%n  Causa   : %s",
            accion, contexto, locator, rootMessage(cause));
        return new AutomationException(msg, accion, contexto, locator.toString(), cause);
    }

    /** Fallo por timeout sin locator específico (e.g. esperar URL, modal, etc.). */
    public static AutomationException timeout(String accion, String contexto, Throwable cause) {
        String msg = String.format(
            "[TIMEOUT] %s en '%s'%n  Causa   : %s",
            accion, contexto, rootMessage(cause));
        return new AutomationException(msg, accion, contexto, null, cause);
    }

    /** Elemento no encontrado en el DOM dentro del tiempo de espera. */
    public static AutomationException elementNotFound(String accion, String contexto,
                                                       By locator, Throwable cause) {
        String msg = String.format(
            "[NO ENCONTRADO] %s en '%s'%n  Locator : %s%n  Causa   : %s",
            accion, contexto, locator, rootMessage(cause));
        return new AutomationException(msg, accion, contexto, locator.toString(), cause);
    }

    /** Click fallido de forma persistente tras agotar reintentos. */
    public static AutomationException clickFailed(String contexto, By locator, Throwable cause) {
        String msg = String.format(
            "[CLICK FALLIDO] '%s'%n  Locator : %s%n  Causa   : %s",
            contexto, locator, rootMessage(cause));
        return new AutomationException(msg, "click", contexto, locator.toString(), cause);
    }

    /**
     * Panel expansible no se abrió dentro del tiempo de espera.
     *
     * <p>Usar cuando un panel o sección expandible no termina de abrirse tras
     * el click de apertura (ej: acordeones, paneles de detalle, sidebars).
     *
     * @param parametro nombre del panel o sección que no respondió
     * @param cause     excepción original (TimeoutException u otra)
     * @return excepción con mensaje descriptivo para el reporte
     */
    public static AutomationException panelNotOpen(String parametro, Throwable cause) {
        String msg = String.format(
            "[PANEL NO ABIERTO] El panel '%s' no respondió dentro del tiempo de espera.%n" +
            "  Verifica que el elemento de apertura haya sido clickeado y que la " +
            "animación de expansión haya terminado.%n  Causa: %s",
            parametro, rootMessage(cause));
        return new AutomationException(msg, "esperarPanelAbierto", parametro, null, cause);
    }

    /** Formato de datos inválido en el feature (ej. falta '>' en subproducto). */
    public static AutomationException invalidFormat(String parametro, String valor,
                                                     String formatoEsperado) {
        String msg = String.format(
            "[FORMATO INVALIDO] Parámetro '%s': valor='%s'%n  Formato esperado: %s",
            parametro, valor, formatoEsperado);
        return new AutomationException(msg, "parsearDato", parametro, null, null);
    }

    // =====================================================================
    // ACCESORES (útiles para logging en Hooks)
    // =====================================================================

    public String getAccion()     { return accion; }
    public String getContexto()   { return contexto; }
    public String getLocatorStr() { return locatorStr; }

    // =====================================================================
    // UTILIDAD INTERNA
    // =====================================================================

    private static String rootMessage(Throwable t) {
        if (t == null) return "desconocida";
        Throwable root = t;
        while (root.getCause() != null) root = root.getCause();
        String msg = root.getMessage();
        // Recortar mensajes de Selenium que son muy largos
        return (msg != null && msg.length() > 120) ? msg.substring(0, 120) + "…" : msg;
    }
}
