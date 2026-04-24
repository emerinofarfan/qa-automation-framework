# Guía del Capacitador — Cómo conducir el onboarding de QA Automatización

> Este documento es para ti como capacitador.
> Te dice qué preparar, cómo estructurar cada sesión,
> cómo evaluar al trainee y cómo responder las preguntas más comunes.

---

## Antes de la primera sesión — Lista de preparación

### Accesos que debes gestionar antes del Día 1

- [ ] Cuenta GitLab del trainee creada con acceso al repositorio (rol Developer)
- [ ] Credenciales de usuario de prueba en el ambiente TEST (no las tuyas)
- [ ] Java 21 instalado en el equipo del trainee (o confirmado que puede instalarlo)
- [ ] VS Code instalado (o permiso para instalarlo)
- [ ] Confirmar que el trainee puede acceder a la URL del ambiente TEST desde su red

### Lo que tú debes tener listo

- [ ] Este repositorio actualizado y en estado limpio (`git status` = clean)
- [ ] El test de demo corriendo en tu máquina (`./gradlew test -Dcucumber.filter.tags="@Smoke"` pasa)
- [ ] Las variables de entorno configuradas en el `.env` del trainee
- [ ] Una funcionalidad real del sistema para el ejercicio práctico (Sesión 2)

---

## Estructura del programa de capacitación

### Sesión 1 — "El primer test" (2 horas)
**Objetivo:** El trainee corre su primer test automatizado y entiende qué pasó.

**Agenda:**

| Tiempo | Actividad | Documento de apoyo |
|---|---|---|
| 0:00 – 0:20 | Instalar Java + VS Code + clonar repo | `DIA1_CHECKLIST.md` Bloques 1-2 |
| 0:20 – 0:35 | Configurar `.env` y verificar Gradle | `DIA1_CHECKLIST.md` Bloque 3 |
| 0:35 – 0:50 | Correr `@Smoke` y ver el reporte Allure | `DIA1_CHECKLIST.md` Bloque 4 |
| 0:50 – 1:20 | Explicar Feature → Step → Page Object | `COMO_FUNCIONA_EL_FRAMEWORK.md` |
| 1:20 – 1:45 | Seguir el código demo con el trainee (Ctrl+Click) | `DIA1_CHECKLIST.md` Bloque 5 |
| 1:45 – 2:00 | Preguntas y evaluación del bloque 6 | `DIA1_CHECKLIST.md` Checklist final |

**Señal de que la sesión fue exitosa:**
El trainee puede explicarte con sus palabras qué hace cada pieza (Feature, Steps, Page Object)
sin mirar el documento.

---

### Sesión 2 — "Mi primer feature" (4 horas)
**Objetivo:** El trainee escribe un feature, steps y page object desde cero para una funcionalidad real.

**Preparación:** Elige una funcionalidad del sistema con un formulario simple
(ej. búsqueda de un cliente, consulta de saldo, cambio de clave).

**Agenda:**

| Tiempo | Actividad | Documento de apoyo |
|---|---|---|
| 0:00 – 0:30 | Revisar el estilo BDD: qué es un buen escenario | `CUCUMBER_BDD_STYLE_GUIDE.md` |
| 0:30 – 0:50 | Mostrar los templates y el ejemplo demo | `docs/03-templates/` |
| 0:50 – 1:20 | El trainee escribe el feature (con acompañamiento) | `FEATURE_TEMPLATE.feature` |
| 1:20 – 1:40 | Cómo encontrar locators con F12 en el navegador | Sesión práctica en Chrome |
| 1:40 – 2:30 | El trainee crea el Page Object (con acompañamiento) | `PAGE_OBJECT_TEMPLATE.md` |
| 2:30 – 3:30 | El trainee crea los Steps (con acompañamiento) | `STEP_DEFINITION_TEMPLATE.md` |
| 3:30 – 3:50 | Correr el test y depurar si falla | `GUIA_DEPURACION.md` |
| 3:50 – 4:00 | Revisión y feedback | — |

**Señal de que la sesión fue exitosa:**
El test del trainee pasa con `BUILD SUCCESSFUL`.
El trainee puede modificar el locator de un elemento sin ayuda.

---

### Sesión 3 — "Mi primer MR" (2 horas)
**Objetivo:** El trainee sube su trabajo al repositorio siguiendo el estándar del equipo.

**Agenda:**

| Tiempo | Actividad | Documento de apoyo |
|---|---|---|
| 0:00 – 0:20 | Revisar naming de branches y commits | `COMMIT_AND_BRANCH_NAMING_POLICY.md` |
| 0:20 – 0:45 | Crear branch, commit y push del ejercicio | `PRIMER_MR_AUTOMATION.md` |
| 0:45 – 1:00 | Crear el MR en GitLab con descripción completa | `MR_QA_CHECKLIST.md` |
| 1:00 – 1:20 | Revisar el pipeline corriendo en el MR | `INTEGRACION_CON_PLANTILLA_DEVSECOPS.md` |
| 1:20 – 1:40 | Simular una revisión de código (code review) | `MR_QA_CHECKLIST.md` |
| 1:40 – 2:00 | Estándares de calidad y siguiente nivel de madurez | `QA_AUTOMATION_STANDARD_V1.md` |

**Señal de que la sesión fue exitosa:**
El MR del trainee pasa el pipeline automáticamente.
El trainee puede responder por qué sus tests tienen los tags que tiene.

---

## Evaluación del trainee — criterios de aprobación

### Nivel 1: QA Ejecutor (al finalizar las 3 sesiones)

El trainee aprueba si puede hacer de forma independiente (sin ayuda):

- [ ] Correr la suite de Smoke localmente y ver el reporte
- [ ] Escribir un feature con un escenario @Smoke y uno @Negativo
- [ ] Crear un Page Object con al menos 2 locators y 2 métodos de acción
- [ ] Crear los Steps que usan ese Page Object
- [ ] Correr el test y que pase
- [ ] Hacer el commit con el nombre correcto y abrir un MR

### Nivel 2: QA Automatizador (2-4 semanas después)

Evaluación adicional:
- [ ] Puede diagnosticar un `TimeoutException` sin ayuda
- [ ] Entiende y puede ajustar los tags de trazabilidad (`@REQ-`)
- [ ] Puede explicar qué hace `@Destructive` y cuándo usarlo
- [ ] Puede leer el reporte de Allure e identificar el paso fallido

---

## Preguntas frecuentes de trainees — respuestas preparadas

**"¿Por qué Java? Yo sé Python."**
> El framework usa Java porque el sistema a probar es Java/Spring Boot.
> Los locators, el manejo de excepciones y la integración con JaCoCo son
> nativos en Java. En el futuro podría haber un módulo Python, pero hoy
> el estándar del equipo es Java.

**"¿Por qué Selenium y no Cypress?"**
> Cypress solo soporta JavaScript/TypeScript y no funciona bien con aplicaciones
> Java empresariales con SSO. Selenium soporta Java, es agnóstico al stack del
> sistema bajo prueba y tiene soporte corporativo maduro.

**"¿Tengo que aprender Java para ser QA automatizador?"**
> Solo los conceptos básicos: clases, métodos, variables, herencia.
> No necesitas ser desarrollador Java. El framework ya está construido;
> tú escribes features, steps y page objects. Con 2-3 días de práctica
> en Java básico es suficiente para empezar.

**"¿Por qué no puedo editar BasePage.java?"**
> BasePage es la infraestructura del framework, equivale a los cimientos
> de un edificio. Si la modificas sin entender el impacto, puedes romper
> el paralelismo o la gestión del driver para todos los tests.
> Si crees que BasePage necesita un cambio, abre un GitLab Issue etiquetado
> como `framework-improvement` y discútelo con el arquitecto QA.

**"El test pasa en mi máquina pero falla en el pipeline, ¿por qué?"**
> Las causas más comunes: (1) el pipeline corre en modo headless y el test
> asume que hay pantalla; (2) el pipeline apunta a un ambiente diferente;
> (3) el timing es diferente en el servidor (más lento). Ver `GUIA_DEPURACION.md`.

**"¿Puedo usar IntelliJ en lugar de VS Code?"**
> Sí. El framework no depende del IDE. IntelliJ tiene mejor soporte nativo
> para Java. La única diferencia es que los atajos y configuraciones de
> `VSCODE_SETUP.md` no aplican. Instala el plugin de Cucumber para IntelliJ.

**"¿Cómo sé si mi locator es bueno o malo?"**
> Un buen locator: usa `id` o `data-testid`, no cambia cuando rediseñan la UI,
> identifica un solo elemento único en la página.
> Un mal locator: usa `xpath` con índices numéricos (ej. `//div[3]/span[1]`),
> depende del texto visible (que puede traducirse), o es un selector CSS muy genérico.

---

## Señales de alerta durante la capacitación

| Comportamiento | Posible causa | Acción |
|---|---|---|
| El trainee copia código sin entender qué hace | Va demasiado rápido | Detente. Pídele que explique el código copiado. |
| El trainee no puede navegar el proyecto en VS Code | Falta de contexto de la estructura | Volver a `COMO_FUNCIONA_EL_FRAMEWORK.md` |
| El trainee modifica BasePage.java | No entendió los límites | Explicar la responsabilidad de cada capa |
| El trainee hardcodea credenciales | No leyó la política de seguridad | Sesión de `SECURITY_AND_TEST_DATA_POLICY.md` |
| El trainee no puede encontrar locators con F12 | Falta práctica en DevTools | Sesión práctica de 30 min con DevTools |

---

## Recursos de apoyo para el capacitador

- `docs/01-onboarding/COMO_FUNCIONA_EL_FRAMEWORK.md` — usa esto en sesión 1
- `docs/01-onboarding/EJERCICIO_PRACTICO.md` — ejercicio para sesión 2
- `docs/01-onboarding/GUIA_DEPURACION.md` — cuando algo falla en sesión 2
- `docs/02-governance/QA_AUTOMATION_STANDARD_V1.md` — para sesión 3, niveles de madurez

---

*Guía del capacitador v1.0 — QA Automation QA Automatización — 2026-03-15*
