# Java Directory Structure Standard - QA Automation QA Automation

## 1. Objetivo
Definir, de forma clara y accionable, que debe ir en cada carpeta de `src/test/java`.
Este documento evita mezcla de responsabilidades, reduce deuda tecnica y acelera onboarding desde QA en nivel inicial hasta QA senior.

## 2. Alcance
Aplica a todo proyecto de automatizacion basado en este framework.

Cobertura de carpetas:
- `src/test/java/core`
- `src/test/java/hooks`
- `src/test/java/models`
- `src/test/java/pages`
- `src/test/java/runner`
- `src/test/java/utils`
- `src/test/java/examples`
- `src/test/java/steps` (cuando exista en el proyecto de equipo)

## 3. Reglas generales para todas las carpetas

1. Una clase debe tener una responsabilidad principal.
2. No hardcodear credenciales ni datos sensibles.
3. Evitar logica de negocio dentro de clases tecnicas del framework.
4. Evitar sleeps fijos; usar esperas explicitas y constantes de timeout.
5. Toda utilidad reutilizable debe tener nombre claro y enfoque especifico.
6. El codigo debe ser legible para un QA en nivel inicial con comentarios puntuales en bloques complejos.

## 4. Guia por carpeta

## 4.1 core

### Proposito
Contener contratos y componentes base del motor de automatizacion UI.

### Que si va aqui
- Interfaces tecnicas del framework.
- Contratos de localizadores y elementos.
- Configuracion transversal de browser.

### Que NO va aqui
- Steps de Cucumber.
- Logica de negocio de una aplicacion concreta.
- Datos de prueba de escenarios.

### Ejemplos reales del repositorio
- `core/browser/BrowserConfig.java`
- `core/browser/IBrowserDriver.java`
- `core/browser/IElement.java`
- `core/browser/ILocator.java`
- `core/browser/Locators.java`

### Regla de calidad
Si el codigo seria reutilizable por cualquier proyecto QA, probablemente pertenece a `core`.
Si depende de un flujo de negocio especifico, no pertenece a `core`.

## 4.2 hooks

### Proposito
Gestionar el ciclo de vida de escenarios y evidencia transversal.

### Que si va aqui
- `@Before` para inicializacion comun.
- `@After` para cierre, screenshot, limpieza y logging transversal.
- Tareas comunes por escenario no ligadas a una historia puntual.

### Que NO va aqui
- Pasos Given/When/Then.
- Navegacion funcional de una pagina especifica.
- Validaciones de negocio de un feature concreto.

### Ejemplo real del repositorio
- `hooks/Hooks.java`

### Regla de calidad
Si el bloque se ejecutaria igual para muchos escenarios, puede ir en hooks.
Si aplica solo a un escenario funcional, debe ir en steps/pages.

## 4.3 models

### Proposito
Representar objetos de datos usados por pruebas (DTOs, payloads, estructuras de apoyo).

### Que si va aqui
- Clases de datos para requests/responses de pruebas.
- Estructuras para lectura de fixtures o datasets.
- Objetos de dominio de prueba sin comportamiento UI.

### Que NO va aqui
- Codigo de Selenium.
- Operaciones de UI.
- Logica de setup de driver.

### Estado actual del repositorio
- Carpeta presente y lista para uso por equipos.
- Mantenerla limpia hasta que exista necesidad real de modelos de datos.

### Regla de calidad
Si una clase solo transporta datos de prueba y no conoce Selenium, es candidata para `models`.

## 4.4 pages

### Proposito
Implementar Page Objects: interacciones y consultas sobre la UI.

### Que si va aqui
- Selectores, acciones y verificaciones de bajo nivel UI.
- Metodos reutilizables por varios steps.
- Encapsulacion de detalles tecnicos de pantalla.

### Que NO va aqui
- Anotaciones Given/When/Then.
- Orquestacion completa de escenarios end-to-end.
- Reglas de negocio complejas en cascada.

### Ejemplo real del repositorio
- `pages/BasePage.java`

### Regla de calidad
Un step debe poder leer como historia funcional.
Si el step tiene demasiados detalles tecnicos de click/input/wait, moverlos a `pages`.

## 4.5 runner

### Proposito
Centralizar configuracion de ejecucion de pruebas y listeners de ejecucion.

### Que si va aqui
- Clase runner principal.
- Listeners para resumen de ejecucion, metricas o hooks de framework.

### Que NO va aqui
- Steps.
- Page Objects.
- Utilidades genericas sin relacion con ejecucion.

### Ejemplos reales del repositorio
- `runner/Runner.java`
- `runner/TestExecutionListener.java`

### Regla de calidad
Cambios en runner deben ser poco frecuentes y coordinados, porque impactan toda la suite.

## 4.6 utils

### Proposito
Alojar utilidades transversales y componentes tecnicos de soporte.

### Que si va aqui
- Manejo de configuracion.
- Driver factory y componentes de infraestructura.
- Constantes de timeout.
- Resumen de ejecucion.
- Manejo de excepciones tecnicas.
- Enmascaramiento de datos sensibles.

### Que NO va aqui
- Logica funcional de negocio.
- Steps o pages de una app concreta.
- Clases comodin gigantes con metodos sin cohesion.

### Ejemplos reales del repositorio
- `utils/AutomationException.java`
- `utils/ConfigManager.java`
- `utils/DataUtils.java`
- `utils/DriverFactory.java`
- `utils/SensitiveDataMasker.java`
- `utils/TestExecutionSummary.java`
- `utils/TimeoutConstants.java`

### Regla de calidad
Toda utilidad nueva en `utils` debe responder: "esto sera reutilizado por mas de un flujo y mantiene cohesion tecnica".

## 4.7 examples

### Proposito
Mostrar patrones de implementacion para onboarding. Es la "biblioteca de referencia"
del equipo: codigo que se lee y copia, no codigo que se ejecuta directamente.

### Que si va aqui
- Clases que demuestran patrones de step definitions (DataTable, SoftAssertions,
  estado compartido, parametros numericos, etc.).
- Ejemplos autocontenidos con comentarios explicando el "por que" de cada decision.

### Que NO va aqui
- Casos productivos reales del equipo (esos van en `steps/` y `pages/`).
- Codigo sensible o datos de negocio reales.

### Ejemplo real del repositorio
- `examples/EjemploPatronesSteps.java` ← catálogo de 10 patrones comentados

### Por que este paquete no interfiere con la suite
El Runner configura `glue = "steps,hooks"`. El paquete `examples` no esta en el glue,
por lo que Cucumber nunca carga ni ejecuta sus step definitions. Los ejemplos conviven
con la suite sin romper nada.

### Regla de calidad
Los ejemplos deben priorizar claridad y estandar, no complejidad.

## 4.8 steps (en proyectos de equipo)

### Proposito
Traducir pasos BDD (Given/When/Then) a llamadas de pages y utils.

### Que si va aqui
- Metodos anotados de Cucumber.
- Orquestacion ligera de acciones y validaciones.
- Lectura clara de la historia de negocio.

### Que NO va aqui
- Selectores y detalles tecnicos UI profundos.
- Setup tecnico global de framework.
- Clases de datos o DTOs.

### Referencias del repositorio
- Ejemplo operativo ejecutable: `steps/DemoLoginSteps.java` + `src/test/resources/features/Demo.feature`
- Catálogo de patrones avanzados: `examples/EjemploPatronesSteps.java`
- Plantilla oficial: `docs/03-templates/STEP_DEFINITION_TEMPLATE.md`

### Regla de calidad
Si un step supera complejidad moderada por detalles tecnicos, refactorizar hacia `pages` y `utils`.

## 5. Matriz rapida de decision (nivel inicial)

| Necesito agregar... | Carpeta recomendada |
|---|---|
| Contrato/interfaz de browser o elemento | `core` |
| Inicializacion/cierre por escenario | `hooks` |
| Objeto de datos para pruebas | `models` |
| Acciones sobre pantalla y selectores | `pages` |
| Configuracion de ejecucion global | `runner` |
| Utilidad tecnica transversal | `utils` |
| Ejemplo didactico para onboarding | `examples` |
| Given/When/Then de Cucumber | `steps` |

## 6. Anti-patrones a bloquear en MR

1. Steps con mas de 2-3 detalles UI tecnicos incrustados.
2. Pages con reglas de negocio no reutilizables.
3. Utils tipo "God class" con metodos sin cohesion.
4. Hooks con validaciones funcionales de un escenario puntual.
5. Runner modificado para resolver problemas locales de un solo equipo.
6. Mezcla de datos sensibles en codigo fuente o logs sin mascara.

## 7. Checklist de revision por senior

Antes de aprobar MR, validar:
1. Cada clase nueva esta en la carpeta correcta segun responsabilidad.
2. No hay duplicacion entre `steps`, `pages` y `utils`.
3. No existen secretos hardcodeados.
4. Las esperas usan estrategia controlada (sin sleeps arbitrarios).
5. El codigo nuevo puede entenderse por un QA en nivel inicial con minima asistencia.
6. Los cambios en `core` o `runner` tienen justificacion tecnica y alcance transversal.

## 8. Ruta de aprendizaje recomendada

Para QA en nivel inicial:
1. Leer este documento completo.
2. Revisar `src/test/resources/features/Demo.feature` y `steps/DemoLoginSteps.java` (demo ejecutable).
3. Leer `examples/EjemploPatronesSteps.java` para ver los 10 patrones más usados con comentarios.
4. Crear un flujo simple separando claramente `steps` y `pages`.

Para QA semi senior:
1. Refactorizar pasos largos hacia pages.
2. Consolidar utilidades con cohesion en `utils`.
3. Proponer mejoras de mantenibilidad en hooks y timeouts.

Para QA senior:
1. Gobernar cambios de `core` y `runner`.
2. Evaluar impacto en paralelismo y estabilidad.
3. Asegurar alineacion con quality gates y politicas de seguridad.

## 9. Relacion con otros estandares

Este documento complementa, no reemplaza:
- `docs/02-governance/QA_AUTOMATION_STANDARD_V1.md`
- `docs/02-governance/SELENIUM_JAVA_PRACTICES.md`
- `docs/02-governance/CUCUMBER_BDD_STYLE_GUIDE.md`
- `docs/02-governance/SECURITY_AND_TEST_DATA_POLICY.md`
