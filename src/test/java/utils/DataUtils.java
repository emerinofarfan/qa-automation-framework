package utils;

import java.util.Arrays;
import java.util.List;

/**
 * Utilidades estáticas para transformar datos de las tablas Cucumber
 * en los tipos que necesitan las páginas y los modelos del proyecto.
 *
 * <p>Clase de utilidades pura: sin estado, sin instancias, todos los métodos son estáticos.
 * El constructor privado previene la creación accidental de instancias.
 */
public final class DataUtils {

    /** Previene instanciación: clase de utilidades estáticas. */
    private DataUtils() {}

    // =====================================================================
    // CONVERSIÓN DE DATOS
    // =====================================================================

    /**
     * Convierte una cadena CSV del feature file en una lista de strings limpia.
     *
     * <p>Maneja los valores "vacíos" que Cucumber introduce cuando una celda de la
     * tabla de ejemplos está sin datos: {@code null}, cadena vacía, {@code "null"}
     * literal y el guion {@code "-"} usado como convención en los features para
     * indicar "sin valor".
     *
     * <p>Ejemplo de uso en el feature:
     * <pre>
     *   | parametroServicio | 70 - CRED.PRENDARIO, 80 - CRED. PYME |
     *   | estadoCredito     | -                                     |  ← se convierte a null
     * </pre>
     *
     * @param value  cadena CSV separada por comas, o indicador de valor vacío
     * @return lista de strings con trim aplicado y elementos vacíos eliminados,
     *         o {@code null} si el valor indica ausencia de datos
     */
    public static List<String> parseList(String value) {

        if (value == null) return null;

        String cleaned = value.trim();

        // Valores vacíos por convención del feature file → null (sin configuración)
        if (cleaned.isEmpty()
                || cleaned.equalsIgnoreCase("null")
                || cleaned.equals("-")) {
            return null;
        }

        // Partir por coma, limpiar espacios y eliminar entradas vacías resultantes
        return Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
