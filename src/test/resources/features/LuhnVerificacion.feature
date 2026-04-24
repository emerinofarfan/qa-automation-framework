# =============================================================================
# LuhnVerificacion.feature
# Pruebas de la calculadora de dígito verificador Luhn — App piloto de automatización.
#
# Propósito: Validar el flujo completo del framework (Selenium → Chrome headless
# → app real → reporte Allure) usando la app Luhn desplegada en el entorno dev.
#
# Datos de prueba basados en el ejemplo estándar del algoritmo de Luhn (RFC):
#   Número base    : 7992739871  → dígito verificador esperado: 3
#   Número completo: 79927398713 → resultado esperado: válido
#
# Tags:
#   @LuhnVerificacion → identifica toda la suite de esta feature
#   @Smoke            → se ejecuta en MR y master (bloquea el merge si falla)
# =============================================================================

@LuhnVerificacion
Feature: Verificación Luhn — Calculadora de dígito verificador

  Background: El usuario abre la aplicación de verificación Luhn
    Given el usuario accede a la calculadora Luhn

  # ---------------------------------------------------------------------------
  # ESCENARIO 1 — Calcular el dígito verificador a partir de un número base
  # ---------------------------------------------------------------------------
  @Smoke
  Scenario: Calcular el dígito verificador de un número base válido
    When ingresa el número "7992739871" para calcular el dígito verificador
    And presiona el botón calcular
    Then el dígito verificador calculado debe ser "3"

  # ---------------------------------------------------------------------------
  # ESCENARIO 2 — Validar que un número completo pasa el algoritmo de Luhn
  # ---------------------------------------------------------------------------
  @Smoke
  Scenario: Validar que un número completo es correcto según el algoritmo Luhn
    When ingresa el número completo "79927398713" para validar
    And presiona el botón validar
    Then el número debe ser indicado como válido
