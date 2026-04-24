# Plantilla de Feature — QA Automation QA Automatizacion

> **Como usar esta plantilla:**
> 1. Copia el bloque de codigo de abajo
> 2. Pega en un nuevo archivo `src/test/resources/features/<modulo>/NombreCapacidad.feature`
> 3. Reemplaza todo lo que esta entre `< >` con valores reales
> 4. Lee [CUCUMBER_BDD_STYLE_GUIDE.md](../02-governance/CUCUMBER_BDD_STYLE_GUIDE.md) si tienes dudas
>
> **Regla de oro:** el feature describe QUE debe hacer el sistema desde el negocio, nunca COMO lo hace tecnicamente.
> Si alguien del area funcional puede leer tu feature y entenderlo sin saber de codigo, esta bien escrito.

---

## Plantilla completa

```gherkin
@<Modulo>
Feature: <Capacidad de negocio en lenguaje funcional>
  Como <rol del usuario: operador, cliente, supervisor, auditor>
  Quiero <accion que desea realizar>
  Para <valor de negocio que obtiene>

  # Background: usa esto SOLO si TODOS los escenarios parten del mismo estado.
  # Si no aplica a todos, elimina el Background y agrega el Given en cada escenario.
  Background:
    Given el usuario autenticado se encuentra en <modulo o seccion del sistema>

  # ESCENARIO FELIZ — ruta principal, obligatorio
  @Smoke @REQ-<CODIGO>
  Scenario: <Accion de negocio> <resultado esperado observable>
    Given <estado inicial o precondicion del negocio>
    When <una sola accion principal del usuario>
    Then <resultado observable y verificable por el negocio>

  # ESCENARIO NEGATIVO — caso de error esperado
  @Regression @Negativo @REQ-<CODIGO>
  Scenario: <Intento de accion invalida> muestra <comportamiento esperado>
    Given <estado inicial que genera el caso negativo>
    When <accion que el sistema debe rechazar>
    Then <mensaje o comportamiento esperado ante el error>
    But <lo que NO debe ocurrir — util para controles de seguridad>

  # SCENARIO OUTLINE — misma regla, distintos datos (minimo 3 filas en Examples)
  @Regression @REQ-<CODIGO>
  Scenario Outline: <Regla de negocio> con <dato> produce <resultado>
    Given <estado inicial>
    When <accion con "<dato_entrada>">
    Then <resultado esperado segun "<resultado_esperado>">

    Examples:
      | dato_entrada | resultado_esperado |
      | <valor1>     | <resultado1>       |
      | <valor2>     | <resultado2>       |
      | <valor3>     | <resultado3>       |
```

---

## Checklist antes de hacer commit

Marca cada punto antes de abrir el MR:

**Feature**
- [ ] El nombre describe una capacidad de negocio (no una pantalla tecnica ni modulo de codigo)
- [ ] Incluye descripcion `Como... Quiero... Para...` si tiene mas de 5 escenarios
- [ ] Background presente solo si TODOS los escenarios comparten el mismo estado inicial

**Escenarios**
- [ ] El nombre es predictivo: al leerlo sabes exactamente que comportamiento valida
- [ ] Cada escenario valida UN solo comportamiento de negocio
- [ ] `Given` establece un estado — sin verbos de accion del usuario (click, navegar, escribir)
- [ ] `When` tiene una sola accion principal
- [ ] `Then` es verificable con un assertion especifico y objetivo
- [ ] Sin referencias tecnicas: sin xpath, css, id=, url, elemento, div, button, click

**Tags**
- [ ] Cada escenario tiene tag de estrategia: `@Smoke`, `@Regression` o `@Critical`
- [ ] Cada escenario tiene tag de trazabilidad: `@REQ-` o `@HU-`
- [ ] Sin `@Wip` en escenarios que van a master
- [ ] Escenarios que modifican datos tienen `@Destructive`

**Lenguaje**
- [ ] Todo en espanol, verbos en tiempo presente
- [ ] Sin palabras ambiguas: "correcto", "ok", "normal", "funciona", "exitoso" sin contexto
- [ ] Sujeto consistente en todo el feature (el usuario como sujeto)

**Organizacion**
- [ ] Archivo en `src/test/resources/features/<modulo>/` (carpeta del modulo de negocio)
- [ ] Nombre del archivo refleja la capacidad: `BusquedaCliente.feature`, no `Test1.feature`

---

## Ejemplo real aplicando la plantilla

```gherkin
@Clientes
Feature: Busqueda de cliente por documento de identidad
  Como operador de plataforma de QA Automation
  Quiero buscar un cliente por su DNI
  Para acceder rapidamente a su informacion y gestionar su cuenta

  Background:
    Given el operador autenticado se encuentra en el modulo de Clientes

  @Smoke @REQ-CLI-001
  Scenario: Busqueda por DNI valido retorna los datos del cliente
    Given existe un cliente registrado con DNI "12345678"
    When el operador busca por el DNI "12345678"
    Then el sistema muestra el nombre y datos del cliente encontrado

  @Regression @Negativo @REQ-CLI-002
  Scenario: Busqueda por DNI inexistente muestra mensaje informativo sin error tecnico
    When el operador busca por el DNI "99999999"
    Then el sistema indica que no se encontraron resultados para el DNI ingresado
    But el sistema no muestra mensajes de error tecnico ni trazas de codigo

  @Regression @Negativo @REQ-CLI-003
  Scenario Outline: Busqueda con formato de DNI invalido muestra validacion del campo
    When el operador intenta buscar con el valor "<dni_invalido>"
    Then el sistema muestra el mensaje "<mensaje_validacion>"

    Examples:
      | dni_invalido | mensaje_validacion                          |
      | ABC123       | El DNI debe contener solo digitos numericos |
      | 123          | El DNI debe tener 8 digitos                 |
      | 123456789    | El DNI no puede tener mas de 8 digitos      |
```

---

*Plantilla v2.0 — QA Automation QA Automatizacion — 2026-03-15*
