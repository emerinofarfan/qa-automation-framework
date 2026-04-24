# Start Here - Framework QA Base

> **IDE estandar: Visual Studio Code.** No se requiere IntelliJ ni licencia comercial.
> Configuracion completa del IDE: [docs/01-onboarding/VSCODE_SETUP.md](../01-onboarding/VSCODE_SETUP.md)

---

## Requisitos minimos
- Java 21 disponible en PATH.
- Variables de entorno definidas por el equipo (una sola vez en el sistema):
  - `TEST_USERNAME`
  - `TEST_PASSWORD`
  - `BASE_URL` (si aplica para el proyecto)

## Paso 0 - Configurar VS Code (solo la primera vez)
1. Abrir el proyecto en VS Code: `code .` desde la carpeta raiz.
2. Instalar extensiones recomendadas cuando VS Code lo sugiera, o:
   `Ctrl+Shift+P` → **Extensions: Show Recommended Extensions** → Install All.
3. Esperar a que la barra inferior muestre `Java: Ready` (puede tardar 1-2 min).
4. Si muestra `Java: Error`: `Ctrl+Shift+P` → **Java: Clean Java Language Server Workspace** → Restart.

Guia completa: [docs/01-onboarding/VSCODE_SETUP.md](../01-onboarding/VSCODE_SETUP.md)

## Si es tu primer dia

Lee solo esto, en este orden:
1. [docs/01-onboarding/VSCODE_SETUP.md](../01-onboarding/VSCODE_SETUP.md)
2. [QUICK_RUN.md](../../QUICK_RUN.md)
3. [docs/01-onboarding/AUTOMATION_FOUNDATIONS_PLAYBOOK.md](../01-onboarding/AUTOMATION_FOUNDATIONS_PLAYBOOK.md)

No necesitas leer todavia los documentos de gobernanza pesada o DevSecOps.
Primero logra esto:
- VS Code listo.
- Java 21 detectado.
- Tu primer `@Smoke` verde en secuencial.

## Paso 1
Clona este repositorio como base del equipo.

## Paso 2
Crea una rama de trabajo:
```bash
git checkout -b feature/inicializar-proyecto-equipo
```

## Paso 3
Prepara tu estructura inicial antes de escribir casos reales.

Ruta recomendada:
1. Si tu equipo parte desde cero, genera un esqueleto con [scripts/new-project.ps1](../../scripts/new-project.ps1).
2. Revisa las plantillas de feature, step definition y page object.
3. Abre el ejemplo demo para entender el formato minimo esperado.

Comando recomendado para scaffold inicial:

```powershell
.\scripts\new-project.ps1 -ProjectName "mi-aplicacion" -BaseUrl "https://miapp.example.pe" -AppName "mi-aplicacion"
```

Referencias obligatorias en este paso:
- [docs/03-templates/FEATURE_TEMPLATE.feature](../03-templates/FEATURE_TEMPLATE.feature)
- [docs/03-templates/STEP_DEFINITION_TEMPLATE.md](../03-templates/STEP_DEFINITION_TEMPLATE.md)
- [docs/03-templates/PAGE_OBJECT_TEMPLATE.md](../03-templates/PAGE_OBJECT_TEMPLATE.md)
- [src/test/resources/features/Demo.feature](../../src/test/resources/features/Demo.feature)
- [src/test/java/steps/DemoLoginSteps.java](../../src/test/java/steps/DemoLoginSteps.java)

Que debes crear o ajustar en tu proyecto:
- features
- steps
- pages
- data
- config.properties del proyecto del equipo

## Paso 4
Ejecuta validaciones:
```powershell
.\gradlew clean test
```

En terminal Bash tambien puedes usar:
```bash
./gradlew clean test
```

## Paso 4.1 - Primer test recomendado (camino feliz)
1. Crea un feature con tag @Smoke en src/test/resources/features.
2. Crea sus step definitions en src/test/java/steps.
3. Implementa acciones UI en src/test/java/pages.
4. Confirma que `TEST_USERNAME`, `TEST_PASSWORD` y `BASE_URL` existen en tu entorno.
5. Ejecuta solo smoke para validar arranque en modo **secuencial** (recomendado para primer test):

```powershell
# Secuencial: logs claros, facil de depurar
.\gradlew test "-Dcucumber.filter.tags=@Smoke" "-Dcucumber.execution.parallel.enabled=false"
```

> **Nota:** El framework corre en paralelo por defecto (3 hilos). Para depuracion local
> usa siempre `-Dcucumber.execution.parallel.enabled=false`. Ver [QUICK_RUN.md](../../QUICK_RUN.md) para
> todos los comandos disponibles (paralelo, secuencial, por suite, con Allure).

Resultado esperado de este paso:
- Compilacion exitosa.
- Un escenario @Smoke verde.
- Sin secretos hardcodeados.
- Evidencia disponible para Allure o consola.

## Paso 5
Abre MR hacia master y sigue gobernanza.

## Paso 6
Antes de solicitar aprobacion, valida cumplimiento del estandar:
- [docs/02-governance/QA_AUTOMATION_STANDARD_V1.md](../02-governance/QA_AUTOMATION_STANDARD_V1.md)
- [docs/02-governance/CUCUMBER_BDD_STYLE_GUIDE.md](../02-governance/CUCUMBER_BDD_STYLE_GUIDE.md)
- [docs/02-governance/SELENIUM_JAVA_PRACTICES.md](../02-governance/SELENIUM_JAVA_PRACTICES.md)
- [docs/02-governance/JAVA_DIRECTORY_STRUCTURE_STANDARD.md](../02-governance/JAVA_DIRECTORY_STRUCTURE_STANDARD.md)
- [docs/02-governance/NAMING_CONVENTIONS.md](../02-governance/NAMING_CONVENTIONS.md)
- [docs/02-governance/QUALITY_GATES.md](../02-governance/QUALITY_GATES.md)
- [docs/02-governance/MR_QA_CHECKLIST.md](../02-governance/MR_QA_CHECKLIST.md)

## Paso 7
Valida seguridad y operacion en entorno corporativo:
- [docs/02-governance/SECURITY_AND_TEST_DATA_POLICY.md](../02-governance/SECURITY_AND_TEST_DATA_POLICY.md)
- [docs/02-governance/INTEGRACION_CON_PLANTILLA_DEVSECOPS.md](../02-governance/INTEGRACION_CON_PLANTILLA_DEVSECOPS.md)

## Paso 8
Si es tu primer despliegue de automatizacion:
- [docs/01-onboarding/PRIMER_MR_AUTOMATION.md](../01-onboarding/PRIMER_MR_AUTOMATION.md)
- [docs/01-onboarding/TROUBLESHOOTING_AUTOMATION.md](../01-onboarding/TROUBLESHOOTING_AUTOMATION.md)
