# Checklist del Día 1 — De cero a tu primer test corriendo

> Este documento es para ti si acabas de unirte al equipo de QA automatización.
> Sigue cada paso en orden. No saltes pasos.
> Tiempo estimado: 2 horas.

---

## BLOQUE 1 — Instalar lo necesario (30 min)

### Paso 1: Java 21
Verifica si ya lo tienes abierto una terminal (Git Bash, PowerShell o CMD):
```bash
java -version
```
Si ves `openjdk 21` o `java version "21"`, ya está. Continúa al paso 2.

Si no está instalado:
1. Ve a: https://adoptium.net/
2. Descarga **Temurin 21 (LTS)**
3. Instala con todas las opciones por defecto
4. Cierra y vuelve a abrir la terminal
5. Ejecuta `java -version` de nuevo para confirmar

---

### Paso 2: VS Code
Verifica:
```bash
code --version
```
Si no está, descárgalo de https://code.visualstudio.com/ e instálalo.

Luego lee y sigue `docs/01-onboarding/VSCODE_SETUP.md` para instalar
las extensiones necesarias (Extension Pack for Java, Cucumber, GitLens).

---

### Paso 3: Git
Verifica:
```bash
git --version
```
Si no está, descárgalo de https://git-scm.com/ e instálalo con opciones por defecto.

Configura tu identidad (usa tu correo corporativo):
```bash
git config --global user.name "Tu Nombre"
git config --global user.email "tu.correo@example.pe"
```

---

## BLOQUE 2 — Obtener el proyecto (20 min)

### Paso 4: Clonar el repositorio

Pídele al líder técnico la URL del repositorio en GitLab. Luego:
```bash
git clone <URL-que-te-dieron>
cd plantilla-devsecops
```

### Paso 5: Abrir en VS Code
```bash
code .
```
VS Code abrirá el proyecto. Espera a que cargue (la barra de estado abajo
mostrará "Java" cuando esté listo, puede tardar 1-2 minutos la primera vez).

---

## BLOQUE 3 — Configurar credenciales locales (15 min)

### Paso 6: Crear tu archivo de configuración local

En la carpeta `qa-automation/` copia el archivo de ejemplo:
```bash
cd qa-automation
cp .env.example .env
```

Abre el archivo `.env` que acabas de crear y rellena los valores reales.
El capacitador te dará las credenciales del ambiente de prueba.

```
TEST_BASE_URL=https://tuapp.example.pe/auth
TEST_USERNAME=tu_usuario_de_prueba
TEST_PASSWORD=tu_clave_de_prueba
```

> **Importante:** El archivo `.env` está en `.gitignore`. Nunca se sube al
> repositorio. Es solo tuyo, local. Nunca compartas tu contraseña.

### Paso 7: Verificar que Gradle funciona

Desde la carpeta `qa-automation/`:
```bash
./gradlew --version
```
Deberías ver algo como `Gradle 8.7`. Si aparece un error de Java,
vuelve al Paso 1 y verifica la instalación de Java 21.

---

## BLOQUE 4 — Correr tu primer test (20 min)

### Paso 8: Ejecutar el test de demo (Smoke)

```bash
./gradlew test -Dcucumber.filter.tags="@Smoke"
```

Verás que Chrome se abre (o corre en segundo plano si estás en CI).
Espera a que termine. Al final verás algo como:

```
BUILD SUCCESSFUL in 12s
✓ 1 test passed
```

Si ves `BUILD FAILED`, ve a `docs/01-onboarding/GUIA_DEPURACION.md`
y sigue los pasos de diagnóstico.

### Paso 9: Ver el reporte de resultados

```bash
./gradlew allureReport
```

Se genera un reporte en `build/reports/allure-report/index.html`.
Ábrelo en el navegador. Verás el resultado del test con screenshots.

---

## BLOQUE 5 — Entender el código (35 min)

### Paso 10: Leer COMO_FUNCIONA_EL_FRAMEWORK.md

Lee `docs/01-onboarding/COMO_FUNCIONA_EL_FRAMEWORK.md` de principio a fin.
Este documento explica la relación entre feature, steps y page objects.

### Paso 11: Rastrear el test de demo en el código

Con VS Code abierto, sigue este recorrido:

1. Abre `src/test/resources/features/Demo.feature`
   → Lee el escenario. ¿Qué debería hacer el test?

2. Haz Ctrl+Click sobre el texto `"el usuario navega al portal de la aplicación"`
   → VS Code debería llevarte al método en `DemoLoginSteps.java`

3. En `DemoLoginSteps.java`, observa cómo el método llama a `loginPage.navigateTo(...)`

4. Abre `src/test/java/pages/LoginPage.java`
   → Observa los locators (constantes `By.id(...)`) y los métodos públicos

5. Abre `src/test/java/pages/BasePage.java`
   → Busca el método `write()` (línea ~221). Lee qué hace internamente.

Si pudiste seguir ese recorrido, entiendes el framework. Continúa.

### Paso 12: Leer los templates

Abre `docs/03-templates/` y lee los tres archivos:
- `FEATURE_TEMPLATE.feature` — cómo escribir escenarios
- `STEP_DEFINITION_TEMPLATE.md` — cómo escribir steps
- `PAGE_OBJECT_TEMPLATE.md` — cómo escribir page objects

---

## BLOQUE 6 — Verificar que todo está en orden (5 min)

### Checklist final antes de tu primer MR

Marca cada ítem antes de decirle al capacitador que estás listo:

- [ ] `java -version` muestra Java 21
- [ ] `./gradlew --version` muestra Gradle 8.7
- [ ] El test de demo pasa (`BUILD SUCCESSFUL`)
- [ ] Puedo abrir el reporte de Allure en el navegador
- [ ] Puedo navegar de un feature a su step con Ctrl+Click en VS Code
- [ ] Leí `COMO_FUNCIONA_EL_FRAMEWORK.md` completo
- [ ] Leí los 3 templates
- [ ] Mi archivo `.env` tiene las credenciales correctas (test pasa)

---

## ¿Algo no funcionó?

| Síntoma | Ir a |
|---|---|
| `java -version` da error | Paso 1 de este documento |
| `./gradlew --version` da error | `docs/01-onboarding/TROUBLESHOOTING_AUTOMATION.md` → sección Gradle |
| El test falla con "element not found" | `docs/01-onboarding/GUIA_DEPURACION.md` → sección Locators |
| Chrome no abre | `docs/01-onboarding/TROUBLESHOOTING_AUTOMATION.md` → sección Chrome |
| No entiendo el código | `docs/01-onboarding/COMO_FUNCIONA_EL_FRAMEWORK.md` |
| Cualquier otro error | Pregunta al capacitador con el mensaje de error copiado completo |

---

## Próximos pasos (Día 2 en adelante)

1. Lee `docs/02-governance/QA_AUTOMATION_STANDARD_V1.md` — los estándares del equipo
2. Lee `docs/02-governance/CUCUMBER_BDD_STYLE_GUIDE.md` — cómo escribir buenos escenarios
3. Lee `docs/01-onboarding/PRIMER_MR_AUTOMATION.md` — cómo subir tu primer cambio
4. Crea tu primera feature con tu capacitador

---

*Documento del programa de incorporación QA — QA Automation*
