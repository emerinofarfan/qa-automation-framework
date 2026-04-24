# Commit and Branch Naming Policy

## 1. Objetivo
Estandarizar el nombramiento de ramas y commits para equipos QA en el flujo DevSecOps corporativo.

## 2. Estrategia de ramificacion (alineada a plantilla de Joel)
Flujo corporativo:
- feature/* -> master -> staging -> production

Ramas base:
- master: integracion continua.
- staging: validacion UAT.
- production: liberacion productiva.

## 3. Nomenclatura de ramas de trabajo QA
Formato recomendado:
- feature/qa-<modulo>-<objetivo-corto>
- hotfix/qa-<incidente>-<objetivo-corto>
- chore/qa-<actividad-corta>

Ejemplos:
- feature/qa-login-smoke
- feature/qa-castigos-restricciones
- hotfix/qa-timeout-auth
- chore/qa-update-docs

Reglas:
- Usar minusculas y guiones medios.
- No usar espacios ni caracteres especiales.
- Mantener nombres cortos y trazables.

## 4. Convencion de commits (Conventional Commits)
Formato:
- <tipo>(<scope>): <descripcion>

Tipos recomendados para QA:
- feat: nueva capacidad de automatizacion.
- fix: correccion de falla en pruebas o framework.
- docs: cambios de documentacion.
- test: cambios de escenarios, steps, datos de prueba.
- refactor: mejora interna sin cambiar comportamiento funcional.
- chore: tareas de soporte (build, config, housekeeping).

Ejemplos:
- feat(qa): agrega smoke de login para modulo castigos
- test(cucumber): incorpora escenarios de restricciones de negocio
- fix(selenium): corrige sincronizacion en pagina de autenticacion
- docs(qa): actualiza guia inicial de primer MR

## 5. Reglas para Merge Request
- No hacer push directo a master, staging o production.
- Abrir MR desde rama de trabajo hacia rama objetivo.
- Completar checklist QA del MR.
- Adjuntar evidencia de ejecucion y riesgos.

## 6. Aprobaciones por ambiente
Referencia recomendada:
- master: 1 peer review.
- staging: 2 aprobaciones (lider tecnico).
- production: 3 aprobaciones (CAB).

## 7. Resultado esperado
Uniformidad de trazabilidad en auditoria, facilidad de soporte y adopcion consistente por todos los equipos QA.
