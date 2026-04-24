/*
 * Copyright (c) 2026 Caja Piura - Automatización QA
 * Este código es confidencial y está protegido.
 */
package examples;

// =============================================================================
// ARCHIVO DE REFERENCIA PEDAGÓGICA — NO ES UN TEST EJECUTABLE
//
// Propósito: mostrar al equipo QA los patrones más usados en step definitions,
//            con comentarios explicando el "por qué" de cada decisión.
//
// Este archivo vive en el paquete "examples" que NO está en cucumber.glue
// (ver Runner.java: glue = "steps,hooks"). Por eso nunca interfiere con la
// suite real aunque tenga anotaciones @Dado/@Cuando/@Entonces.
//
// Cómo usarlo:
//   1. Lee este archivo completo antes de crear tu primer feature real.
//   2. Copia el patrón que necesitas a tu clase en src/test/java/steps/.
//   3. Adapta los textos de los steps y los page objects.
//   4. Ver también: docs/03-templates/STEP_DEFINITION_TEMPLATE.md
//
// IMPORTANTE — PLACEHOLDERS:
//   Muchos métodos tienen líneas comentadas como:
//     // Ejemplo: miPagina.realizarAccion(valor);
//   y líneas activas con valores fijos como:
//     mensajeCapturado = "Operación procesada exitosamente"; // placeholder
//
//   Esas líneas existen SOLO para que el archivo compile sin errores.
//   Cuando copies un patrón a tu clase real en steps/:
//     - Descomenta la llamada al Page Object.
//     - ELIMINA la línea "placeholder" — nunca la copies tal cual.
// =============================================================================

import io.cucumber.datatable.DataTable;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import models.UsuarioTestData;
import org.assertj.core.api.SoftAssertions;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Catálogo de patrones de step definitions para el equipo QA de Caja Piura.
 *
 * <p>Cada sección demuestra un patrón distinto con comentarios que explican
 * cuándo usarlo y qué errores evita. El código compila pero no se ejecuta
 * porque el paquete {@code examples} no está en la configuración de glue.
 *
 * <h3>Índice de patrones</h3>
 * <ol>
 *   <li>Step básico sin parámetros</li>
 *   <li>Step con parámetro de texto ({@code {string}})</li>
 *   <li>Step con parámetro numérico ({@code {int}})</li>
 *   <li>Step con DataTable — lista de valores</li>
 *   <li>Step con DataTable — tabla de filas (Maps)</li>
 *   <li>Estado compartido entre Given / When / Then</li>
 *   <li>SoftAssertions — múltiples verificaciones en un Then</li>
 *   <li>Verificación de tabla con conteo de filas</li>
 *   <li>Verificación de mensaje dinámico</li>
 *   <li>Uso de modelo de datos (UsuarioTestData)</li>
 * </ol>
 */
@SuppressWarnings("unused")
public class EjemploPatronesSteps {

    // =========================================================================
    // ESTADO COMPARTIDO ENTRE STEPS (patrón 6)
    //
    // Los campos de instancia son la forma correcta de compartir estado entre
    // Given, When y Then DENTRO DEL MISMO ESCENARIO.
    //
    // ¿Por qué no variables static?
    //   Static = compartido entre hilos → en paralelo los escenarios se
    //   pisarían entre sí. Con campos de instancia cada escenario tiene su
    //   propio objeto y su propio estado. Thread-safe.
    //
    // ¿Por qué no variables locales?
    //   Las variables locales desaparecen al salir del método. Si el Given
    //   obtiene un resultado que el Then necesita verificar, debe guardarse
    //   en un campo de instancia.
    // =========================================================================

    /** Resultado capturado en el When para ser verificado en el Then. */
    private String mensajeCapturado;

    /** Cantidad de filas observada en la tabla durante el When. */
    private int filasCapturadasEnTabla;

    // =========================================================================
    // PATRÓN 1 — Step básico sin parámetros
    //
    // Cuándo usarlo: precondiciones o acciones que siempre son iguales.
    // =========================================================================

    /**
     * Ejemplo de step sin parámetros.
     *
     * <p>En el feature se escribe exactamente:
     * <pre>Given el sistema está en el estado inicial</pre>
     */
    @Dado("el sistema está en el estado inicial")
    public void elSistemaEstaEnEstadoInicial() {
        // Llamar al page object que prepara el estado inicial.
        // Nunca poner lógica de Selenium aquí directamente.
        // Ejemplo: miPagina.navegarAlInicio();
    }

    // =========================================================================
    // PATRÓN 2 — Step con parámetro de texto {string}
    //
    // Cuándo usarlo: cuando el dato cambia entre escenarios pero la acción
    // es la misma. Ideal para Scenario Outline con Examples.
    //
    // En el feature:
    //   When el usuario busca "transferencia"
    //   When el usuario busca "préstamo"
    // =========================================================================

    /**
     * Ejemplo de step con parámetro de texto.
     *
     * @param terminoBusqueda texto que el usuario ingresa en el buscador
     */
    @Cuando("el usuario busca {string}")
    public void elUsuarioBusca(String terminoBusqueda) {
        // El parámetro {string} captura el texto entre comillas del feature.
        // Ejemplo: miPagina.escribirEnBuscador(terminoBusqueda);
    }

    // =========================================================================
    // PATRÓN 3 — Step con parámetro numérico {int}
    //
    // En el feature:
    //   Then deben mostrarse 10 resultados en la tabla
    // =========================================================================

    /**
     * Ejemplo de step con parámetro numérico.
     *
     * @param cantidadEsperada cantidad de filas que deben aparecer
     */
    @Entonces("deben mostrarse {int} resultados en la tabla")
    public void debenMostrarseCantidadResultados(int cantidadEsperada) {
        // Cucumber convierte automáticamente el texto del feature a int.
        // Nunca hagas Integer.parseInt() manualmente en un step.
        assertThat(filasCapturadasEnTabla)
                .as("La tabla debe mostrar exactamente %d filas", cantidadEsperada)
                .isEqualTo(cantidadEsperada);
    }

    // =========================================================================
    // PATRÓN 4 — DataTable como lista de valores
    //
    // Cuándo usarlo: cuando el feature pasa una columna de valores.
    //
    // En el feature:
    //   When selecciona los estados:
    //     | Activo   |
    //     | Inactivo |
    //     | Pendiente|
    // =========================================================================

    /**
     * Ejemplo de step que recibe una lista de valores desde el feature.
     *
     * @param dataTable tabla de una columna proveniente del feature
     */
    @Cuando("selecciona los estados:")
    public void seleccionaLosEstados(DataTable dataTable) {
        // asList() convierte la tabla de una columna en una List<String>.
        List<String> estados = dataTable.asList();

        // Itera y aplica la acción en el page object.
        // for (String estado : estados) {
        //     miPagina.seleccionarEstado(estado);
        // }
    }

    // =========================================================================
    // PATRÓN 5 — DataTable como lista de Maps (tabla con cabecera)
    //
    // Cuándo usarlo: cuando el feature pasa múltiples columnas de datos.
    //
    // En el feature:
    //   When registra los siguientes usuarios:
    //     | nombre  | perfil    | estado  |
    //     | Ana     | Analista  | Activo  |
    //     | Luis    | Supervisor| Inactivo|
    // =========================================================================

    /**
     * Ejemplo de step que recibe una tabla con cabecera como lista de Maps.
     *
     * @param dataTable tabla con cabecera proveniente del feature
     */
    @Cuando("registra los siguientes usuarios:")
    public void registraLosUsuarios(DataTable dataTable) {
        // asMaps() convierte cada fila en un Map<String, String> donde
        // la clave es el nombre de la columna (primera fila del feature).
        List<Map<String, String>> filas = dataTable.asMaps();

        for (Map<String, String> fila : filas) {
            String nombre  = fila.get("nombre");
            String perfil  = fila.get("perfil");
            String estado  = fila.get("estado");
            // Ejemplo: miPagina.registrarUsuario(nombre, perfil, estado);
        }
    }

    // =========================================================================
    // PATRÓN 6 — Estado compartido entre steps (ya visto en campos de instancia)
    //
    // Ejemplo de captura en When para verificar en Then.
    // =========================================================================

    /**
     * Captura el mensaje mostrado en pantalla para verificarlo en el Then.
     *
     * <p>Patrón: el When captura, el Then verifica.
     * Nunca hagas la aserción en el When — viola la separación de fases BDD.
     */
    @Cuando("el sistema procesa la operación")
    public void elSistemaProcelaOperacion() {
        // Captura el resultado en el campo de instancia.
        // mensajeCapturado = miPagina.obtenerMensajeResultado();
        mensajeCapturado = "Operación procesada exitosamente"; // placeholder
    }

    // =========================================================================
    // PATRÓN 7 — SoftAssertions: múltiples verificaciones en un Then
    //
    // Cuándo usarlo: cuando un Then debe verificar 3 o más condiciones
    // y quieres que el reporte muestre TODOS los fallos, no solo el primero.
    //
    // Con assertThat() normal: el primer fallo detiene el test.
    // Con SoftAssertions: todos los fallos se acumulan y el test falla al final
    // mostrando el reporte completo.
    // =========================================================================

    /**
     * Ejemplo de verificación múltiple con SoftAssertions.
     *
     * <p>Úsalo cuando el Then debe verificar más de 2 condiciones distintas.
     */
    @Entonces("el resumen de la operación debe ser correcto")
    public void elResumenDebeSerCorrecto() {
        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(mensajeCapturado)
                .as("El mensaje de resultado no debe estar vacío")
                .isNotBlank();

        soft.assertThat(mensajeCapturado)
                .as("El mensaje debe indicar éxito de la operación")
                .containsIgnoringCase("exitosamente");

        // Agrega todas las verificaciones ANTES de assertAll().
        // assertAll() lanza AssertionError con el resumen de todos los fallos.
        soft.assertAll();
    }

    // =========================================================================
    // PATRÓN 8 — Verificación de conteo de filas en tabla
    //
    // Error común: llamar driver.findElements() directamente en el step.
    // Correcto: delegar al Page Object que usa findAll() con espera explícita.
    // =========================================================================

    /**
     * Ejemplo de verificación de conteo de filas con espera implícita correcta.
     *
     * <p>El Page Object usa {@code findAll()} con wait. El step solo verifica
     * el número; nunca invoca Selenium directamente.
     */
    @Entonces("la tabla de resultados contiene al menos {int} filas")
    public void laTablaContieneFilas(int minimoEsperado) {
        // int filas = miPagina.contarFilasTabla();  ← Page Object con findAll()
        int filas = filasCapturadasEnTabla;           // placeholder

        assertThat(filas)
                .as("La tabla debe tener al menos %d filas visibles", minimoEsperado)
                .isGreaterThanOrEqualTo(minimoEsperado);
    }

    // =========================================================================
    // PATRÓN 9 — Verificación de mensaje dinámico
    //
    // Cuándo usarlo: cuando el mensaje incluye datos variables (nombre, número,
    // fecha) que no se pueden predecir exactamente.
    //
    // Malo:  assertThat(msg).isEqualTo("Bienvenido, Ana García — 14/03/2026")
    // Bueno: assertThat(msg).contains("Bienvenido")  y  contains("Ana García")
    // =========================================================================

    /**
     * Ejemplo de verificación de mensaje que contiene texto parcial esperado.
     *
     * @param textoEsperado fragmento de texto que debe aparecer en el mensaje
     */
    @Entonces("el mensaje de bienvenida contiene {string}")
    public void elMensajeBienvenidaContiene(String textoEsperado) {
        // String mensajeReal = miPagina.obtenerMensajeBienvenida();
        String mensajeReal = mensajeCapturado; // placeholder

        assertThat(mensajeReal)
                .as("El mensaje de bienvenida debe contener '%s'", textoEsperado)
                .containsIgnoringCase(textoEsperado);
    }

    // =========================================================================
    // PATRÓN 10 — Uso de modelo de datos (UsuarioTestData)
    //
    // Cuándo usarlo: cuando un escenario maneja un conjunto de datos de prueba
    // que se repite en varios steps del mismo escenario.
    //
    // En lugar de pasar 4 parámetros sueltos al page object, encapsula los
    // datos en un objeto del paquete models/.
    // =========================================================================

    /** Datos del usuario de prueba, compartidos entre los steps del escenario. */
    private UsuarioTestData usuarioDePrueba;

    /**
     * Ejemplo de step que construye un modelo de datos para el escenario.
     *
     * <p>El modelo se guarda como campo de instancia para que el When y el Then
     * puedan acceder a los mismos datos sin repetir parámetros.
     *
     * @param nombreUsuario nombre del usuario a registrar
     */
    @Dado("existe un usuario de prueba con nombre {string}")
    public void existeUnUsuarioDePrueba(String nombreUsuario) {
        // Construir el modelo de datos del escenario.
        usuarioDePrueba = UsuarioTestData.builder()
                .nombre(nombreUsuario)
                .perfil("Analista QA")
                .activo(true)
                .build();

        // Ahora cualquier step puede usar this.usuarioDePrueba
        // sin necesidad de pasar los datos como parámetros.
    }
}
