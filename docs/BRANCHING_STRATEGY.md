# Estrategia de Ramificación — GitLab Flow

## ¿Por qué GitLab Flow?

GitLab Flow combina la simplicidad de GitHub Flow con el soporte explícito para múltiples ambientes de despliegue. A diferencia de GitFlow, que mantiene dos ramas permanentes (`master` + `develop`) y genera merges redundantes, GitLab Flow usa una **única rama principal** como fuente de verdad y ramas de ambiente que reflejan exactamente lo que está desplegado en cada entorno.

### Comparativa

| Aspecto | GitFlow | GitLab Flow |
|---|---|---|
| Ramas permanentes | 2 (`master` + `develop`) | 1 (`master`) + ramas de ambiente |
| Complejidad de merges | Alta (release → master + develop) | Baja (flujo lineal descendente) |
| Ramas release | Temporales, manuales | No necesarias (el ambiente es la rama) |
| Hotfixes | Rama propia → merge doble | Cherry-pick o MR directo a `master` |
| Integración con GitLab | Parcial | Nativa (Environments, MR, CI/CD) |
| Trazabilidad | Dispersa en múltiples ramas | Lineal: master → staging → production |

---

## Modelo: GitLab Flow con Ramas de Ambiente

### Principio Fundamental — "Upstream First"

Todo cambio entra primero por `master` y fluye hacia abajo a los ambientes. Nunca se hace commit directo en una rama de ambiente; siempre se promueve desde la rama superior.

```
master  →  staging  →  production
 (dev)     (UAT)       (prod)
```

---

## Ramas del Proyecto

### `master` — Desarrollo e Integración
- Rama principal y **única fuente de verdad**.
- Recibe todos los Merge Requests desde `feature/*`.
- Cada commit dispara automáticamente el pipeline de **Desarrollo**.
- Siempre debe estar en estado "desplegable".
- Protegida: requiere Merge Request con al menos 1 aprobación (peer review).

### `staging` — Ambiente UAT
- Refleja exactamente lo que está desplegado en UAT.
- Se actualiza **exclusivamente** mediante Merge Request desde `master`.
- El MR hacia `staging` requiere aprobación del **Líder Técnico**.
- Aquí el Área de Negocio ejecuta las pruebas de aceptación.
- No se permiten commits directos ni merges desde otras ramas.

### `production` — Ambiente Productivo
- Refleja exactamente lo que está desplegado en producción.
- Se actualiza **exclusivamente** mediante Merge Request desde `staging`.
- El MR hacia `production` requiere aprobación del **Comité de Cambios (CAB)**.
- Cada merge genera automáticamente un **tag de versión**.
- No se permiten commits directos.

### `feature/<nombre>` — Funcionalidades
- Se crean desde `master`.
- Nombrado: `feature/JIRA-123-descripcion-breve`.
- Una vez completada, se abre Merge Request hacia `master`.
- Se elimina tras el merge.

### `hotfix/<nombre>` — Correcciones Urgentes
- Se crean desde `master` (upstream first).
- Se mergean a `master` y luego se promueven aceleradamente: `master → staging → production`.
- El CAB aprueba mediante procedimiento de emergencia.

---

## Flujo Visual

```
feature/A ──┐
feature/B ──┤
feature/C ──┘
             ↓  (Merge Request + peer review)
master       ●━━●━━●━━●━━●━━●━━●━━●━━●━━●━━━━━━━━━━
             │        │              │
             ↓ MR     ↓ MR           ↓ MR (hotfix)
staging      ●━━━━━━━━●━━━━━━━━━━━━━━●━━━━━━━━━━━━
                      │              │
                      ↓ MR (CAB)     ↓ MR (CAB express)
production            ●━━━━━━━━━━━━━━●━━━━━━━━━━━━━
                      ↑              ↑
                    tag v1.0.0     tag v1.0.1
```

### Flujo paso a paso para una funcionalidad

1. Desarrollador crea `feature/calcular-luhn` desde `master`.
2. Desarrolla, hace commits, push.
3. Abre **Merge Request → `master`**: se ejecuta pipeline (SAST + Test + Build).
4. Peer review aprueba → se mergea a `master` → deploy automático a **Desarrollo**.
5. Cuando se acumula una versión lista para UAT, se abre **MR: `master` → `staging`**.
6. Líder Técnico aprueba → deploy a **UAT** → Negocio prueba.
7. Negocio da conformidad → se abre **MR: `staging` → `production`**.
8. CAB aprueba → deploy a **Producción** → se crea tag.

### Flujo de hotfix

1. Se crea `hotfix/fix-overflow` desde `master`.
2. Se corrige y se abre **MR → `master`** (con la corrección).
3. Se mergea a `master` → deploy a desarrollo (verificación).
4. Se promueve inmediatamente: **`master` → `staging`** (verificación UAT rápida).
5. Se promueve: **`staging` → `production`** (CAB express: 2 aprobadores).

---

## Reglas de Protección en GitLab

| Rama | Push directo | MR requerido | Aprobaciones mínimas | Pipeline obligatorio |
|---|---|---|---|---|
| `production` | Prohibido | Sí (solo desde `staging`) | 3 (CAB + Líder + Seguridad) | Sí |
| `staging` | Prohibido | Sí (solo desde `master`) | 2 (Líder Técnico + QA) | Sí |
| `master` | Prohibido | Sí | 1 (Peer review) | Sí |
| `feature/*` | Permitido | No | — | Sí (SAST + tests) |
| `hotfix/*` | Permitido | No | — | Sí |

### Configuración en GitLab (Settings → Repository → Protected Branches)

Para `production`:
- Allowed to merge: Maintainers
- Allowed to push: No one
- Require approval: 3 approvals
- Allowed merge sources: `staging` only

Para `staging`:
- Allowed to merge: Maintainers
- Allowed to push: No one
- Require approval: 2 approvals
- Allowed merge sources: `master` only

---

## Política de Tags y Versionado

Se utiliza **Semantic Versioning (SemVer)**: `MAJOR.MINOR.PATCH`

Los tags se crean automáticamente al mergear a `production`:

```bash
v1.0.0   # Primer release
v1.1.0   # Nueva funcionalidad
v1.1.1   # Hotfix
v2.0.0   # Cambio mayor
```

---

## Convenciones de Commits

Se utiliza **Conventional Commits**:

```
feat: agregar cálculo de dígito Luhn
fix: corregir overflow en números largos
docs: actualizar documentación del API
test: agregar pruebas paramétricas
ci: actualizar stage de SAST
refactor: extraer validación a método privado
chore: actualizar dependencias de Maven
```

---

## Ventajas de Este Modelo para Nuestro Contexto

1. **Trazabilidad**: Cada rama de ambiente es una fotografía exacta de lo desplegado. Si alguien pregunta "¿qué hay en UAT?", la respuesta es `staging`.

2. **Auditoría**: El historial de Merge Requests entre ramas de ambiente documenta cada promoción, quién la aprobó y cuándo.

3. **Simplicidad**: No hay ramas `release/*` temporales ni merges dobles. El flujo es siempre lineal y descendente.

4. **Integración nativa con GitLab**: Los Environments de GitLab mapean directamente a las ramas. Los deployment badges, los rollback buttons y el historial de ambientes funcionan sin configuración adicional.

5. **Compatibilidad con CAB**: El MR de `staging → production` es el punto natural de control donde el Comité de Cambios revisa, comenta y aprueba (o rechaza).
