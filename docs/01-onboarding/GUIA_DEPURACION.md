# Guía de depuración — Cómo leer errores y diagnosticar fallos

> Un test fallido no es un fracaso. Es información.
> Esta guía te enseña a leer esa información y actuar.

---

## Paso 0: Regla de los 2 minutos

Antes de pedir ayuda, dedica 2 minutos a leer el error completo.
El 80% de los fallos te dicen exactamente qué pasó si los lees con calma.

---

## Cómo leer un error en la terminal

Cuando un test falla, la terminal muestra algo así:

```
Scenario: Ingreso exitoso con credenciales válidas  # features/Demo.feature:14
  Given el usuario navega al portal               # DemoLoginSteps.java:43
  When ingresa usuario y clave válidos            # DemoLoginSteps.java:52
  And hace clic en el botón de ingresar           # DemoLoginSteps.java:60
    org.openqa.selenium.TimeoutException:         ← TIPO DE ERROR
    Expected condition failed: waiting for        ← QUÉ ESPERABA
    visibility of element located by              ← OPERACIÓN QUE FALLÓ
    By.cssSelector: button[type='submit']         ← EL LOCATOR QUE NO ENCONTRÓ
      (tried for 20 second(s) with 500 MILLISECONDS interval)
    at pages.BasePage.findVisible(BasePage.java:150) ← DÓNDE EN EL CÓDIGO
    at pages.LoginPage.clickBotonIngresar(LoginPage.java:95)
    at steps.DemoLoginSteps.haceClicEnBotonIngresar(DemoLoginSteps.java:62)
```

**Cómo leerlo de abajo hacia arriba:**
1. `DemoLoginSteps.java:62` → el step que disparó el error
2. `LoginPage.java:95` → el método del page object que falló
3. `BasePage.java:150` → el método de BasePage que lanzó la excepción
4. `By.cssSelector: button[type='submit']` → el locator que no encontró
5. `TimeoutException` → esperó 20 segundos y el elemento no apareció

---

## Los 5 tipos de error más comunes y cómo resolverlos

---

### Error 1: `TimeoutException` — El elemento no apareció

```
org.openqa.selenium.TimeoutException: Expected condition failed:
waiting for visibility of element located by By.id: "btnLogin"
(tried for 20 second(s)...)
```

**¿Qué significa?** Selenium esperó 20 segundos buscando el elemento y no lo encontró.

**Causas posibles (revisa en este orden):**

**a) El locator está mal** — lo más común
- Abre el sistema en Chrome
- Presiona F12 → Inspector (Elements)
- Busca el elemento en la pantalla
- Verifica que el ID/CSS/XPath del locator coincide exactamente

```
¿El ID en el código es "btnLogin"?
¿El HTML dice id="btnLogin" o id="btn_login" o id="loginBtn"?
```

**b) La página aún está cargando**
- El elemento existe pero la página no terminó de cargar
- Solución: el framework ya tiene waits automáticos de 20 segundos
- Si la página tarda más de 20 segundos, reporta el problema al equipo de dev

**c) La URL es incorrecta**
- Verifica que `TEST_BASE_URL` en tu `.env` apunta al ambiente correcto
- Pega la URL en el navegador manualmente y confirma que carga

**d) El ambiente está caído**
- Entra manualmente al sistema con tu usuario y contraseña
- Si no puedes entrar, el problema no es el test, es el ambiente

---

### Error 2: `NoSuchElementException` — Elemento inexistente en el DOM

```
org.openqa.selenium.NoSuchElementException:
no such element: Unable to locate element: {"method":"id","selector":"usuario"}
```

**¿Qué significa?** El elemento no existe en el HTML en absoluto.

**Causas posibles:**
- El ID cambió (el equipo de dev refactorizó el frontend)
- Estás buscando el elemento antes de que la página cargue
- La página que abriste no es la correcta

**Solución:**
1. Abre la página en Chrome
2. F12 → Ctrl+F en el inspector → escribe el ID o selector
3. Si Chrome no lo encuentra tampoco, el locator es incorrecto
4. Si Chrome lo encuentra, compara con exactitud el valor en el código

---

### Error 3: `StaleElementReferenceException` — Elemento obsoleto

```
org.openqa.selenium.StaleElementReferenceException:
stale element reference: element is not attached to the page document
```

**¿Qué significa?** El elemento existía, Selenium lo encontró, pero luego
el DOM cambió (Angular re-renderizó) y el elemento ya no es el mismo objeto.

**Buenas noticias:** `clickElement()` en BasePage ya tiene 3 reintentos automáticos
contra este error. Si lo ves en un `click`, es raro.

**Si aparece en otro método:**
- Usa `findVisible()` en lugar de guardar el elemento en una variable
- No hagas `WebElement el = find(...); doSomething(); el.click()` si hay código
  entre el find y la acción (el elemento puede cambiar entre medio)

---

### Error 4: `AssertionError` — La verificación falló

```
org.openqa.selenium.AssertionError:
[El panel principal del sistema debe ser visible tras el login exitoso]
Expected: true
     but: was false
```

**¿Qué significa?** El test llegó hasta la verificación final pero el resultado
no es el esperado. El sistema no se comportó como se describe en el feature.

**Este es el caso "feliz":** el test detectó un bug real.

**Qué hacer:**
1. Reproduce el caso manualmente en el navegador
2. Si el bug se reproduce manualmente → reporta el defecto
3. Si manualmente funciona → el locator del elemento de verificación está mal
   (el panel existe pero con otro selector)

---

### Error 5: `SessionNotCreatedException` — Chrome no arrancó

```
org.openqa.selenium.SessionNotCreatedException:
Could not start a new session. Response code 500.
Message: session not created: Chrome failed to start: exited abnormally.
```

**¿Qué significa?** WebDriverManager no pudo iniciar Chrome.

**Causas posibles:**
- Chrome no está instalado en la máquina
- La versión de Chrome y ChromeDriver no coinciden
- En Windows: Chrome está bloqueado por políticas corporativas

**Solución:**
1. Verifica que Chrome está instalado: `google-chrome --version` o busca Chrome en el menú inicio
2. El framework usa WebDriverManager que descarga ChromeDriver automáticamente
3. Si hay firewall corporativo bloqueando la descarga, dile al líder que necesitas
   ChromeDriver descargado manualmente y configurado en el PATH

---

## Flujo de diagnóstico: árbol de decisión

```
Test falla
    │
    ├─ ¿Es TimeoutException o NoSuchElementException?
    │       │
    │       ├─ SÍ → ¿El locator es correcto? (verifica con F12)
    │       │           ├─ NO → Corrige el locator en el PageObject
    │       │           └─ SÍ → ¿El ambiente está arriba?
    │       │                       ├─ NO → Reporta al equipo de infra
    │       │                       └─ SÍ → Aumenta el timeout (caso raro)
    │       │
    ├─ ¿Es AssertionError?
    │       │
    │       └─ ¿Manualmente el caso falla también?
    │                   ├─ SÍ → BUG ENCONTRADO. Reporta el defecto.
    │                   └─ NO → Locator de verificación incorrecto
    │
    ├─ ¿Es SessionNotCreatedException?
    │       └─ Chrome no arranca. Ver sección Error 5.
    │
    └─ ¿Es otro error?
            └─ Copia el mensaje completo y consulta con el capacitador
```

---

## Cómo usar el debugger de VS Code

Cuando no entiendes por qué falla, el debugger te muestra el estado exacto del código:

1. Abre el archivo del step que falla (ej. `DemoLoginSteps.java`)
2. Haz clic en el número de línea del método sospechoso → aparece un punto rojo (breakpoint)
3. Abre el Runner: busca `Runner.java` → botón `Debug Test` sobre la clase
4. La ejecución se pausa en el breakpoint
5. En el panel izquierdo de VS Code (Variables) puedes ver el valor de cada variable

**Qué mirar cuando el debugger se pausa:**
- ¿`driver` es null? → El driver no se inicializó (problema en Hooks.java)
- ¿`url` tiene el valor correcto? → Verifica ConfigManager y el `.env`
- ¿El locator está escrito correctamente? → Pásate al page object y revisa

---

## Cómo leer el reporte de Allure cuando falla

```bash
./gradlew allureReport
# Abre build/reports/allure-report/index.html
```

El reporte muestra:
- **Paso exacto donde falló** (en rojo)
- **Screenshot del momento del fallo** (adjunto automáticamente)
- **URL de la página cuando falló** (adjunto en fallos)

El screenshot es tu mejor herramienta: te muestra exactamente qué vio Chrome.
A veces el test falla porque Chrome estaba en un modal, en una página incorrecta,
o el elemento estaba tapado por otro elemento.

---

## Logging: qué hace `log.debug` y cómo verlo

### Qué son los niveles de log

Cuando escribes `log.debug("Ingresando clave: [ENMASCARADA]")` en un Page Object,
el mensaje **existe pero puede no aparecer** en la consola. Depende del nivel configurado.

Los niveles de menor a mayor severidad son:

| Nivel | Cuándo usarlo | ¿Se ve en consola por defecto? |
|-------|--------------|-------------------------------|
| `log.debug(...)` | Detalles técnicos internos: qué locator se usó, qué valor se escribió | ❌ No (filtrado) |
| `log.info(...)` | Pasos importantes del flujo: "usuario navegó a URL", "click realizado" | ✅ Sí |
| `log.warn(...)` | Situaciones inusuales pero no fatales | ✅ Sí |
| `log.error(...)` | Errores que impiden continuar | ✅ Sí |

**Regla en banca:** usa `log.debug` para detalles sensibles (valores de campos, respuestas de API).
Usa `log.info` para acciones de negocio que quedan como evidencia de auditoría.
**Nunca** loguees passwords ni tokens en texto plano — usa `[ENMASCARADA]` como en `LoginPage.java`.

---

### Por qué no ves los mensajes `log.debug` en la consola

El archivo `src/test/resources/logback-test.xml` controla qué niveles se muestran.
La configuración por defecto tiene el root logger en `INFO`, lo que significa que
`log.debug` se descarta silenciosamente antes de llegar a la consola.

Esto es correcto para ejecución normal — evita ruido innecesario en el output del pipeline.

---

### Cómo activar debug temporalmente para diagnosticar un problema

**Opción A — Solo para tu clase (recomendada, mínimo impacto)**

Edita `src/test/resources/logback-test.xml` y agrega antes de la etiqueta `</configuration>`:

```xml
<!-- DEBUG temporal para LoginPage — quitar antes del commit -->
<logger name="pages.LoginPage" level="DEBUG" additivity="false">
    <appender-ref ref="STDOUT"/>
    <appender-ref ref="FILE"/>
</logger>
```

Esto activa `log.debug(...)` solo para `LoginPage`. El resto del framework sigue en INFO.

**Opción B — Para todo el framework (más ruidoso)**

En `logback-test.xml`, cambia el nivel del root logger:

```xml
<!-- Cambiar INFO por DEBUG -->
<root level="DEBUG">
    <appender-ref ref="STDOUT"/>
    <appender-ref ref="FILE"/>
</root>
```

> ⚠️ Recuerda revertir el cambio antes del commit. El nivel DEBUG en producción
> genera miles de líneas por escenario y degrada el rendimiento.

---

### Dónde aparecen los logs

Los mensajes van a dos lugares:

| Destino | Ruta | Cuándo usarlo |
|---------|------|---------------|
| Consola (terminal) | Output del `./gradlew test` | Mientras ejecutas en local |
| Archivo de log | `build/logs/test-execution.log` | Revisión post-ejecución, análisis de fallos en CI |

Para ver el archivo después de un test fallido:

```powershell
# Ver las últimas 50 líneas del log (PowerShell)
Get-Content build/logs/test-execution.log -Tail 50

# Buscar mensajes de error en el log
Select-String -Path build/logs/test-execution.log -Pattern "ERROR"

# Buscar mensajes de una clase específica
Select-String -Path build/logs/test-execution.log -Pattern "LoginPage"
```

---

### Cómo declarar el logger en una clase nueva

Cuando crees un Page Object o una clase de utils, declara el logger así
en la primera línea de la clase (después de los campos estáticos de locators):

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiNuevaPagina extends BasePage {

    // Logger: usa el nombre de la clase para que logback-test.xml
    // pueda filtrar por paquete (ej. "pages.MiNuevaPagina")
    private static final Logger log = LoggerFactory.getLogger(MiNuevaPagina.class);

    public void realizarAccion(String valor) {
        log.debug("realizarAccion() llamado con valor: {}", valor);  // detalle técnico
        log.info("Realizando acción en pantalla");                   // evidencia de negocio
        // ... código Selenium
    }
}
```

**Nota:** usa `{}` como placeholder en lugar de concatenar strings:
- ✅ `log.debug("Usuario: {}", usuario)` — eficiente: si DEBUG está desactivado,
  ni siquiera construye el string
- ❌ `log.debug("Usuario: " + usuario)` — ineficiente: construye el string siempre,
  aunque DEBUG esté filtrado

---

### Logging en banca: qué debe quedar registrado

En entornos bancarios el log de los tests es parte de la evidencia de ejecución.
Como regla mínima, cada test debe dejar rastro de:

| Acción | Nivel | Ejemplo |
|--------|-------|---------|
| Navegación a URL | INFO | `log.info("Navegando a: {}", url)` |
| Credenciales usadas | INFO | `log.info("Usuario: {} / Clave: [ENMASCARADA]", usuario)` |
| Click en botón crítico | INFO | `log.info("Click en botón de ingresar")` |
| Verificación superada | INFO | `log.info("Panel principal visible — login exitoso")` |
| Detalle de locator | DEBUG | `log.debug("Locator usado: {}", locator)` |
| Valor de campo leído | DEBUG | `log.debug("Texto leído: {}", texto)` |

El archivo `build/logs/test-execution.log` queda disponible como artefacto
en GitLab CI (ver `.gitlab/ci/qa.yml`) y puede ser auditado por el equipo de
seguridad o compliance en caso de incidente.

---

En lugar de correr toda la suite, corre solo el escenario problemático:

```bash
# Por tag
./gradlew test -Dcucumber.filter.tags="@Smoke"

# Por nombre del escenario (partial match)
./gradlew test -Dcucumber.filter.tags="@Smoke" --tests "*Demo*"

# Sin paralelo (más fácil de depurar)
./gradlew test -Dcucumber.filter.tags="@Smoke" -Dcucumber.execution.parallel.enabled=false

# Con la consola visible (sin headless) — solo local, nunca en CI
# En config.properties: browser.headless=false
./gradlew test -Dcucumber.filter.tags="@Smoke"
```

---

## Cuándo escalar al capacitador

Escala cuando:
- Llevas más de 15 minutos en el mismo error sin avance
- El error no se parece a ninguno de esta guía
- El ambiente de prueba no responde

Al escalar, proporciona siempre:
1. El mensaje de error completo (copia y pega, no una foto)
2. El nombre del feature y el escenario que falla
3. Lo que ya intentaste

---

*Guía de depuración v1.0 — QA Automation QA Automatización*
