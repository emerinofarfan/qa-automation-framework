# GitLab Project Hardening - Estandar QA Automation

Configuracion minima de proyecto para que las politicas del framework se cumplan en plataforma.

## 1. Branch protection
Aplicar a master, staging y production:
- Allowed to push: No one.
- Allowed to merge: Maintainers (o rol definido por gobierno TI).
- Require successful pipeline: habilitado.
- Prevent approval by author: habilitado.
- Prevent approval by committers: habilitado cuando aplique.

## 2. Approval rules por rama
- master: minimo 1 aprobacion (peer review).
- staging: minimo 2 aprobaciones (lider tecnico).
- production: minimo 3 aprobaciones (CAB).

## 3. Merge checks
- Requerir discusiones resueltas antes de merge.
- Requerir pipeline verde para merge.
- Bloquear merge si hay jobs obligatorios fallidos.

## 4. Variables CI/CD (seguridad)
- Secrets como Protected y Masked cuando aplique.
- Limitar variables protegidas a ramas protegidas.
- Rotacion periodica de secretos criticos.

## 5. CODEOWNERS
- Mantener CODEOWNERS activo para docs de gobernanza, CI y framework base.
- Requerir aprobacion de code owners en cambios de politicas.

Configuracion recomendada en GitLab:
- Activar aprobacion por Code Owners en Merge Requests.
- No permitir merge si falta aprobacion de owner requerido.
- Mantener sincronizado el archivo `CODEOWNERS` cuando cambie estructura de carpetas.

Uso esperado:
- Cambio en `.gitlab-ci.yml` o `/.gitlab/ci/*` -> revisa owner de CI/arquitectura.
- Cambio en `docs/02-governance/*` -> revisa owner de gobernanza.
- Cambio en `src/*` -> revisa owner tecnico del framework.

Buenas practicas:
- No poner demasiados owners por ruta (ralentiza aprobaciones).
- Evitar patrones ambiguos que se superpongan sin necesidad.
- Revisar `CODEOWNERS` en cada version mayor del framework.

## 6. Auditoria minima
- Evidencia de pipeline por MR.
- Historial de aprobaciones por ambiente.
- Registro de cambios de configuracion del proyecto GitLab.

Referencias:
- docs/02-governance/BRANCHING_POLICY_BASE.md
- docs/02-governance/QUALITY_GATES.md
- docs/02-governance/SECURITY_AND_TEST_DATA_POLICY.md
- docs/02-governance/INTEGRACION_CON_PLANTILLA_DEVSECOPS.md
