# Paralelismo y Escalabilidad — De 10 tests a miles sin romper nada

> Este documento tiene cuatro niveles de lectura según tu momento en automatización:
> - **Nivel Inicial (secciones 1–4):** qué es, por qué existe, qué no debes hacer
> - **Nivel Operativo (secciones 5–7):** cómo funciona internamente, qué configuras
> - **Nivel Avanzado (secciones 8–10):** fórmulas, configuración, diagnóstico avanzado
> - **QA Arquitectura (secciones 11–16):** escala bancaria real, infraestructura, hoja de ruta

---

## Nivel Inicial

---

## 1. ¿Qué es el paralelismo y para qué sirve?

Imagina que eres el único cajero en un banco.
Atiendes a un cliente, lo terminas, llamas al siguiente.
Si cada atención toma 5 minutos y tienes 10 clientes: 50 minutos.

Ahora imagina que el banco abre 3 ventanillas al mismo tiempo.
Los 3 cajeros atienden clientes en paralelo.
Los mismos 10 clientes: ~20 minutos. Casi 3 veces más rápido.

**En este framework:**

```
Sin paralelo (1 hilo):   Test1 → Test2 → Test3 → Test4 → Test5   = 50 seg
Con paralelo (3 hilos):  Test1  Test2  Test3
                                       Test4  Test5               = ~20 seg
```

Con `parallelism=3`, hasta 3 escenarios corren al mismo tiempo.
El tiempo total es el del escenario más largo, no la suma de todos.

---

## 2. ¿Qué es un "hilo"?

Un hilo (thread) es como un cajero: un trabajador independiente que ejecuta
instrucciones. Tu computadora puede tener varios corriendo al mismo tiempo.

Cada hilo en este framework:
- Tiene su propio Chrome abierto
- Ejecuta un escenario completo de principio a fin
- No sabe qué están haciendo los otros hilos
- Termina → cierra su Chrome → queda libre para el siguiente escenario

```
Hilo 1: [Abre Chrome #1] → [Feature: Login] → [Cierra Chrome #1]
Hilo 2: [Abre Chrome #2] → [Feature: Búsqueda] → [Cierra Chrome #2]
Hilo 3: [Abre Chrome #3] → [Feature: Transferencia] → [Cierra Chrome #3]
```

Los tres ocurren al mismo tiempo, en ventanas separadas de Chrome.

---

## 3. La regla más importante para quien empieza en automatización

**Cada escenario debe poder correr solo, sin depender de otro.**

Si el escenario A crea un usuario y el escenario B usa ese usuario,
en paralelo pueden ejecutarse al revés: B intenta usar el usuario
antes de que A lo haya creado → B falla aunque el código esté bien.

```
CORRECTO — escenarios independientes:
  Scenario: Login con usuario demo_01        ← tiene sus propios datos
  Scenario: Búsqueda por DNI "12345678"      ← tiene sus propios datos
  Scenario: Consulta saldo cuenta "001-xxx"  ← tiene sus propios datos

INCORRECTO — escenarios dependientes:
  Scenario: Crear cliente con DNI "99999999"    ← escenario A
  Scenario: Buscar el cliente recién creado      ← depende de A → ROTO en paralelo
```

**Regla práctica:** Si para escribir un escenario necesitas que otro haya
corrido primero, tienes un problema de diseño, no de código.

---

## 4. ¿Qué es `@Destructive` y por qué existe?

Algunos escenarios crean, modifican o eliminan datos en el sistema.
Por ejemplo: registrar una nueva cuenta, procesar un castigo, eliminar un registro.

Si estos corren en paralelo pueden pisarse entre sí:
- El hilo 1 crea el registro X
- El hilo 2, al mismo tiempo, intenta crear el mismo registro X
- El sistema rechaza el duplicado → el test falla aunque el código esté bien

Por eso existe el tag `@Destructive`. Los escenarios con este tag:
1. **No corren durante la fase paralela principal**
2. **Corren al final, uno por uno (secuencial)**, cuando todos los demás terminaron

```
Fase 1 — Paralelo (3 hilos simultáneos):
  ✓ Todos los escenarios sin @Destructive

Fase 2 — Secuencial (1 hilo, uno tras otro):
  ✓ Solo los escenarios @Destructive
```

Esto está implementado en `build.gradle` con dos tasks:
- `test` → corre todo excepto `@Destructive` en paralelo
- `testDestructive` → corre solo `@Destructive` secuencial, siempre después de `test`

**Cuándo usar `@Destructive`:**
- El escenario crea, modifica o elimina un dato que otros tests podrían necesitar
- El escenario deja el sistema en un estado diferente al inicial
- El escenario no tiene una forma simple de "limpiar" lo que hizo

---

## 4.1 Configuración de tu laptop — sin hacer cuentas

Antes de correr cualquier test, verifica cuánta RAM tiene tu equipo
y usa la configuración correspondiente. Sin más cálculos.

**¿Cuánta RAM tengo?**
- Windows: `Win + R` → `msinfo32` → "Memoria RAM instalada"
- O en la terminal: `wmic memorychip get capacity`

**Usa esta tabla directamente:**

| RAM de tu laptop | parallelism local | Modo | Config en `.env` |
|---|---|---|---|
| 4 GB o menos | 1 (secuencial) | Con ventana | `BROWSER_HEADLESS=false` |
| 6 GB | 1 | Con ventana | `BROWSER_HEADLESS=false` |
| 8 GB | 2 | Con ventana | `BROWSER_HEADLESS=false` |
| 12 GB | 2 | Con ventana o headless | `BROWSER_HEADLESS=false` |
| 16 GB | 3 | Con ventana | `BROWSER_HEADLESS=false` |
| 16 GB + | 3 | Con ventana | `BROWSER_HEADLESS=false` |

> No subas de parallelism=3 en tu laptop aunque tengas 32 GB.
> A partir de ese punto la ganancia de velocidad es marginal
> y el riesgo de inestabilidad no vale la pena localmente.
> Para más paralelismo está el CI.

**Comando para verificar que tu configuración es estable:**

```bash
# Corre el Smoke 2 veces seguidas. Si ambas pasan, tu laptop está bien configurada.
./gradlew test -Dcucumber.filter.tags="@Smoke"
./gradlew test -Dcucumber.filter.tags="@Smoke"
```

Si la primera pasa y la segunda falla → tu laptop tiene problemas de recursos.
Baja el parallelism en 1 y repite la prueba.

---

## Nivel Operativo

---

## 5. Cómo funciona internamente: ThreadLocal

Aquí está el mecanismo que hace posible el paralelismo seguro.

**El problema:** Selenium necesita un `WebDriver` (la conexión con Chrome).
Si los 3 hilos usan el mismo `WebDriver`, se pisarían: el hilo 1 navega a Login,
el hilo 2 hace click en Buscar sobre la misma ventana. Caos total.

**La solución: ThreadLocal**

`ThreadLocal<WebDriver>` es como un casillero con llave donde cada hilo
guarda su propio driver. Cada hilo solo puede acceder a su propio casillero.

```
ThreadLocal — visualización:

              ┌──────────────────────────────────────────────┐
              │           ThreadLocal<WebDriver>             │
              │                                              │
  Hilo 1 ────►│  [llave-hilo-1] → ChromeDriver instancia A  │
  Hilo 2 ────►│  [llave-hilo-2] → ChromeDriver instancia B  │
  Hilo 3 ────►│  [llave-hilo-3] → ChromeDriver instancia C  │
              │                                              │
              └──────────────────────────────────────────────┘

Hilo 1 llama getDriver() → recibe instancia A (solo la suya)
Hilo 2 llama getDriver() → recibe instancia B (solo la suya)
Hilo 3 llama getDriver() → recibe instancia C (solo la suya)
```

**En el código real** (`DriverFactory.java` línea 36):

```java
// Una sola variable estática que internamente tiene N entradas (una por hilo)
private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

// Guardar: asocia el driver al hilo que llama a este método
driverThreadLocal.set(new ChromeDriver(options));  // línea 130

// Leer: cada hilo lee solo su propio driver
return driverThreadLocal.get();  // línea 142

// Limpiar: elimina la entrada del hilo cuando termina el escenario
driverThreadLocal.remove();  // línea 165
```

**El ciclo de vida completo por escenario:**

```
@Before (Hooks.java)
    │
    ├─ DriverFactory.initDriver()   → crea ChromeDriver → guarda en ThreadLocal[hilo-N]
    │
    Steps del escenario
    │  BasePage() obtiene driver via DriverFactory.getDriver() → ThreadLocal[hilo-N]
    │  Todos los métodos del PageObject usan ese mismo driver del hilo
    │
@After (Hooks.java)
    │
    ├─ Captura screenshot
    └─ DriverFactory.quitDriver()   → driver.quit() + ThreadLocal.remove()
```

---

## 6. El lock del WebDriverManager — por qué existe

`DriverFactory.java` tiene este bloque (líneas 78–80):

```java
synchronized (WDM_LOCK) {
    WebDriverManager.chromedriver().setup();
}
```

**¿Por qué?**

Cuando 3 hilos arrancan casi al mismo tiempo, los 3 llaman a `setup()` a la vez.
`setup()` descarga el ChromeDriver si no está en caché.
Si los 3 intentan descargarlo simultáneamente → corrupción del archivo o error de red.

`synchronized` fuerza que solo un hilo ejecute ese bloque a la vez.
El segundo y tercer hilo esperan afuera hasta que el primero termine.

```
Arranque simultáneo (3 hilos):

  Hilo 1: synchronized → setup() → descarga chromedriver → sale del lock
  Hilo 2: espera... espera... entra al lock → setup() → "ya está en caché, nada que hacer" → sale
  Hilo 3: espera... espera... entra al lock → setup() → "ya está en caché, nada que hacer" → sale
```

Solo la primera vez es lenta. Las siguientes son instantáneas (lectura de caché).
La creación del ChromeDriver (después del lock) sí es paralela y rápida.

---

## 7. Qué rompe el paralelismo — errores que debes evitar

### Error 1: Variables estáticas mutables en pages o steps

```java
// ROMPE EL PARALELISMO — variable estática compartida entre hilos
public class LoginPage extends BasePage {
    private static String ultimoUsuarioLogueado;  // ← compartida entre hilos

    public void login(String usuario) {
        ultimoUsuarioLogueado = usuario;  // hilo 1 escribe "user_a"
                                          // hilo 2 escribe "user_b" al mismo tiempo
                                          // hilo 1 lee "user_b" → bug silencioso
    }
}

// CORRECTO — variable de instancia, cada hilo tiene su propia instancia
public class LoginPage extends BasePage {
    private String ultimoUsuarioLogueado;  // cada hilo tiene su propia LoginPage
}
```

**Regla:** Si una variable en un Page Object o Step es `static` y puede cambiar
durante la ejecución → es un bug de paralelismo esperando ocurrir.

### Error 2: Compartir datos entre escenarios vía archivos

```java
// ROMPE EL PARALELISMO — dos hilos escriben al mismo archivo
FileWriter fw = new FileWriter("resultados.txt", true);
fw.write("Escenario X pasó");  // hilo 1 y hilo 2 escriben al mismo tiempo → corrupción
```

Si necesitas acumular datos entre escenarios, usa `AtomicInteger` o `ConcurrentHashMap`
(como lo hace `TestExecutionSummary.java` con `AtomicInteger`).

### Error 3: Escenarios con orden implícito

```gherkin
# INCORRECTO — Scenario B depende de que A haya corrido antes
@Smoke
Scenario: A — Crear usuario de prueba con DNI 77777777
  ...

@Smoke
Scenario: B — Verificar que el usuario 77777777 existe en el sistema
  ...  ← en paralelo puede correr antes que A
```

### Error 4: Asumir que el orden en el feature es el orden de ejecución

En paralelo, los escenarios pueden ejecutarse en cualquier orden.
Nunca diseñes un flujo donde el escenario 3 necesita que el 1 y 2 hayan corrido.

---

## Nivel Senior

---

## 8. Los 4 controles de configuración que debes conocer

Todo está en `src/test/resources/junit-platform.properties`:

```properties
# ── Control 1: Encender/apagar el paralelismo ────────────────────────────────
cucumber.execution.parallel.enabled=true
# → false: todo secuencial (útil para depurar)

# ── Control 2: Estrategia de asignación de hilos ────────────────────────────
cucumber.execution.parallel.config.strategy=fixed
# → fixed:    N hilos exactos (predecible, recomendado para CI)
# → dynamic:  JUnit decide según CPU disponible (impredecible en CI)

# ── Control 3: Número de hilos simultáneos ───────────────────────────────────
cucumber.execution.parallel.config.fixed.parallelism=3
# → Este es el número de Chromes que corren al mismo tiempo
# → Ver sección 9 para calcular el valor correcto

# ── Control 4: Qué corre en paralelo vs secuencial ──────────────────────────
cucumber.execution.parallel.execution-mode.default=CONCURRENT
# → CONCURRENT: escenarios en paralelo (default, lo que queremos)
# → SAME_THREAD: escenario en el mismo hilo que su feature (legacy, no usar)
```

**Regla de oro:** `junit-platform.properties` define el contrato base del framework.
Para cambiar el parallelism temporalmente (CI, debug), usa `-D` en el comando:

```bash
# Sobrescribir sin tocar el archivo:
./gradlew test -Dcucumber.execution.parallel.config.fixed.parallelism=5
./gradlew test -Dcucumber.execution.parallel.enabled=false
```

---

## 9. La fórmula de escalabilidad — de 10 a 100 tests

### La ecuación fundamental

```
RAM requerida = (N_hilos × RAM_Chrome) + JVM_overhead + 20% buffer

Donde:
  N_hilos     = valor de parallelism
  RAM_Chrome  = 200 MB en headless / 400–600 MB con ventana
  JVM_overhead = 800 MB fijos (JVM + Gradle + Allure + framework)
  buffer 20%  = margen para picos de carga
```

### Tabla de referencia para QA Automation

| Ambiente | parallelism | Modo Chrome | RAM mínima | Tiempo típico (50 tests) | Cuándo usar |
|---|---|---|---|---|---|
| Local desarrollo | 2 | Con ventana | 2.5 GB | ~4 min | Escribiendo y depurando |
| Local estable | 3 | Con ventana | 3.2 GB | ~3 min | Ejecución diaria local |
| CI — runner estándar | 3 | Headless | 1.8 GB | ~3 min | Pipeline de MR |
| CI — runner dedicado | 5 | Headless | 2.8 GB | ~2 min | Pipeline staging |
| CI — nightly regression | 8 | Headless | 4.4 GB | ~1.5 min | Suite nocturna completa |
| Máximo absoluto | 12 | Headless | 6.4 GB | ~1 min | Solo si el runner tiene 8+ GB |

> **Nunca superes parallelism=12 con Selenium.**
> Más hilos no significa más velocidad pasado ese punto: Chrome empieza a competir
> por CPU y los tiempos de respuesta del DOM aumentan. Los tests se vuelven
> más lentos Y más inestables al mismo tiempo.

### Cálculo práctico para tu caso

**Escenario:** Tienes 100 tests, cada uno tarda ~8 segundos. ¿Cuánto tardará con parallelism=5?

```
Tiempo sin paralelo:  100 tests × 8 seg = 800 seg (~13 min)
Tiempo con 5 hilos:   800 seg / 5 hilos = 160 seg (~2.7 min)

RAM necesaria: (5 × 200 MB headless) + 800 MB JVM + 20% buffer
             = 1000 MB + 800 MB + 360 MB
             = 2.16 GB → pide al menos 3 GB en el runner de GitLab
```

### El proceso correcto para escalar (no saltarte pasos)

```
PASO 1 — Verificar independencia de escenarios
  Corre la suite completa en secuencial y confirma que todos pasan:
  ./gradlew test -Dcucumber.execution.parallel.enabled=false

PASO 2 — Escalar gradualmente, no de golpe
  Empieza con parallelism=3 (ya configurado)
  → Si es estable: sube a 5
  → Si es estable: sube a 8
  → Nunca saltes de 3 a 12 directamente

PASO 3 — Validar flakiness en cada nivel
  Después de subir el parallelism, corre la suite 3 veces seguidas.
  Si el mismo escenario falla en 1 de 3 runs → es flaky, investiga antes de continuar.

PASO 4 — Actualizar el runner de GitLab antes de subir el parallelism en CI
  En .gitlab/ci/qa.yml, ajusta el parallelism y coordina con el sysadmin
  para confirmar que el runner tiene la RAM necesaria:
  -Dcucumber.execution.parallel.config.fixed.parallelism=5

PASO 5 — Ajustar @Destructive
  Con 100 tests es probable que tengas más escenarios @Destructive.
  Estos siempre corren secuenciales → planifica que la fase 2 puede tomar
  N × 8 seg donde N es el número de escenarios @Destructive.
```

---

## 10. Diagnóstico de fallos en paralelo

### El síntoma más común: "pasa en secuencial, falla en paralelo"

Este síntoma tiene tres causas posibles:

**Causa 1: Estado compartido entre escenarios**
```bash
# Reproduce en secuencial para confirmar que pasa
./gradlew test -Dcucumber.filter.tags="@NombreTag" -Dcucumber.execution.parallel.enabled=false
# Si pasa → el test en sí está bien. El problema es concurrencia.
# Busca variables static mutables en el PageObject o Steps del escenario
```

**Causa 2: Datos que se solapan entre escenarios**
Dos escenarios usan el mismo usuario/cuenta/registro.
En secuencial el primero termina antes de que empiece el segundo.
En paralelo los dos intentan usar el mismo dato al mismo tiempo.

Solución: cada escenario usa datos únicos. Si no puedes cambiar los datos,
marca el escenario como `@Destructive` para forzar ejecución secuencial.

**Causa 3: El ambiente no aguanta la carga**
El servidor del ambiente TEST tiene sus propios límites.
Si 3 hilos abren sesión al mismo tiempo y el servidor solo aguanta 1 sesión
concurrente → 2 fallan.

```bash
# Verificar: corre con parallelism=1 (no secuencial total, solo 1 hilo)
./gradlew test -Dcucumber.execution.parallel.config.fixed.parallelism=1
# Si pasa → el código está bien. El problema es el servidor del ambiente.
# Reportar al equipo de infraestructura.
```

### El árbol de diagnóstico completo

```
Escenario falla solo en paralelo
    │
    ├─ ¿Pasa con parallel.enabled=false? (secuencial total)
    │       │
    │       ├─ NO (falla en secuencial también) → el bug no es de paralelismo
    │       │                                     es un bug funcional normal
    │       │
    │       └─ SÍ (pasa en secuencial, falla en paralelo)
    │               │
    │               ├─ ¿Hay variables static en el PageObject o Steps?
    │               │       └─ SÍ → cambiarlas a variables de instancia
    │               │
    │               ├─ ¿Comparte datos con otro escenario?
    │               │       └─ SÍ → usar datos únicos por escenario
    │               │              o agregar tag @Destructive
    │               │
    │               └─ ¿Falla con parallelism=1 también?
    │                       └─ SÍ → el servidor no aguanta concurrencia
    │                              reportar a infraestructura
    │                       └─ NO → race condition en locators/waits
    │                              revisar con basePage.fastWait en lugar de wait
```

### Comandos de diagnóstico de referencia rápida

```bash
# 1. Aislar el escenario problemático en secuencial puro
./gradlew test \
  -Dcucumber.filter.tags="@NombreTag" \
  -Dcucumber.execution.parallel.enabled=false

# 2. Correr con solo 1 hilo (paralelo pero sin concurrencia real)
./gradlew test \
  -Dcucumber.filter.tags="@NombreTag" \
  -Dcucumber.execution.parallel.config.fixed.parallelism=1

# 3. Correr N veces para detectar flakiness (bash loop)
for i in 1 2 3; do
  ./gradlew test -Dcucumber.filter.tags="@NombreTag"
done

# 4. Ver qué escenarios fallaron y requieren rerun
cat build/rerun.txt

# 5. Rerun solo los fallidos (sin re-ejecutar toda la suite)
./gradlew test -Dcucumber.features="@build/rerun.txt" \
  -Dcucumber.execution.parallel.enabled=false
```

---

## Resumen por nivel

| Concepto | Nivel Inicial | Nivel Operativo | Nivel Avanzado |
|---|---|---|---|
| ¿Qué es un hilo? | Un cajero que atiende un cliente | Un worker con su propio Chrome | Un worker con ThreadLocal aislado |
| ¿Por qué paralelo? | Más rápido | Reducir tiempo de pipeline | Optimizar throughput con RAM disponible |
| ¿Qué NO hacer? | No crear dependencia entre tests | No usar `static` mutable | No superar parallelism=12 |
| ¿Cómo configurar? | No toco nada, el capacitador configuró | `-Dcucumber.execution.parallel.config.fixed.parallelism=N` | Calcular con fórmula RAM y ajustar runner de GitLab |
| ¿Cómo depurar? | Corro en secuencial y le aviso al capacitador | `parallel.enabled=false` vs `parallelism=1` | Árbol de diagnóstico + análisis de static vs datos compartidos |

---

---

## Nivel Arquitecto

---

## 11. ¿Cuántos tests tienen las aplicaciones bancarias reales?

Esta es la pregunta que todo QA senior debe poder responder cuando le digan
"escala la suite a producción".

### Números reales por tipo de aplicación

| Tipo de aplicación | @Smoke | @Regression | Total | Tiempo en CI (parallelism=8) |
|---|---|---|---|---|
| App interna pequeña (admin, reportes) | 20–50 | 80–200 | 100–250 | 3–8 min |
| App de gestión (castigos, créditos) | 50–150 | 200–500 | 250–650 | 8–20 min |
| Portal de clientes (web/app) | 100–300 | 400–900 | 500–1200 | 15–35 min |
| Core bancario (sistema central) | 300–800 | 1000–3000 | 1300–3800 | Por módulo |
| Suite completa multi-sistema | — | — | 5000–15000 | Grid distribuido |

**Para QA Automation — Sample Application Automatizados (aplicación actual):**
- Horizonte v1.0 (primer año): 50–150 tests críticos es un objetivo realista y sólido
- Horizonte v2.0 (segundo año): 300–600 con cobertura completa de todos los flujos
- No hay prisa por llegar a 1000. 150 tests bien escritos valen más que 1000 inestables

### La regla del 20/80 en banca

En sistemas financieros, el 20% de los flujos genera el 80% del riesgo:
- Autenticación y sesión
- Operaciones que mueven dinero (transferencias, pagos, castigos)
- Cierres y liquidaciones
- Accesos por rol (qué puede ver cada perfil)

Esos flujos críticos deben tener cobertura @Smoke del 100% antes de
preocuparte por automatizar flujos secundarios.

---

## 12. Los tres niveles de ejecución — dónde corre cada cosa

La clave para entender la escalabilidad es que **la máquina del QA nunca
es el lugar donde corre la suite completa**. Hay tres niveles:

```
NIVEL 1 — Máquina del QA (local)
  ¿Qué corre aquí?  Solo lo que estás desarrollando ahora mismo
  ¿Cuántos tests?   5–30 tests máximo
  ¿Parallelism?     2–3 (no más, sin headless si debug)
  ¿Cuándo?          Mientras escribes el feature/steps/page

NIVEL 2 — Runner de CI (GitLab runner compartido)
  ¿Qué corre aquí?  @Smoke en cada MR, @Regression en staging
  ¿Cuántos tests?   50–500 tests
  ¿Parallelism?     5–8 headless
  ¿Cuándo?          Automático, en cada push al repositorio

NIVEL 3 — Grid o Cloud (infraestructura dedicada)
  ¿Qué corre aquí?  Suite completa, nightly, multi-browser
  ¿Cuántos tests?   500–5000+ tests
  ¿Parallelism?     20–100+ nodos simultáneos
  ¿Cuándo?          Pipeline nocturno o pre-release
```

**Implicación directa para el QA:**
Tu laptop nunca debe tardar más de 5 minutos corriendo tests.
Si tarda más, estás corriendo demasiado localmente. Delega al CI.

---

## 13. Qué hace el QA en su máquina a medida que la suite crece

Esto es lo que cambia en la práctica del QA día a día según el tamaño de la suite:

### Etapa 1 — Suite de 10 a 50 tests

```bash
# Corres todos los @Smoke antes de hacer push (dura ~2-3 min)
./gradlew test -Dcucumber.filter.tags="@Smoke"

# Corres solo tu feature nuevo mientras lo desarrollas
./gradlew test -Dcucumber.filter.tags="@MiFeature"
```

El QA puede correr toda la suite de Smoke en su laptop sin problema.
Parallelism=3 es suficiente.

### Etapa 2 — Suite de 50 a 200 tests

```bash
# Ya NO corres @Regression completo en tu laptop. Solo Smoke.
./gradlew test -Dcucumber.filter.tags="@Smoke"

# Para verificar tu feature específico + los relacionados
./gradlew test -Dcucumber.filter.tags="@Smoke and @MiModulo"

# La suite @Regression la corre el CI, no tú
# Push → GitLab corre @Regression en el runner
```

El QA empieza a confiar en el CI para la validación completa.
Solo verifica su trabajo localmente, no toda la suite.

### Etapa 3 — Suite de 200 a 500 tests

```bash
# Local: SOLO el escenario en el que trabajas hoy
./gradlew test -Dcucumber.filter.tags="@REQ-042"

# O solo los @Smoke del módulo que tocaste
./gradlew test -Dcucumber.filter.tags="@Smoke and @Autenticacion"

# Nunca corres @Regression local. Siempre en CI.
```

**Cambio de mentalidad importante:** el QA deja de validar todo localmente.
Su responsabilidad es que su escenario corra en su máquina.
El CI valida la integración con el resto.

### Etapa 4 — Suite de 500 a 1000+ tests

```bash
# Local: escenario exacto en desarrollo (sin paralelo para debug)
./gradlew test \
  -Dcucumber.filter.tags="@REQ-042" \
  -Dcucumber.execution.parallel.enabled=false

# Verificación rápida pre-push (solo @Smoke críticos del módulo)
./gradlew test -Dcucumber.filter.tags="@Smoke and @CriticoTransaccional"
# ~10-15 tests, ~2 min
```

El runner de CI ya no alcanza a correr todo en un tiempo razonable con parallelism=8.
Aquí entra el siguiente nivel de infraestructura.

---

## 14. Qué comprar o pedir según el tamaño de tu suite

### Opción A — Runner propio en servidor on-premise (hasta 500 tests)

Para QA Automation con infraestructura propia, esta es la ruta más económica y controlable:

**Servidor GitLab Runner dedicado:**

| Escenario | CPU | RAM | Disco | parallelism | Tests en ~15 min |
|---|---|---|---|---|---|
| Suite pequeña (hasta 150 tests) | 4 vCPU | 8 GB | 50 GB SSD | 5 | ~150 @Regression |
| Suite mediana (hasta 350 tests) | 8 vCPU | 16 GB | 100 GB SSD | 8 | ~350 @Regression |
| Suite grande (hasta 600 tests) | 16 vCPU | 32 GB | 200 GB SSD | 12 | ~600 @Regression |

**Configuración del runner en `.gitlab-ci.yml`:**

```yaml
qa:regression:
  tags:
    - qa-runner-dedicado    # Etiqueta del runner con más recursos
  variables:
    PARALLELISM: "8"
  script:
    - ./gradlew test
        -Dcucumber.execution.parallel.config.fixed.parallelism=${PARALLELISM}
        -Dcucumber.filter.tags="@Regression and not @Destructive"
```

**Costo estimado en Perú (servidor físico o VM en datacenter propio):**
- Servidor para 150 tests: S/. 3,000–8,000 (VM en infraestructura existente)
- Servidor para 600 tests: S/. 15,000–30,000 (servidor dedicado)
- Mantenimiento: coordinación con el área de infraestructura de la Caja

---

### Opción B — Selenium Grid propio (500 a 2000 tests)

Cuando un solo runner no alcanza, se usa **Selenium Grid**: una máquina Hub que
distribuye los tests entre N máquinas Node, cada una corriendo sus propios Chromes.

```
Arquitectura Selenium Grid:

  GitLab Runner
       │
       │ ./gradlew test (con Remote WebDriver apuntando al Hub)
       ▼
  ┌─────────────────────────────────────────────────────┐
  │              SELENIUM GRID HUB                       │
  │         selenium/hub:4.18.1                          │
  └────────┬──────────────┬──────────────┬──────────────┘
           │              │              │
           ▼              ▼              ▼
  ┌────────────┐  ┌────────────┐  ┌────────────┐
  │  Node 1    │  │  Node 2    │  │  Node 3    │
  │ Chrome ×4  │  │ Chrome ×4  │  │ Chrome ×4  │
  │ 4 GB RAM   │  │ 4 GB RAM   │  │ 4 GB RAM   │
  └────────────┘  └────────────┘  └────────────┘
       4 tests         4 tests         4 tests
       al mismo        al mismo        al mismo
       tiempo          tiempo          tiempo
                    = 12 tests simultáneos
```

**Para activar Selenium Grid en el framework:**

En `DriverFactory.java` agregarías un modo Remote:

```java
// Modo remoto (Grid) cuando se define la variable de entorno SELENIUM_HUB_URL
String hubUrl = System.getenv("SELENIUM_HUB_URL");
if (hubUrl != null && !hubUrl.isBlank()) {
    ChromeOptions options = new ChromeOptions();
    // ... mismas opciones headless
    driverThreadLocal.set(new RemoteWebDriver(new URL(hubUrl), options));
} else {
    // modo local actual (ChromeDriver directo)
    driverThreadLocal.set(new ChromeDriver(options));
}
```

**Hardware para Grid con 3 nodos (hasta ~800 tests en 15 min):**

| Componente | CPU | RAM | Cantidad | Costo estimado |
|---|---|---|---|---|
| Hub (coordinador) | 2 vCPU | 4 GB | 1 | VM pequeña |
| Node (ejecutor) | 4 vCPU | 8 GB | 3–5 | VM mediana × 3–5 |

---

### Opción C — Cloud Testing (1000+ tests, multi-browser, sin infraestructura propia)

Para suites grandes o cuando se necesita validar en múltiples navegadores:

| Plataforma | Capacidad | Ideal para | Costo aprox. |
|---|---|---|---|
| **BrowserStack Automate** | 1000+ tests paralelos | Multi-browser, mobile | USD 399–999/mes |
| **SauceLabs** | Escala infinita | Empresas grandes, compliance | USD 500–2000/mes |
| **LambdaTest** | Hasta 25 paralelos | Costo-beneficio para cajas | USD 99–399/mes |
| **AWS Device Farm** | Escalable | Si ya usan AWS | Pago por uso |

**Para QA Automation en el corto plazo:** Ninguna de estas es necesaria todavía.
Son opciones para cuando la suite supere los 500 tests con necesidad de multi-browser.

---

## 15. Hoja de ruta de infraestructura para QA Automation

```
HOY — v1.0 (0–150 tests)
  Infraestructura: Runner compartido de GitLab + laptops de QAs
  parallelism local: 3  |  parallelism CI: 5
  Acción: ninguna, la configuración actual es suficiente

6–12 MESES — v1.5 (150–400 tests)
  Infraestructura: Solicitar runner dedicado al área de infraestructura
  Specs mínimas: 8 vCPU / 16 GB RAM / 100 GB SSD
  parallelism CI: 8
  Acción: Coordinar con Joel y el área de infraestructura
          Actualizar parallelism en .gitlab/ci/qa.yml

12–24 MESES — v2.0 (400–800 tests)
  Infraestructura: Selenium Grid on-premise con 3 nodos
  Specs: Hub 2vCPU/4GB + 3 Nodes de 4vCPU/8GB cada uno
  parallelism efectivo: 12–16 (distribuido entre nodos)
  Acción: Propuesta de inversión con ROI calculado
          Modificar DriverFactory para soporte RemoteWebDriver

MÁS DE 2 AÑOS — v3.0 (800+ tests, multi-app)
  Infraestructura: Cloud testing (LambdaTest o BrowserStack)
  o Grid con más nodos según crecimiento
  Acción: Evaluar costo cloud vs mantenimiento on-premise
```

---

## 16. Lo que NUNCA debes hacer para "solucionar" la lentitud

Cuando la suite crece y se vuelve lenta, hay atajos tentadores que crean
problemas mayores:

| Tentación | Por qué NO hacerlo | Solución correcta |
|---|---|---|
| Subir parallelism a 20 en el laptop | RAM insuficiente → Chrome crashea → falsos negativos | Delegar al CI, no al laptop |
| Deshabilitar @Destructive para que corra en paralelo | Datos se corrompen entre tests → resultados inválidos | Optimizar los @Destructive, no eliminar la protección |
| Dividir la suite en archivos separados y correrlos manualmente | Inconsistencia de resultados, no hay reporte unificado | Usar Grid o aumentar nodos de CI |
| Agregar `Thread.sleep(5000)` para "estabilizar" | Suma 5 segundos × N tests = suite de 2 horas | Usar waits explícitos en BasePage |
| Deshabilitar tests inestables con `@Ignore` | La cobertura baja silenciosamente | Investigar y corregir la causa raíz |

---

---

## 17. Estrategia consolidada para el equipo de 6 QAs de QA Automation

Todo lo anterior en una sola hoja. Guárdala, imprímela, tenla a mano.

### La estrategia de hoy (v1.0 — hasta 150 tests)

```
CADA QA EN SU LAPTOP
  ├── Corre: solo el escenario que está escribiendo hoy
  ├── Comando: ./gradlew test -Dcucumber.filter.tags="@REQ-XXX"
  ├── parallelism: según tu RAM (ver tabla sección 4.1)
  └── NUNCA corre @Regression completo en su laptop

EL PIPELINE DE GITLAB (runner compartido)
  ├── Corre: @Smoke en cada MR (automático)
  ├── Corre: @Regression en cada push a staging (automático)
  ├── parallelism: 5 (headless automático)
  └── Nadie tiene que activarlo manualmente

EL CAPACITADOR / LÍDER QA
  ├── Monitorea: pass rate semanal en Allure
  ├── Reporta: KPIs mensuales al liderazgo
  └── Decide: cuándo pedir el runner dedicado (sección 15)
```

### La distribución de trabajo para los 6 QAs

Con 6 personas automatizando en paralelo, la suite crece rápido.
Para evitar conflictos y duplicación:

| Rol | Responsabilidad de automatización | Tests esperados por persona |
|---|---|---|
| QA 1 (tú — capacitador) | Arquitectura, estándares, flujos críticos de autenticación | 20–30 tests |
| QA 2 | Flujos de castigos — alta y registro | 20–30 tests |
| QA 3 | Flujos de castigos — consulta y búsqueda | 20–30 tests |
| QA 4 | Flujos de reportes y exportaciones | 15–25 tests |
| QA 5 | Flujos negativos y validaciones de formulario | 15–25 tests |
| QA 6 | Flujos de administración y configuración del sistema | 15–20 tests |
| **Total equipo** | | **~105–160 tests** |

Con esta distribución, en 6 semanas tienen una suite de Regression completa
para la primera versión de la aplicación.

### Las 3 reglas que el equipo completo debe saber de memoria

```
REGLA 1 — Mi escenario corre solo
  Cada escenario tiene sus propios datos. No depende de otro escenario.
  Si necesita datos específicos, los crea dentro del mismo escenario o usa
  datos fijos del ambiente de prueba.

REGLA 2 — Lo que destruye datos lleva @Destructive
  Si el escenario crea, modifica o elimina un registro del sistema,
  le agrego @Destructive. El framework lo protege automáticamente.

REGLA 3 — Mi laptop verifica mi trabajo. El CI verifica el equipo.
  En mi laptop solo corro lo que estoy desarrollando hoy.
  El pipeline corre la suite completa. Confío en el reporte de Allure del CI.
```

---

*Documento de paralelismo y escalabilidad v1.1 — QA Automation QA Automatización — 2026-03-15*
