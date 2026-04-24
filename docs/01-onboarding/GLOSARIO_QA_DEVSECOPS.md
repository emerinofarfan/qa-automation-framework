# Glosario QA DevSecOps (Nivel Inicial)

Este glosario explica palabras tecnicas que aparecen en README, scripts, onboarding,
gobernanza y pipeline del framework.

## Base de automatizacion

- **Selenium:** libreria Java que permite controlar un navegador web (Chrome, Firefox) desde codigo. Es el "control remoto" del navegador. Ver `LAS_HERRAMIENTAS_Y_POR_QUE.md`.
- **WebDriver:** protocolo estandar (W3C) que usa Selenium para comunicarse con el navegador. Es el "idioma" entre tu codigo y Chrome.
- **ChromeDriver:** programa ejecutable que Google provee para traducir las instrucciones de WebDriver al lenguaje interno de Chrome. Se descarga automaticamente con WebDriverManager.
- **BDD:** (Behavior-Driven Development) enfoque de pruebas basado en comportamiento con lenguaje cercano al negocio. Los tests se escriben como si fueran historias de usuario.
- **Cucumber:** motor que ejecuta escenarios BDD escritos en Gherkin (`Given/When/Then`). Conecta el lenguaje natural del feature con el codigo Java.
- **Gherkin:** el lenguaje de los archivos `.feature`. Usa palabras clave (`Given`, `When`, `Then`, `And`) para describir escenarios en lenguaje natural.
- **Feature file:** archivo `.feature` que contiene uno o mas escenarios escritos en Gherkin. Es lo que escribe el QA para describir que debe probarse.
- **Scenario (Escenario):** caso de prueba funcional individual dentro de un feature.
- **Step Definition:** codigo Java que implementa cada paso del escenario. Conecta una linea Gherkin con una accion real en el navegador.
- **Page Object:** clase Java que encapsula los locators y acciones de una pantalla del sistema.
- **Locator:** selector que identifica un elemento HTML en la pagina (`By.id`, `By.cssSelector`, `By.xpath`).
- **Tag:** etiqueta de Cucumber (`@Smoke`, `@Regression`) para filtrar ejecuciones.
- **Suite:** conjunto de escenarios ejecutados bajo un mismo criterio (por tag o task).
- **Runner:** clase que arranca Cucumber/JUnit y define glue/plugins base.
- **Hooks:** metodos `@Before/@After` que preparan y limpian cada escenario.

## Ejecucion y rendimiento

- Smoke: conjunto minimo de pruebas criticas para validar continuidad.
- Critical: escenarios de alto impacto funcional para validaciones previas a promocion.
- Regression: conjunto amplio para validar que no hubo regresiones funcionales.
- Destructive: escenarios que alteran datos globales y se ejecutan al final en secuencial.
- Secuencial: ejecucion de un escenario a la vez (facil para depurar).
- Paralelo: ejecucion de multiples escenarios al mismo tiempo usando hilos.
- Parallelism: numero de hilos usados en ejecucion paralela.
- Thread (Hilo): unidad de ejecucion concurrente dentro de una JVM.
- Thread-safe: implementacion segura para uso concurrente sin colisiones entre hilos.
- ThreadLocal: mecanismo para asignar una instancia por hilo (ejemplo: un driver por hilo).
- Headless: ejecucion de browser sin interfaz grafica visible (util en CI/CD).
- Flaky test: prueba inestable que falla de forma intermitente sin cambio funcional real.
- Rerun: relanzar solo escenarios fallidos (ejemplo: `build/rerun.txt`).
- Baseline: resultado de referencia inicial (normalmente en secuencial) para comparar cambios.

## Scripts y operacion local

- Scaffold: generacion automatica de estructura inicial de proyecto (carpetas + archivos base).
- Wrapper: script que simplifica un comando largo (ejemplo: `quick-run.ps1` sobre Gradle).
- Gradle Wrapper (`gradlew`): ejecuta Gradle con version fija del proyecto sin depender de instalacion global.
- Workspace (VS Code): carpeta/proyecto abierto con su configuracion local (`.vscode/`).

## CI/CD y DevSecOps

- CI/CD: integracion continua y entrega/despliegue continuo mediante pipeline automatizado.
- Pipeline: secuencia de jobs automatizados (build, test, seguridad, reportes).
- Job: unidad de trabajo individual dentro de un pipeline.
- Stage: fase del pipeline que agrupa jobs (ej. validate, test, security).
- Artifact: archivo generado por el pipeline (reportes, logs, resultados de pruebas).
- Preflight: validacion rapida previa a pruebas pesadas (ej. verificar `BASE_URL` disponible).
- MR (Merge Request): solicitud formal para integrar cambios de una rama a otra.
- CODEOWNERS: archivo que define que equipo/persona debe revisar cambios por ruta.
- Code Owner Approval: aprobacion obligatoria del owner definido en CODEOWNERS para permitir merge.
- Quality Gate: condicion obligatoria para permitir merge o promocion.
- Branch protection: regla de GitLab para bloquear push directo y exigir controles.
- Protected variable: variable CI/CD restringida para ramas protegidas.
- SAST: analisis estatico de seguridad del codigo fuente.
- Checkmarx: herramienta SAST usada en el pipeline corporativo.

## Gobierno y release

- UAT: validacion de aceptacion por usuarios o area funcional en entorno de pruebas.
- CAB: comite de cambios que aprueba paso a produccion.
- Peer review: revision de codigo por otro integrante del equipo.
- Hotfix: correccion urgente aplicada con flujo controlado hacia ramas principales.
- Release checklist: lista obligatoria de control antes de liberar cambios.

## Datos y seguridad

- Dataset sintetico: datos de prueba no reales ni sensibles.
- Secreto: credencial/token/clave que nunca debe quedar hardcodeada en repositorio.
- Masking: enmascaramiento de datos sensibles en logs o reportes.

## Reporteria y observabilidad

- Allure: herramienta de reportes de pruebas con evidencias y trazabilidad de escenarios.
- Evidencia: capturas, logs y resultados anexados para auditoria y validacion.
