# Automation Foundations Playbook - 30 minutos para iniciar

## 1. Objetivo
Permitir que un QA nuevo pueda iniciar automatizacion en menos de 30 minutos usando la plantilla.

## 1.1 Antes de empezar

Si es tu primer dia, revisa primero:
1. [docs/01-onboarding/START_HERE.md](../01-onboarding/START_HERE.md)
2. [docs/01-onboarding/VSCODE_SETUP.md](../01-onboarding/VSCODE_SETUP.md)
3. [QUICK_RUN.md](../../QUICK_RUN.md)

Objetivo minimo antes de seguir con este playbook:
- VS Code listo.
- Java 21 detectado.
- Un `@Smoke` ejecutado en modo secuencial.

## 2. Paso a paso rapido
1. Clonar el repositorio base.
2. Crear rama feature.
3. Configurar base.url y credenciales de prueba.
4. Crear primera feature con tag @Smoke.
5. Implementar page + steps.
6. Ejecutar pruebas locales.
7. Abrir MR con evidencia.

## 3. Que editar
- src/test/resources/config.properties
- src/test/resources/features/*.feature
- src/test/java/pages/*
- src/test/java/steps/*
- projects/<tu-proyecto>/ si el equipo genera scaffold con [scripts/new-project.ps1](../../scripts/new-project.ps1)

## 4. Que no editar al inicio
- Core de drivers.
- Hooks globales.
- Pipeline base sin coordinacion.
- junit-platform.properties para cambiar paralelismo base.

## 5. Comando minimo de ejecucion
```powershell
# Se asume TEST_USERNAME y TEST_PASSWORD ya definidos en el entorno local o CI.
# No hardcodear credenciales en scripts ni en el repositorio.
.\gradlew test "-Dcucumber.filter.tags=@Smoke"
```

## 5.1 Paralelismo - lo que debes saber desde el dia 1

El framework ejecuta en paralelo por defecto (3 hilos). Esto es correcto para CI/CD,
pero para tu primer test local usa siempre modo secuencial para ver los logs limpios:

```powershell
# Primer test: secuencial para debug claro
.\gradlew test "-Dcucumber.filter.tags=@Smoke" "-Dcucumber.execution.parallel.enabled=false"
```

Reglas basicas:
- No modifiques `src/test/resources/junit-platform.properties` sin coordinacion con el arquitecto QA.
- No uses variables estaticas mutables en pages ni steps (producen fallas intermitentes en paralelo).
- Si un test pasa en secuencial pero falla en paralelo: reportar como flakiness con causa raiz antes del merge.
- Escenarios @Destructive siempre van al final secuencial (el Gradle task `testDestructive` lo gestiona).

Referencia completa de comandos: `QUICK_RUN.md`
Referencia de reglas tecnicas: [docs/02-governance/SELENIUM_JAVA_PRACTICES.md](../02-governance/SELENIUM_JAVA_PRACTICES.md)
Referencia de fallas comunes: [docs/01-onboarding/TROUBLESHOOTING_AUTOMATION.md](../01-onboarding/TROUBLESHOOTING_AUTOMATION.md)

## 6. Criterio de entrega inicial
- Escenario @Smoke funcionando.
- Evidencia de reporte generada.
- MR con checklist completo.
- Sin secretos en codigo.
- Sin `Thread.sleep` en steps ni pages.

## 7. Escalamiento
Si un bloqueo excede 2 horas, escalar al referente tecnico QA con:
- Mensaje de error.
- Paso exacto de reproduccion.
- Commit o rama asociada.
