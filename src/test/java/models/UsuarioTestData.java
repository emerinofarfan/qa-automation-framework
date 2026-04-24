/*
 * Copyright (c) 2026 Caja Piura - Automatización QA
 * Este código es confidencial y está protegido.
 */
package models;

// =============================================================================
// MODELO DE DATOS DE PRUEBA — UsuarioTestData
//
// Propósito: transportar los datos de un usuario de prueba entre los steps
//            de un escenario sin pasar múltiples parámetros sueltos.
//
// ¿Cuándo crear un modelo en este paquete?
//   - Cuando 3 o más steps del mismo escenario comparten los mismos datos.
//   - Cuando una DataTable del feature tiene múltiples columnas relacionadas.
//   - Cuando los datos representan un objeto del dominio (usuario, cuenta,
//     transacción, etc.) que se reutiliza en varios features.
//
// ¿Cuándo NO crear un modelo?
//   - Para datos que solo usa 1 step → usa el parámetro directamente.
//   - Para configuración técnica → usa ConfigManager o constants.
//   - Para datos que vienen de la UI → usa getText() en el Page Object.
//
// Convención de nombres:
//   <NombreDelConcepto>TestData.java
//   Ejemplos: UsuarioTestData, TransferenciaTestData, CuentaTestData
// =============================================================================

/**
 * Objeto de datos para representar un usuario de prueba en los escenarios BDD.
 *
 * <p>Se utiliza para agrupar los atributos de un usuario que se comparte entre
 * varios steps de un mismo escenario, evitando pasar múltiples parámetros
 * sueltos entre Given, When y Then.
 *
 * <h3>Ejemplo de uso en steps</h3>
 * <pre>
 * // En el Given: construir el modelo
 * UsuarioTestData usuario = UsuarioTestData.builder()
 *         .nombre("Ana García")
 *         .perfil("Analista")
 *         .activo(true)
 *         .build();
 *
 * // En el When: usar el modelo en el Page Object
 * miPagina.registrarUsuario(usuario.getNombre(), usuario.getPerfil());
 *
 * // En el Then: verificar con los datos del modelo
 * assertThat(miPagina.obtenerNombreUsuario())
 *         .as("El nombre del usuario registrado debe coincidir")
 *         .isEqualTo(usuario.getNombre());
 * </pre>
 *
 * <h3>Nota sobre Lombok</h3>
 * <p>Este modelo usa el patrón Builder manual para que el equipo junior entienda
 * la estructura sin necesidad de conocer Lombok. Una vez que el equipo domine
 * el patrón, se puede reemplazar por {@code @Builder @Data} de Lombok para
 * reducir el boilerplate.
 */
public class UsuarioTestData {

    // =========================================================================
    // CAMPOS — atributos del usuario de prueba
    //
    // Todos son final: los modelos de datos son inmutables una vez construidos.
    // Esto evita que un step modifique accidentalmente los datos de otro step.
    // =========================================================================

    /** Nombre completo del usuario (ej: "Ana García López") */
    private final String nombre;

    /** Perfil o rol del usuario en el sistema (ej: "Analista", "Supervisor") */
    private final String perfil;

    /** Indica si el usuario está activo en el sistema */
    private final boolean activo;

    // =========================================================================
    // CONSTRUCTOR — privado, se usa solo desde el Builder
    // =========================================================================

    private UsuarioTestData(Builder builder) {
        this.nombre  = builder.nombre;
        this.perfil  = builder.perfil;
        this.activo  = builder.activo;
    }

    // =========================================================================
    // GETTERS — lectura de datos (sin setters: objeto inmutable)
    // =========================================================================

    /** @return nombre completo del usuario */
    public String getNombre()  { return nombre; }

    /** @return perfil o rol del usuario */
    public String getPerfil()  { return perfil; }

    /** @return {@code true} si el usuario está activo */
    public boolean isActivo()  { return activo; }

    // =========================================================================
    // BUILDER — patrón para construir el objeto paso a paso
    //
    // Ventaja vs constructor con múltiples parámetros:
    //   new UsuarioTestData("Ana", "Analista", true)  ← ¿qué es true?
    //   UsuarioTestData.builder().nombre("Ana").perfil("Analista").activo(true).build()
    //   ← autodocumentado, no hay confusión de orden de parámetros
    // =========================================================================

    /**
     * Inicia la construcción de un {@link UsuarioTestData}.
     *
     * @return nuevo {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Constructor fluido para {@link UsuarioTestData}. */
    public static class Builder {

        private String  nombre  = "";
        private String  perfil  = "";
        private boolean activo  = true;

        /** @param nombre nombre completo del usuario */
        public Builder nombre(String nombre)   { this.nombre = nombre;  return this; }

        /** @param perfil perfil o rol del usuario */
        public Builder perfil(String perfil)   { this.perfil = perfil;  return this; }

        /** @param activo {@code true} si el usuario está activo */
        public Builder activo(boolean activo)  { this.activo = activo;  return this; }

        /** Construye el objeto con los valores acumulados. */
        public UsuarioTestData build() {
            return new UsuarioTestData(this);
        }
    }

    // =========================================================================
    // toString — útil para logs y mensajes de error
    // =========================================================================

    @Override
    public String toString() {
        return "UsuarioTestData{"
                + "nombre='" + nombre + '\''
                + ", perfil='" + perfil + '\''
                + ", activo=" + activo
                + '}';
    }
}
