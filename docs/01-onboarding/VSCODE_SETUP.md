# Configuracion de VS Code para el Framework QA

> Este es el IDE estandar del equipo. No se requiere IntelliJ IDEA ni licencia comercial.

---

## 1. Extensiones requeridas

Al abrir el proyecto en VS Code aparecera una notificacion:
**"This workspace has extension recommendations. Do you want to install them?"** → clic en **Install All**.

Si no aparece: `Ctrl+Shift+P` → **Extensions: Show Recommended Extensions** → instalar todas.

| Extension | Para que sirve |
|-----------|---------------|
| Extension Pack for Java | Compilar, navegar y debuggear Java |
| Gradle for Java | Ver tareas Gradle, ejecutar desde panel lateral |
| Cucumber (official) | Go-to-definition entre `.feature` y `Steps.java` |
| CucumberAutoComplete | Autocompletado en archivos `.feature` |
| GitLens | Blame, historial por linea, comparar ramas |
| GitLab Workflow | Ver pipelines y MRs sin salir de VS Code |
| Markdown Preview Enhanced | Preview de documentacion con tablas y Mermaid |
| YAML | Validacion y autocompletado en `.gitlab-ci.yml` |
| SonarLint | Detecta bugs y code smells en tiempo real |

---

## 2. Terminal recomendada — cuál usar y por qué

### El problema con CMD y PowerShell 5 heredado

VS Code en Windows abre **CMD** o **PowerShell 5** por defecto.
Ambas son terminales de Windows y funcionan para tareas básicas,
pero generan fricción en un proyecto DevOps porque:

- El pipeline de GitLab CI corre sobre **Linux** (Ubuntu)
- Los scripts del repositorio (`gradlew`, scripts de bash) usan sintaxis Unix
- Comandos como `./gradlew` fallan en CMD (hay que escribir `.\gradlew`)
- Variables de entorno, rutas y comillas se comportan diferente

El resultado: quien está iniciando en automatización prueba algo en local con CMD,
funciona, lo sube al pipeline y falla — porque en Linux el comportamiento es diferente.

---

### La terminal recomendada: Git Bash

**Git Bash** es la terminal recomendada para este proyecto. Se instala junto con Git
y emula un entorno Unix dentro de Windows. Ventajas concretas:

| Criterio | CMD | PowerShell 5 | Git Bash ✅ |
|----------|-----|--------------|------------|
| Sintaxis igual que CI/CD (Linux) | ❌ | ❌ | ✅ |
| `./gradlew` funciona sin modificar | ❌ | ❌ | ✅ |
| SSH nativo (`ssh`, `scp`) | ❌ | Limitado | ✅ |
| Variables de entorno con `export` | ❌ | ❌ | ✅ |
| Pipes y redirecciones Unix (`\|`, `>`) | Parcial | Parcial | ✅ |
| Compatible con scripts `.sh` del repo | ❌ | ❌ | ✅ |
| Instalación requerida | No | No | Git (ya obligatorio) |

**PowerShell 7+** como segunda opción: es cross-platform y mejor que PowerShell 5,
pero aún difiere de Linux en rutas y scripts. Úsalo solo para tareas específicas
de Windows (variables de entorno del sistema, gestión de servicios).

**Evita CMD** completamente para trabajo de automatización.

---

### Instalar Git Bash (si no lo tienes)

1. Descarga Git desde [https://git-scm.com/download/win](https://git-scm.com/download/win)
2. Durante la instalación, en la pantalla **"Adjusting your PATH environment"**:
   selecciona **"Git from the command line and also from 3rd-party software"**
3. En **"Choosing the default terminal emulator"**:
   selecciona **"Use MinTTY (the default terminal of MSYS2)"**
4. Completa la instalación con las demás opciones por defecto
5. Verifica que quedó instalado:
   ```bash
   git --version
   # Esperado: git version 2.x.x.windows.x
   ```

---

### Configurar Git Bash como terminal por defecto en VS Code

Una sola vez, después de instalar Git:

1. En VS Code: `Ctrl+Shift+P` → escribe **"Terminal: Select Default Profile"**
2. Selecciona **"Git Bash"** de la lista
3. Cierra todas las terminales abiertas con `Ctrl+Shift+`\`` → ícono de papelera
4. Abre una nueva terminal: `Ctrl+`\`` → debe mostrar `$` (prompt de bash)

**Verificación:**
```bash
echo $SHELL
# Esperado: /usr/bin/bash  (confirma que es Git Bash, no CMD)
```

Si no aparece Git Bash en la lista, cierra y vuelve a abrir VS Code después
de instalar Git.

---

### Instalar PowerShell 7+ (complemento para tareas Windows)

PowerShell 7 no reemplaza a Git Bash — es complementario para tareas
que solo se hacen en Windows (configurar variables de entorno del sistema,
trabajar con servicios Windows, automatización de Office).

1. Descarga desde [https://aka.ms/powershell](https://aka.ms/powershell)
   → selecciona la versión **LTS (Long Term Support)** para Windows x64
2. Instala con opciones por defecto
3. Verifica:
   ```powershell
   $PSVersionTable.PSVersion
   # Esperado: Major 7 o superior
   ```

Para configurarlo como terminal adicional en VS Code (sin quitar Git Bash):
`Ctrl+Shift+P` → **"Terminal: Select Default Profile"** → cuando necesites PowerShell,
ábrelo con el botón `+` de la terminal y selecciona "PowerShell" del menú desplegable.

---

### Cuándo usar cada terminal

| Tarea | Terminal recomendada |
|-------|---------------------|
| Ejecutar tests Gradle (`./gradlew test`) | Git Bash |
| Comandos Git (`git clone`, `git push`) | Git Bash |
| Ver logs (`tail`, `grep`) | Git Bash |
| Scripts del repositorio (`./scripts/quick-run.ps1`) | PowerShell 7 |
| Configurar variables de entorno del sistema | PowerShell 7 (como Admin) |
| Verificar versiones (`java -version`, `git --version`) | Cualquiera |
| Comandos del pipeline CI en local | Git Bash (mismo entorno que CI) |

---

## 2. Verificar que Java y Gradle estan detectados

Abrir la terminal integrada (`Ctrl+`` ` ``) y ejecutar:

```powershell
java -version
# Esperado: openjdk version "21.x.x" o Java(TM) SE Runtime 21.x.x

.\gradlew -v
# Esperado: Gradle 8.12 / Launcher JVM 21.x.x
```

Si `java -version` falla: revisar que `JAVA_HOME` apunta a `C:\Program Files\Java\jdk-21`.

```powershell
# Verificar JAVA_HOME
[System.Environment]::GetEnvironmentVariable("JAVA_HOME", "Machine")

# Si esta vacio, definirlo (requiere PowerShell como Administrador):
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-21", "Machine")
```

Reiniciar VS Code despues de cambiar variables de entorno.

---

## 3. Resolver "Java: Error" en la barra de estado

Si VS Code muestra `Java: Error` en la barra inferior:

1. `Ctrl+Shift+P` → **Java: Clean Java Language Server Workspace**
2. Seleccionar **Restart and delete** cuando pregunte
3. Esperar a que el icono cambie de `Java: Error` a `Java: Ready` (puede tardar 1-2 minutos)

> **Causa raiz:** el Language Server de Java necesita indexar el proyecto con Gradle.
> Esto ocurre la primera vez que se abre el proyecto o al cambiar dependencias en `build.gradle`.

---

## 4. Ejecutar tests desde VS Code

### Opcion A — Terminal integrada (recomendada, igual que en CI)

Abrir terminal con `Ctrl+` `` ` `` y usar cualquier comando de `QUICK_RUN.md`:

```powershell
# Primer test: Smoke secuencial
.\gradlew test "-Dcucumber.filter.tags=@Smoke" "-Dcucumber.execution.parallel.enabled=false"
```

### Opcion B — Run & Debug con configuraciones predefinidas (`launch.json`)

1. Abrir el panel **Run & Debug** con `Ctrl+Shift+D`
2. Seleccionar una configuracion del desplegable:

| Configuracion | Cuando usarla |
|---------------|--------------|
| `Smoke - Secuencial (debug local)` | Primer test del dia, verificar ambiente |
| `Smoke - Paralelo (3 hilos)` | Validar thread-safety antes de MR |
| `Escenario especifico (por tag o nombre)` | Depurar una falla puntual |
| `Rerun - Solo escenarios fallidos` | Confirmar si un fallo es flaky |
| `Regression - Paralelo (sin Destructive)` | Validacion completa pre-merge |

3. Presionar `F5` para iniciar

> Para depurar un escenario especifico, editar `vmArgs` en `.vscode/launch.json`
> cambiando `@MiTag` por el tag del escenario que necesitas depurar.

### Opcion C — Panel de Gradle

Panel lateral **Gradle** (icono de elefante) → `Tasks` → `verification`:
- `test` — ejecuta la suite completa
- `testDestructive` — ejecuta solo escenarios @Destructive
- `qaReport` — ejecuta tests y genera reporte Allure

---

## 5. Navegar entre Feature y Steps

Con la extension **Cucumber (official)** instalada:

- Posicionarse sobre un step en el `.feature` (ej. `Given el usuario abre la aplicacion`)
- `F12` o `Ctrl+Click` → navega directamente al metodo Java en `Steps.java`
- `Shift+F12` → muestra todos los usos del step

Para que funcione, los archivos de steps deben estar en: `src/test/java/steps/`

---

## 6. Ver el reporte Allure

Despues de ejecutar tests, abrir el reporte directamente en VS Code:

```powershell
.\gradlew allureReport
# El reporte queda en: build/reports/allure-report/index.html
```

Desde el explorador de VS Code: clic derecho sobre `build/reports/allure-report/index.html` → **Open with Live Server** (si tienes la extension) o abrir en el navegador.

---

## 7. Breakpoints y debug paso a paso

1. Abrir el archivo `Steps.java` del escenario que quieres depurar
2. Hacer clic en el margen izquierdo junto al numero de linea donde quieres pausar (punto rojo)
3. Ejecutar con `Ctrl+Shift+D` → seleccionar `Escenario especifico` → `F5`
4. VS Code pausa en el breakpoint:
   - `F10` — siguiente linea (sin entrar al metodo)
   - `F11` — entrar al metodo
   - `F5` — continuar hasta el siguiente breakpoint

> El debug funciona en modo secuencial. Nunca debuggear con paralelo habilitado:
> los hilos se intercalan y los breakpoints no se comportan de forma predecible.

---

## 8. Variables de entorno para ejecucion local

Las credenciales y URLs **nunca van en codigo ni en `.vscode/launch.json`**.
Definirlas en el sistema para que VS Code las herede automaticamente:

```powershell
# Abrir PowerShell como Administrador y ejecutar una sola vez:
[System.Environment]::SetEnvironmentVariable("TEST_USERNAME", "tu_usuario_qa", "User")
[System.Environment]::SetEnvironmentVariable("TEST_PASSWORD", "tu_password_qa", "User")
[System.Environment]::SetEnvironmentVariable("BASE_URL", "https://tuapp.example.pe", "User")

# Reiniciar VS Code para que tome las variables nuevas
```

Verificar que estan activas:
```powershell
echo $env:TEST_USERNAME
echo $env:BASE_URL
```

---

## 9. Atajos utiles de VS Code para este proyecto

| Atajo | Accion |
|-------|--------|
| `Ctrl+Shift+P` | Paleta de comandos (acceso a todo) |
| `Ctrl+Shift+D` | Panel Run & Debug |
| `` Ctrl+` `` | Abrir / cerrar terminal integrada |
| `Ctrl+Shift+G` | Panel Git (commits, diff, staging) |
| `F12` | Ir a la definicion (feature → step, step → page) |
| `Shift+F12` | Ver todos los usos de un metodo |
| `Ctrl+Shift+F` | Buscar en todos los archivos del proyecto |
| `Ctrl+P` | Abrir archivo por nombre rapido |
| `Ctrl+K V` | Preview de Markdown en panel lateral |

---

## 10. Si algo no funciona — checklist

- [ ] Java 21 en PATH: `java -version` muestra 21.x
- [ ] Gradle wrapper operativo: `.\gradlew -v` muestra 8.12
- [ ] Language Server listo: barra inferior muestra `Java: Ready` (no `Java: Error`)
- [ ] Extensiones instaladas: Ver → Extensions → filtrar por @recommended
- [ ] Variables de entorno definidas: `echo $env:TEST_USERNAME` muestra valor
- [ ] Primer test pasa: `.\gradlew test "-Dcucumber.filter.tags=@Smoke" "-Dcucumber.execution.parallel.enabled=false"`

Si todo lo anterior esta bien y hay una falla: ver `docs/01-onboarding/TROUBLESHOOTING_AUTOMATION.md`.
