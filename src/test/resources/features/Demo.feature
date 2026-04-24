# =============================================================================
# DEMO — Plantilla de feature para nuevos QAs de Caja Piura
#
# Este archivo muestra el patrón BDD completo del framework:
#   - Tags obligatorios por severidad (@Smoke, @Negativo)
#   - Background para precondición compartida entre escenarios
#   - Escenario positivo (happy path) y negativo (error path)
#   - Nomenclatura en español, verbos en infinitivo
#
# Para crear tu propio feature:
#   1. Copia este archivo y renómbralo con la funcionalidad (ej. TransferenciaFondos.feature)
#   2. Cambia el tag de Feature y el nombre
#   3. Adapta el Background a tu precondición
#   4. Escribe tus escenarios siguiendo este patrón
#   Ver: docs/03-templates/FEATURE_TEMPLATE.md
# =============================================================================

@Demo
Feature: Autenticación de usuario — Acceso al sistema

  # Background: se ejecuta ANTES de cada escenario del feature.
  # Úsalo para precondiciones que todos los escenarios comparten.
  Background: El usuario abre el portal
    Given el usuario navega al portal de la aplicación

  # ---------------------------------------------------------------------------
  # ESCENARIO POSITIVO — happy path
  # NOTA: Este feature es una PLANTILLA de referencia para nuevos QAs.
  # No se ejecuta en CI/CD (@Demo excluido del filtro @Smoke del pipeline).
  # Los tests reales de autenticación deben crearse en el repo de cada app.
  # ---------------------------------------------------------------------------
  @Demo
  Scenario: Ingreso exitoso con credenciales válidas del ambiente de prueba
    When ingresa usuario y clave válidos del ambiente de prueba
    And hace clic en el botón de ingresar
    Then debe visualizarse el panel principal del sistema

  # ---------------------------------------------------------------------------
  # ESCENARIO NEGATIVO — error path, tag @Negativo
  # ---------------------------------------------------------------------------
  @Negativo
  Scenario: Intento de acceso con credenciales incorrectas muestra mensaje de error
    When ingresa el usuario "usuario_invalido" y la clave "clave_incorrecta"
    And hace clic en el botón de ingresar
    Then debe mostrarse un mensaje de error de autenticación
