# Referencia de Comandos Gradle — Framework QA QA Automation

> **Para Windows:** usa `.\gradlew` (PowerShell) o `./gradlew` (bash/Git Bash).
> **Los comandos de esta guia usan `.\gradlew` (PowerShell — estandar del equipo).**

---

## Tabla de contenido rapido

| Quiero...                                    | Ir a sección |
|----------------------------------------------|-------------|
| Compilar y verificar que todo compila        | [Compilacion](#1-compilacion) |
| Correr mi primer @Smoke                      | [Primer dia](#2-primer-dia--comandos-de-inicio) |
| Correr tests en secuencial (depuracion)      | [Secuencial](#3-ejecucion-secuencial) |
| Correr tests en paralelo (velocidad)         | [Paralelo](#4-ejecucion-en-paralelo) |
| Filtrar por tags (Smoke, Regression, etc.)   | [Tags](#5-filtrado-por-tags) |
| Correr un solo escenario o feature           | [Escenario especifico](#6-ejecutar-un-escenario-o-feature-especifico) |
| Correr los tests que fallaron antes          | [Rerun](#7-rerun--correr-solo-los-escenarios-que-fallaron) |
| Correr tests con ambiente especifico         | [Entornos](#8-ejecutar-contra-un-entorno-especifico) |
| Generar reporte Allure                       | [Allure](#9-reportes-allure) |
| Correr tests destructivos                    | [Destructive](#10-tests-destructivos-destructive) |
| Ver que pasa en consola (verbose)            | [Debug](#11-modo-debug-y-verbose) |
| Validar calidad del codigo (checkstyle)      | [Calidad](#12-tareas-de-calidad-de-codigo) |
| Ver todos los tasks disponibles              | [Catalogo](#13-catalogo-completo-de-tasks-gradle) |
| Ayuda cuando algo falla                      | [Errores comunes](#14-errores-comunes-al-ejecutar) |

---

## 1. Compilacion

**Primer paso antes de ejecutar cualquier test.** Detecta errores de Java antes de
abrir ningun navegador.

```powershell
# Limpiar build anterior y compilar desde cero
.\gradlew clean compileTestJava

# Solo compilar sin limpiar (mas rapido si solo cambiaste un archivo)
.\gradlew compileTestJava

# Compilar y verificar que Checkstyle no tiene violaciones
.\gradlew compileTestJava checkstyleMain
```

> Si la compilacion falla: revisa el mensaje de error. Normalmente es un import faltante,
> un metodo que no existe o un error de sintaxis. Nunca ejecutes tests si la compilacion falla.

---

## 2. Primer dia — comandos de inicio

Si es tu primer dia con el framework, usa exactamente esta secuencia:

```powershell
# Paso 1 — Compilar (solo una vez al inicio del dia)
.\gradlew clean compileTestJava

# Paso 2 — Correr el ejemplo demo en secuencial (sin paralelo para ver logs claros)
.\gradlew test "-Dcucumber.filter.tags=@Smoke" "-Dcucumber.execution.parallel.enabled=false"

# Paso 3 — Ver el reporte de lo que corrio
.\gradlew allureReport
# Abre: build/reports/allure-report/allureReport/index.html
```

> Si falla el paso 2, no avances. Revisa
> [docs/01-onboarding/TROUBLESHOOTING_AUTOMATION.md](docs/01-onboarding/TROUBLESHOOTING_AUTOMATION.md)

---

## 3. Ejecucion secuencial

Secuencial = un escenario a la vez. Ideal para depurar, ver logs completos o correr
un solo tag sin interferencia entre tests.

```powershell
# Cualquier tag, modo secuencial
.\gradlew test "-Dcucumber.filter.tags=@Smoke" "-Dcucumber.execution.parallel.enabled=false"

.\gradlew test "-Dcucumber.filter.tags=@Regression" "-Dcucumber.execution.parallel.enabled=false"

.\gradlew test "-Dcucumber.filter.tags=@Auth" "-Dcucumber.execution.parallel.enabled=false"

# Todos los tests en secuencial (sin filtro de tag, sin paralelo)
.\gradlew test "-Dcucumber.execution.parallel.enabled=false"
```

> **Cuando usar secuencial:**
> - Cuando un test esta fallando y quieres ver el log limpio
> - Cuando estas desarrollando un escenario nuevo
> - Cuando el test involucra datos que podrian colisionar en paralelo

---

## 4. Ejecucion en paralelo

Paralelo = hasta N escenarios al mismo tiempo, cada uno con su propio Chrome.
El tiempo total es el del escenario mas largo, no la suma de todos.

```powershell
# Paralelo con 3 hilos (configuracion por defecto del proyecto)
.\gradlew test "-Dcucumber.filter.tags=@Smoke"

# Paralelo con numero de hilos personalizado
.\gradlew test "-Dcucumber.filter.tags=@Smoke" `
    "-Dcucumber.execution.parallel.enabled=true" `
    "-Dcucumber.execution.parallel.config.fixed.parallelism=3"

# Regresion completa con 3 hilos (tipico para CI/CD)
.\gradlew test "-Dcucumber.filter.tags=@Regression" `
    "-Dcucumber.execution.parallel.enabled=true" `
    "-Dcucumber.execution.parallel.config.fixed.parallelism=3"

# Suite completa con el default del proyecto (paralelo 3 hilos, excluye @Destructive)
.\gradlew test
```

### Cuantos hilos usar

| Ambiente               | Hilos recomendados | RAM minima | Modo Chrome     |
|------------------------|--------------------|------------|-----------------|
| Local Windows          | 2–3                | 4 GB       | Normal (visible)|
| CI runner estandar     | 3–5                | 3 GB       | Headless (auto) |
| CI runner dedicado     | 5–8                | 5 GB       | Headless (auto) |
| CI nightly / XL        | 8–12               | 7 GB       | Headless (auto) |

> **Formula RAM:** `N_hilos × 300 MB + 800 MB JVM`. Con 3 hilos: ~1,700 MB.
> El modo headless es automatico cuando `CI=true` (GitLab lo define). En local,
> forzar headless con: `$env:BROWSER_HEADLESS="true"` antes del comando.
>
> Aumentar hilos sin validar el runner genera flakiness. Escalar en pasos: 3 → 5 → 8.

---

## 5. Filtrado por tags

### Tags disponibles en el proyecto

| Tag | Que incluye | Cuando correrlo |
|---|---|---|
| `@Smoke` | Los N casos mas criticos del sistema | En cada push, rapido, debe pasar siempre |
| `@Regression` | Suite completa de regresion | Antes de cada release o MR |
| `@Auth` | Login, sesion, cierre de sesion | Al cambiar el modulo de autenticacion |
| `@Destructive` | Operaciones que modifican datos de referencia | Al final, secuencial, con cuidado |
| `@Negativo` | Flujos de error y validacion | En regresion y cuando el modulo cambia |
| `@REQ-XXX` | Trazabilidad a un requisito especifico | Para reportes de cobertura al negocio |
| `@Manual` | Escenarios documentados pero no automatizados aun | No se ejecutan, son referencia |
| `@WIP` | Work in Progress — en desarrollo activo | Solo el QA que lo esta desarrollando |

### Combinaciones de tags (operadores logicos)

```powershell
# UN solo tag
.\gradlew test "-Dcucumber.filter.tags=@Smoke"

# AND — debe tener AMBOS tags
.\gradlew test "-Dcucumber.filter.tags=@Smoke and @Auth"
.\gradlew test "-Dcucumber.filter.tags=@Regression and @Negativo"

# OR — debe tener AL MENOS UNO de los tags
.\gradlew test "-Dcucumber.filter.tags=@Smoke or @Auth"

# NOT — excluir un tag
.\gradlew test "-Dcucumber.filter.tags=@Regression and not @Destructive"
.\gradlew test "-Dcucumber.filter.tags=@Regression and not @WIP"

# Combinaciones complejas
.\gradlew test "-Dcucumber.filter.tags=(@Smoke or @Auth) and not @WIP"
.\gradlew test "-Dcucumber.filter.tags=@Regression and not @Destructive and not @Manual"

# Por requisito especifico
.\gradlew test "-Dcucumber.filter.tags=@REQ-1234"

# Varios requisitos
.\gradlew test "-Dcucumber.filter.tags=@REQ-1234 or @REQ-1235 or @REQ-1236"
```

> **Nota sobre el default:** si no indicas `-Dcucumber.filter.tags`, el proyecto corre
> `@Regression and not @Destructive` segun `junit-platform.properties`.

---

## 6. Ejecutar un escenario o feature especifico

```powershell
# Correr un feature file especifico (ruta relativa desde src/test/resources/)
.\gradlew test "-Dcucumber.features=classpath:features/autenticacion/Login.feature" `
    "-Dcucumber.execution.parallel.enabled=false"

# Correr todos los features de una carpeta
.\gradlew test "-Dcucumber.features=classpath:features/transferencias" `
    "-Dcucumber.execution.parallel.enabled=false"

# Correr un escenario especifico por nombre exacto
.\gradlew test "-Dcucumber.filter.name=Login exitoso con credenciales validas" `
    "-Dcucumber.execution.parallel.enabled=false"

# Correr escenarios cuyo nombre contenga una palabra clave (busqueda parcial)
.\gradlew test "-Dcucumber.filter.name=transferencia" `
    "-Dcucumber.execution.parallel.enabled=false"

# Combinar feature especifico + tag
.\gradlew test `
    "-Dcucumber.features=classpath:features/autenticacion/Login.feature" `
    "-Dcucumber.filter.tags=@Negativo" `
    "-Dcucumber.execution.parallel.enabled=false"
```

> El nombre del escenario es el texto que va despues de `Scenario:` en el feature.
> La busqueda es case-sensitive y parcial: `"transferencia"` encuentra todos los
> escenarios que contienen esa palabra en su nombre.

---

## 7. Rerun — correr solo los escenarios que fallaron

El framework genera automaticamente `build/rerun.txt` con los escenarios que fallaron
en la ultima ejecucion. Esto evita correr toda la suite cuando solo 2 de 30 tests fallaron.

```powershell
# Paso 1 — Correr la suite normalmente (genera rerun.txt con los fallidos)
.\gradlew test "-Dcucumber.filter.tags=@Regression"

# Paso 2 — Si hubo fallas, relanzar SOLO los escenarios que fallaron
.\gradlew test "-Dcucumber.features=@build/rerun.txt" `
    "-Dcucumber.execution.parallel.enabled=false"

# Si el rerun pasa: los tests son FLAKY. Investigar la causa raiz antes del MR.
# Si el rerun tambien falla: es un bug real. Corregir antes de mergear.
```

> **Importante:** `rerun.txt` se sobreescribe en cada ejecucion de `.\gradlew test`.
> Si haces dos runs seguidos, el segundo `rerun.txt` tiene los fallidos del segundo run.
> Usar `@build/rerun.txt` inmediatamente despues del run que quieres relanzar.

---

## 8. Ejecutar contra un entorno especifico

### Con variable de entorno (recomendado para cambios temporales)

```powershell
# Apuntar a TEST (ambiente de desarrollo — trabajo diario)
$env:TEST_BASE_URL="https://testwebcastigos.example.pe/Sample ApplicationAutomatizados/auth"
$env:TEST_USERNAME="mi_usuario_test"
$env:TEST_PASSWORD="mi_clave_test"
.\gradlew test "-Dcucumber.filter.tags=@Smoke"

# Apuntar a UAT (requiere credenciales de UAT)
$env:TEST_BASE_URL="https://uatwebcastigos.example.pe/Sample ApplicationAutomatizados/auth"
$env:TEST_USERNAME="mi_usuario_uat"
$env:TEST_PASSWORD="mi_clave_uat"
.\gradlew test "-Dcucumber.filter.tags=@Smoke"
```

### Con parametro -D (sobreescribe todo por comando, sin modificar .env)

```powershell
.\gradlew test `
    "-Dbase.url=https://testwebcastigos.example.pe/Sample ApplicationAutomatizados/auth" `
    "-Dtest.username=mi_usuario" `
    "-Dtest.password=mi_clave" `
    "-Dcucumber.filter.tags=@Smoke" `
    "-Dcucumber.execution.parallel.enabled=false"
```

### Activar modo headless (sin ventana de Chrome visible)

```powershell
# Headless para local (normalmente activo solo en CI)
$env:BROWSER_HEADLESS="true"
.\gradlew test "-Dcucumber.filter.tags=@Smoke"

# Volver a modo visible
$env:BROWSER_HEADLESS="false"
```

---

## 9. Reportes Allure

### Comandos de reporte

```powershell
# Generar reporte estatico (carpeta HTML) despues de un test run
.\gradlew allureReport

# Levantar servidor Allure en vivo (abre el navegador automaticamente)
.\gradlew allureServe

# Correr tests y generar reporte en un solo comando
.\gradlew test allureReport "-Dcucumber.filter.tags=@Smoke"

# Correr tests y abrir el servidor Allure automaticamente al terminar
.\gradlew test allureServe "-Dcucumber.filter.tags=@Smoke"

# Limpiar los resultados de Allure anteriores (antes de un run limpio)
.\gradlew clean
# (clean borra build/ completo, incluyendo allure-results/)
```

### Donde esta el reporte

| Tipo | Ubicacion | Como abrirlo |
|---|---|---|
| Reporte estatico HTML | `build/reports/allure-report/allureReport/index.html` | Abrir con cualquier navegador |
| Resultados raw (JSON) | `build/allure-results/` | No abrir directamente |
| Servidor en vivo | `http://localhost:PORT` | Se abre automaticamente con `allureServe` |
| ExtentReports HTML | `test-output/ExtentReport.html` | Abrir con cualquier navegador |

### Diferencia entre `allureReport` y `allureServe`

```
allureReport → genera HTML estatico en build/reports/allure-report/
               Puedes compartir la carpeta o adjuntarla en el MR
               Requiere abrir index.html manualmente

allureServe  → levanta un servidor HTTP temporal en tu maquina
               Abre el navegador automaticamente con el reporte interactivo
               El servidor se cierra cuando presionas Ctrl+C en la terminal
               Ideal para revisar resultados en local rapidamente
```

---

## 10. Tests destructivos (@Destructive)

Los tests `@Destructive` modifican datos de referencia o configuracion del sistema.
Corren **siempre en secuencial** y **despues de todos los demas tests**.

```powershell
# Correr TODA la suite: primero Regression (paralelo), luego Destructive (secuencial)
# Este es el comando de CI/CD para el pipeline completo
.\gradlew testAll

# Correr SOLO los Destructive (en secuencial, aislado)
.\gradlew testDestructive

# Correr toda la suite con reporte incluido
.\gradlew testAll allureReport
```

> **Regla:** `@Destructive` NUNCA contra produccion. Solo en ambiente TEST con
> datos de prueba controlados. Ver
> [docs/02-governance/GESTION_DE_ENTORNOS.md](docs/02-governance/GESTION_DE_ENTORNOS.md).
>
> **Por que secuencial:** los tests destructivos suelen depender de un estado especifico
> del sistema. En paralelo podrian colisionar entre si o con otros tests.

---

## 11. Modo debug y verbose

```powershell
# Ver el output de cada test en consola (logs de SLF4J + resumen Cucumber)
.\gradlew test "-Dcucumber.filter.tags=@Smoke" `
    "-Dcucumber.execution.parallel.enabled=false" `
    --info

# Ver el stack trace completo de cada falla (util para bugs en BasePage)
.\gradlew test "-Dcucumber.filter.tags=@Smoke" `
    "-Dcucumber.execution.parallel.enabled=false" `
    --stacktrace

# Modo mas verboso (incluye debug de Gradle, muy ruidoso)
.\gradlew test "-Dcucumber.filter.tags=@Smoke" --debug

# Dry run — parsea los features y verifica que los steps esten implementados
# SIN abrir ningun navegador (util para verificar que un feature nuevo esta bien escrito)
.\gradlew test "-Dcucumber.filter.tags=@Smoke" `
    "-Dcucumber.execution.dry-run=true" `
    "-Dcucumber.execution.parallel.enabled=false"
```

> **Dry run:** ejecuta el feature sin hacer nada en el navegador. Si un step no tiene
> implementacion, aparece como "undefined". Util para verificar un feature nuevo antes
> de escribir los steps.

---

## 12. Tareas de calidad de codigo

```powershell
# Verificar estilo de codigo con Checkstyle (obligatorio antes de MR)
.\gradlew checkstyleMain

# Aplicar formateo automatico con Spotless (corrige el formato del codigo Java)
.\gradlew spotlessApply

# Verificar que el codigo esta bien formateado (sin corregir, solo reportar)
.\gradlew spotlessCheck

# Generar reporte de cobertura JaCoCo (despues de correr los tests)
.\gradlew test jacocoTestReport
# Reporte en: build/jacoco/html/index.html

# Generar JavaDoc de las clases de test
.\gradlew testJavadoc
# JavaDoc en: build/docs/test-javadoc/index.html

# Validacion completa: Checkstyle + Spotless + Tests + JaCoCo
.\gradlew validateCode
```

---

## 13. Catalogo completo de tasks Gradle

```powershell
# Ver todos los tasks disponibles en el proyecto
.\gradlew tasks

# Ver los tasks con descripcion completa
.\gradlew tasks --all
```

### Resumen de tasks propios del proyecto

| Task | Que hace | Cuando usarlo |
|---|---|---|
| `clean` | Borra la carpeta `build/` completa | Antes de un run limpio |
| `compileTestJava` | Compila el codigo de tests | Verificar que compila sin correr tests |
| `test` | Corre la suite principal (excluye @Destructive) | Trabajo diario, CI/CD |
| `testDestructive` | Corre solo @Destructive en secuencial | Al final del pipeline o aislado |
| `testAll` | Corre `test` + `testDestructive` de forma explícita | Pipeline completo con opt-in destructivo |
| `allureReport` | Genera el reporte HTML de Allure | Despues de cualquier `test` run |
| `allureServe` | Levanta servidor Allure en vivo | Para revisar resultados rapidamente |
| `checkstyleMain` | Verifica estilo con Checkstyle | Antes de hacer commit o MR |
| `spotlessApply` | Formatea el codigo automaticamente | Antes de hacer commit |
| `spotlessCheck` | Verifica el formato sin corregir | En CI para detectar formato incorrecto |
| `jacocoTestReport` | Genera reporte de cobertura | Despues de tests (opcional) |
| `testJavadoc` | Genera JavaDoc HTML de los tests | Documentacion (opcional) |
| `validateCode` | Checkstyle + Spotless + Tests + JaCoCo | Quality gate completo antes de MR |

---

## 14. Errores comunes al ejecutar

### El comando no se reconoce (`gradlew` no encontrado)

```
Error: The term '.\gradlew' is not recognized...
```
**Causa:** estas ejecutando desde un directorio que no es `qa-automation/`.
```powershell
# Ir al directorio correcto primero
cd D:\AUTOMATIZACION\FRAMEWORK-AUTOMATIZADOS\plantilla-devsecops\qa-automation
.\gradlew clean compileTestJava
```

---

### Los tags con espacios fallan

```powershell
# MAL — PowerShell interpreta los espacios dentro de la cadena
.\gradlew test -Dcucumber.filter.tags=@Smoke and not @WIP

# BIEN — encerrar el valor en comillas dobles dentro de comillas dobles
.\gradlew test "-Dcucumber.filter.tags=@Smoke and not @WIP"
```

---

### Chrome no abre (ChromeDriver error)

```
SessionNotCreatedException: Could not start a new session...
```
**Causas frecuentes:**
- Chrome no esta instalado o la version no es compatible
- WebDriverManager no pudo descargar el driver (sin internet en el runner)

```powershell
# Forzar que WebDriverManager use el Chrome del sistema sin descargar nada
$env:WDM_CHROMEDRIVER_PATH="C:\Path\Al\Chromedriver\chromedriver.exe"
.\gradlew test "-Dcucumber.filter.tags=@Smoke"
```

---

### El rerun.txt esta vacio o no existe

```powershell
# Si no existe build/rerun.txt, significa que el run anterior no tuvo fallas
# O que el build fue limpiado con .\gradlew clean
# Correr la suite normalmente primero para que se genere
.\gradlew test "-Dcucumber.filter.tags=@Regression"
# Ahora si existe build/rerun.txt con los fallidos (si los hubo)
```

---

### Allure dice "No results found" en el reporte

```powershell
# Los resultados de Allure se generan durante el test run en build/allure-results/
# Si limpias con .\gradlew clean ANTES de generar el reporte, pierdes los resultados
# Flujo correcto:
.\gradlew test allureReport  # en un solo comando para no perder resultados
# O si ya corriste el test:
.\gradlew allureReport       # sin clean entre medio
```

---

### Demasiados tests en paralelo (Chrome crashea)

```
WebDriverException: chrome not reachable
```
**Causa:** mas hilos de los que la maquina puede sostener.
```powershell
# Reducir a 2 hilos
.\gradlew test "-Dcucumber.filter.tags=@Smoke" `
    "-Dcucumber.execution.parallel.config.fixed.parallelism=2"
```

---

## Combinaciones tipicas del dia a dia

```powershell
# Flujo de desarrollo local (nuevo escenario)
.\gradlew compileTestJava
.\gradlew test "-Dcucumber.filter.name=Mi Nuevo Escenario" "-Dcucumber.execution.parallel.enabled=false"

# Flujo pre-commit (verificar que no rompi nada)
.\gradlew spotlessApply
.\gradlew test "-Dcucumber.filter.tags=@Smoke" "-Dcucumber.execution.parallel.enabled=false"

# Flujo pre-MR (validacion completa)
.\gradlew clean
.\gradlew spotlessApply checkstyleMain
.\gradlew test "-Dcucumber.filter.tags=@Regression"
.\gradlew allureReport

# Flujo de investigacion de fallas (tras un pipeline rojo)
.\gradlew test "-Dcucumber.features=@build/rerun.txt" "-Dcucumber.execution.parallel.enabled=false" --info

# Flujo nightly completo (replica del pipeline)
.\gradlew clean test testDestructive allureReport
```

---

*Guia v2.0 — QA Automation QA Automatizacion — 2026-03-15*
*Actualizar cuando se agreguen nuevos tasks a build.gradle o nuevos tags al proyecto.*
