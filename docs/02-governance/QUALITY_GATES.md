# Quality Gates QA Automation

## 1. Ambito de ejecucion por flujo
- MR feature/* u hotfix/* -> master: ejecutar @Smoke.
- MR master -> staging: ejecutar @Smoke + @Critical.
- MR staging -> production: ejecutar @Regression completo.

Implementacion CI de referencia en este repositorio:
- .gitlab-ci.yml -> validate-build
- .gitlab-ci.yml -> test-smoke-mr (gate obligatorio en MR)
- .gitlab-ci.yml -> test-critical-main (gate en rama principal)
- .gitlab-ci.yml -> regression-nightly y regression-nightly-destructive

## 2. Gates minimos por ambiente

### master
- Build en verde.
- Escenarios @Smoke en verde.
- Sin credenciales expuestas.
- MR con 1 aprobacion requerida.

### staging
- Gates de master cumplidos.
- @Critical en verde.
- Evidencia Allure publicada.
- MR con 2 aprobaciones requeridas (lider tecnico).

### production
- Gates de staging cumplidos.
- @Regression en verde.
- MR con 3 aprobaciones requeridas (CAB).

## 3. Reglas de bloqueo
Se bloquea merge cuando ocurra al menos una de estas condiciones:
- Falla cualquier escenario @Smoke o @Critical.
- No existe evidencia de ejecucion.
- Faltan aprobaciones obligatorias.
- Existe incumplimiento de estandares de seguridad.

## 4. Relacion con configuracion GitLab
- Branch protection activa en master/staging/production.
- Requerir pipeline exitoso para merge.
- Reglas de aprobacion por rama alineadas al ambiente.

## 5. Evidencia minima por ejecucion
- Resumen de pipeline.
- Resultado de escenarios por tag.
- Capturas o adjuntos de fallas criticas.
- Referencia de commit y rama.
