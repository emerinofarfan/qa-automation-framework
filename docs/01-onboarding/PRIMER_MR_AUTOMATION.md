# Primer MR Automation - Guia operativa

## 1. Objetivo
Abrir el primer Merge Request de automatizacion sin romper el estandar.

## 1.1 Lectura minima antes del MR

Antes de abrir tu primer MR, confirma que ya revisaste:
- [docs/01-onboarding/START_HERE.md](../01-onboarding/START_HERE.md)
- [docs/01-onboarding/AUTOMATION_FOUNDATIONS_PLAYBOOK.md](../01-onboarding/AUTOMATION_FOUNDATIONS_PLAYBOOK.md)
- [docs/02-governance/MR_QA_CHECKLIST.md](../02-governance/MR_QA_CHECKLIST.md)
- [docs/02-governance/QUALITY_GATES.md](../02-governance/QUALITY_GATES.md)

## 2. Checklist previo
- Rama creada desde master: `feature/<nombre-corto>`.
- Al menos un escenario `@Smoke` implementado.
- Step definitions sin duplicidad semantica.
- Evidencia local de ejecucion.
- Pipeline local o validacion minima ejecutada sin secretos hardcodeados.

## 3. Flujo recomendado
1. Crear feature y steps del caso.
2. Ejecutar local:
   - `./gradlew clean test "-Dcucumber.filter.tags=@Smoke" "-Dcucumber.execution.parallel.enabled=false"`
3. Validar reporte generado.
4. Commit con convencional commit.
5. Push y apertura de MR.

## 4. Contenido minimo del MR
- Objetivo funcional.
- Cambios realizados.
- Riesgos y supuestos.
- Evidencia de pruebas.
- Resultado de pipeline.

## 5. Criterio de aprobacion inicial
- Pipeline en verde.
- Cumple [docs/02-governance/MR_QA_CHECKLIST.md](../02-governance/MR_QA_CHECKLIST.md).
- Cumple [docs/02-governance/QUALITY_GATES.md](../02-governance/QUALITY_GATES.md) del ambiente.

## 6. Como impacta CODEOWNERS en tu MR

`CODEOWNERS` define que equipo debe aprobar cambios segun el tipo de archivo.
En este framework, cambios en core, docs de gobernanza o CI pueden requerir aprobacion
del equipo de arquitectura QA antes de merge.

Que debes hacer en etapa inicial:
1. Abrir tu MR con descripcion clara y evidencia.
2. Revisar en la seccion de aprobaciones si GitLab solicita Code Owner.
3. Si falta aprobacion de owner, no forzar merge: pedir revision al equipo indicado.
4. Corregir comentarios y volver a solicitar aprobacion.

Regla:
- Si el pipeline esta verde pero falta aprobacion de Code Owner, el MR aun NO esta listo para merge.

Referencia:
- [docs/02-governance/GITLAB_PROJECT_HARDENING.md](../02-governance/GITLAB_PROJECT_HARDENING.md)
