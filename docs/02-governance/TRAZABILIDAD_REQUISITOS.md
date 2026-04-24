# Trazabilidad de Requisitos — Mapa Test ↔ Historia de Usuario

## ¿Por qué es obligatoria en banca?

La SBS y las auditorías internas exigen que cada prueba automatizada pueda
responderse con estas preguntas:

- ¿Qué requisito valida este test?
- ¿Quién aprobó ese requisito?
- ¿Cuándo se ejecutó la prueba y cuál fue el resultado?

Sin trazabilidad, el equipo de auditoría rechaza la evidencia. El pipeline
puede pasar perfectamente y aun así no cumplir el requisito de control interno.

---

## El sistema de trazabilidad de este framework

Usamos **tags de Cucumber** para vincular cada escenario a su origen:

```
@REQ-{ID}     → Requisito funcional en GitLab Issues o Jira
@HU-{ID}      → Historia de Usuario del sprint
@BUG-{ID}     → Escenario que reproduce y valida el fix de un defecto
```

### Ejemplo en un feature:

```gherkin
@Smoke @REQ-042 @HU-118
Scenario: Transferencia exitosa entre cuentas propias
  Given el cliente tiene saldo disponible en cuenta origen
  When realiza una transferencia de S/. 100.00 a su cuenta de ahorros
  Then el saldo de la cuenta origen se reduce en S/. 100.00
  And el saldo de la cuenta destino aumenta en S/. 100.00
  And se genera el comprobante de operación con número de referencia

@Regression @Negativo @REQ-043
Scenario: Transferencia rechazada por saldo insuficiente
  Given el cliente tiene S/. 50.00 de saldo disponible
  When intenta transferir S/. 200.00
  Then la operación es rechazada con el mensaje "Saldo insuficiente"
  And el saldo de ambas cuentas permanece sin cambios
```

---

## Convención de IDs

| Prefijo | Fuente | Quién lo asigna | Ejemplo |
|---|---|---|---|
| `@REQ-` | GitLab Issue / Confluence | Product Owner o BA | `@REQ-042` |
| `@HU-` | Historia de usuario del sprint | Scrum Master | `@HU-118` |
| `@BUG-` | Ticket de defecto | QA que reportó | `@BUG-7731` |

**Regla:** Un escenario puede tener varios tags de trazabilidad si cubre
múltiples requisitos. Un requisito puede tener varios escenarios.

---

## Cómo Allure renderiza la trazabilidad

Allure muestra los tags de cada escenario en su reporte.
Para que aparezcan como "issues" clicables que abren GitLab:

En `src/test/resources/allure.properties` (créalo si no existe):

```properties
allure.issues.tracker.pattern=https://gitlab.example.pe/grupo/proyecto/-/issues/%s
allure.tms.tracker.pattern=https://gitlab.example.pe/grupo/proyecto/-/issues/%s
```

Con esto, `@REQ-042` en el reporte se convierte en un link directo al issue #42.

---

## Matriz de trazabilidad — cómo generarla

La matriz vincula cada requisito con sus escenarios y su estado de ejecución.
Se genera a partir de los resultados de Allure con el siguiente proceso:

### 1. Ejecutar los tests con Allure habilitado (ya está configurado)
```bash
./gradlew test
```

### 2. Extraer la matriz desde los resultados

Los archivos JSON de Allure en `build/allure-results/` contienen los tags.
Un script básico para extraer la matriz:

```bash
# Listar todos los escenarios con sus tags REQ/HU
grep -r "\"@REQ-\|\"@HU-" build/allure-results/*.json | \
  grep -oP '"@(REQ|HU|BUG)-\d+"' | sort | uniq -c | sort -rn
```

### 3. Formato de la matriz para auditoría

| ID Requisito | Descripción | Escenario | Feature | Última ejecución | Estado |
|---|---|---|---|---|---|
| REQ-042 | Transferencia entre cuentas propias | Transferencia exitosa... | Transferencia.feature | 2026-03-15 | ✅ PASS |
| REQ-043 | Rechazo por saldo insuficiente | Transferencia rechazada... | Transferencia.feature | 2026-03-15 | ✅ PASS |
| REQ-044 | Límite diario de transferencias | Sin escenario automatizado | — | — | ⚠️ SIN COBERTURA |

---

## Criterio de cobertura mínima para auditoría

| Tipo de requisito | Cobertura mínima requerida |
|---|---|
| Requisitos críticos (flujos de dinero) | 100% con al menos 1 @Smoke |
| Requisitos funcionales estándar | 80% |
| Requisitos no funcionales (performance) | Documentados, automatización opcional |

Un requisito sin escenario asociado debe documentarse como
**"cobertura manual"** con referencia al caso de prueba manual correspondiente.

---

## Responsabilidades

| Rol | Responsabilidad |
|---|---|
| QA Ejecutor | Agrega los tags `@REQ-` y `@HU-` al escribir el escenario |
| QA Senior | Revisa que la cobertura sea correcta en cada MR |
| Líder QA | Genera y entrega la matriz de trazabilidad en cada release |
| Auditoría | Recibe el reporte de Allure + la matriz como evidencia |

---

## Qué NO hacer

```gherkin
# MAL — escenario sin trazabilidad, rechazado en auditoría
@Smoke
Scenario: Transferencia
  Given tengo saldo
  When transfiero
  Then funciona

# BIEN — con trazabilidad completa
@Smoke @REQ-042 @HU-118
Scenario: Transferencia exitosa entre cuentas propias del cliente
  Given el cliente tiene S/. 500.00 de saldo disponible en cuenta corriente
  When realiza una transferencia de S/. 100.00 a su cuenta de ahorros
  Then ambos saldos reflejan la operación correctamente
```

---

*Política de trazabilidad v1.0 — QA Automation QA Automatización — 2026-03-15*
