# Branching Policy - Framework Base QA

## 1. GitLab Flow corporativo
Flujo oficial:
- feature/* -> master -> staging -> production
- hotfix/* -> master -> staging -> production

Ramas y ambientes:
- master: integracion continua (desarrollo).
- staging: validacion UAT.
- production: liberacion productiva.

## 2. Reglas obligatorias por rama protegida
- No push directo a master, staging o production.
- Merge Request obligatorio para ramas protegidas.
- Pipeline exitoso obligatorio antes de merge.
- Resolver discusiones del MR antes de aprobar.

## 3. Aprobaciones minimas por ambiente
- MR a master: 1 aprobacion (peer review).
- MR a staging: 2 aprobaciones (lider tecnico).
- MR a production: 3 aprobaciones (CAB).

## 4. Politica recomendada en GitLab (Project Settings)
- Protect branch master/staging/production.
- Allowed to push: No one.
- Allowed to merge: Maintainers (o rol definido por gobierno TI).
- Require approval rule por rama segun ambiente.
- Require successful pipeline: habilitado.

## 5. Nomenclatura recomendada para ramas QA
- feature/qa-<modulo>-<objetivo-corto>
- hotfix/qa-<incidente>-<objetivo-corto>
- chore/qa-<actividad-corta>

## 6. Referencias obligatorias
- docs/02-governance/COMMIT_AND_BRANCH_NAMING_POLICY.md
- docs/02-governance/QUALITY_GATES.md
- docs/BRANCHING_STRATEGY.md
