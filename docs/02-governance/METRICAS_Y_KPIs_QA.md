# Métricas y KPIs de QA Automatización — QA Automation

## ¿Por qué medir?

Sin métricas, la automatización es una caja negra para el liderazgo.
Con métricas, puedes demostrar valor, detectar degradación y tomar decisiones
basadas en datos.

---

## Los 5 KPIs fundamentales

---

### KPI 1: Tasa de éxito de pruebas (Pass Rate)

**Definición:** Porcentaje de escenarios que pasan sobre el total ejecutado.

```
Pass Rate = (Escenarios PASS / Total escenarios ejecutados) × 100
```

**Umbrales para QA Automation:**

| Suite | Target | Alerta | Crítico |
|---|---|---|---|
| @Smoke (MR) | ≥ 98% | < 98% | < 90% |
| @Regression (staging) | ≥ 92% | < 92% | < 80% |
| @Regression (producción) | ≥ 95% | < 95% | < 85% |

**Acción por nivel:**
- **Alerta:** Investigar en el mismo día. Puede ser flakiness o ambiente inestable.
- **Crítico:** Bloquear la promoción. Escalar al líder técnico inmediatamente.

**Cómo medirlo:** El reporte de Allure muestra el pass rate automáticamente.
El pipeline de GitLab también publica el badge de resultado en el MR.

---

### KPI 2: Tasa de flakiness (Flakiness Rate)

**Definición:** Porcentaje de escenarios que no tienen resultado consistente
(pasan en una ejecución y fallan en la siguiente sin cambio de código).

```
Flakiness Rate = (Escenarios que fallaron y pasaron en rerun / Total ejecutados) × 100
```

**Umbrales:**

| Nivel | Valor | Acción |
|---|---|---|
| Saludable | < 2% | Monitoreo rutinario |
| Preocupante | 2% – 5% | Revisar y priorizar corrección en el sprint |
| Crítico | > 5% | Sprint de estabilización antes de continuar |

**Cómo medirlo:** El rerun de Cucumber genera `build/rerun.txt` con los fallos.
Si un escenario pasa en el rerun, es flaky. Registra estos en GitLab Issues con
label `flaky-test`.

**Cómo reducirla:**
- Reemplazar `Thread.sleep()` por esperas explícitas
- Verificar que los locators son estables (preferir `id` sobre `xpath`)
- Asegurarse que los tests son independientes entre sí (no comparten estado)
- Revisar si el ambiente de TEST tiene lentitud intermitente

---

### KPI 3: Tiempo de ejecución de la suite

**Definición:** Cuánto tiempo tarda en completarse cada suite.

**Umbrales para QA Automation:**

| Suite | Target | Alerta |
|---|---|---|
| @Smoke (3 threads) | ≤ 5 min | > 8 min |
| @Regression completa (5 threads) | ≤ 25 min | > 35 min |
| Pipeline completo (sast+test+build+deploy) | ≤ 45 min | > 60 min |

**Por qué importa en banca:** Un pipeline lento bloquea merges y despliegues.
Si el pipeline tarda 2 horas, los desarrolladores dejan de correrlo.

**Cómo medirlo:** GitLab muestra la duración de cada job en la vista del pipeline.
Allure también registra la duración de cada escenario.

**Causas comunes de degradación de tiempo:**
- Tests que usan `waitForSeconds(30)` innecesariamente
- Aumento de escenarios sin ajuste de parallelism
- Ambiente lento (problema de infraestructura, no del test)

---

### KPI 4: Cobertura de requisitos automatizados

**Definición:** Porcentaje de requisitos funcionales con al menos un escenario @Smoke.

```
Cobertura = (Requisitos con @REQ-tag / Total requisitos del sprint) × 100
```

**Umbrales:**

| Categoría de requisito | Cobertura mínima |
|---|---|
| Flujos críticos de dinero (transferencias, pagos) | 100% |
| Funcionalidades principales | 80% |
| Funcionalidades secundarias | 50% |
| Casos de borde y negativos | 60% |

**Cómo medirlo:** Contar los `@REQ-` tags únicos en los features vs.
el total de requisitos del sprint en GitLab Issues.

---

### KPI 5: Densidad de defectos detectados por automatización

**Definición:** Cuántos defectos encuentra la automatización antes de que
lleguen a producción.

```
Defectos en CI = Defectos encontrados por @Smoke o @Regression en pipeline
Defectos en PROD = Defectos reportados por usuarios reales
Efectividad = Defectos en CI / (Defectos en CI + Defectos en PROD) × 100
```

**Target:** Que el 80% de los defectos se detecte en el pipeline (CI), no en producción.

**Cómo rastrearlo:** Cuando un `@Smoke` falla, abrir un GitLab Issue con
label `defecto-detectado-por-qa`. Al cierre del release, comparar con
los defectos reportados post-deploy.

---

## Reporte semanal de métricas — template

El líder QA envía este resumen cada viernes al canal de equipo:

```
📊 REPORTE QA SEMANAL — Semana del [fecha] al [fecha]

Suite @Smoke:
  ✅ Pass Rate: 97% (target ≥98%) ← ALERTA
  ⏱  Tiempo promedio: 4.2 min
  🔁 Flakiness: 1.2% (3 escenarios inestables)

Suite @Regression (última ejecución en staging):
  ✅ Pass Rate: 94%
  ⏱  Tiempo: 22 min
  🔁 Flakiness: 0.8%

Cobertura de requisitos:
  📌 Sprint actual: 12/15 requisitos automatizados (80%)
  ⚠️ Sin cobertura: REQ-051, REQ-053, REQ-056

Defectos detectados esta semana:
  🐛 En CI (antes de producción): 4
  🔴 En producción: 1
  📈 Efectividad de detección: 80%

Acciones pendientes:
  • Estabilizar 3 escenarios flaky (asignado a: [nombre])
  • Automatizar REQ-051 (asignado a: [nombre])
```

---

## Reporte mensual para liderazgo — indicadores clave

| Indicador | Mes anterior | Mes actual | Tendencia |
|---|---|---|---|
| Escenarios totales | 45 | 62 | ↑ +38% |
| Pass Rate promedio @Smoke | 96% | 98% | ↑ |
| Flakiness promedio | 3.2% | 1.8% | ↓ (mejora) |
| Tiempo pipeline promedio | 38 min | 31 min | ↓ (mejora) |
| Requisitos cubiertos | 68% | 80% | ↑ |
| Defectos detectados en CI | 8 | 14 | ↑ (framework más efectivo) |
| Defectos en producción | 3 | 1 | ↓ (mejora) |

---

## Herramientas para medir

| KPI | Herramienta | Dónde ver |
|---|---|---|
| Pass Rate | Allure Report | `build/reports/allure-report/index.html` |
| Flakiness | `build/rerun.txt` + GitLab Issues | Después de cada ejecución |
| Tiempo de ejecución | GitLab Pipeline view | Página del pipeline en GitLab |
| Cobertura de requisitos | Tags `@REQ-` en features | Conteo manual o script |
| Defectos detectados | GitLab Issues | Etiqueta `defecto-detectado-por-qa` |

---

*Política de métricas QA v1.0 — QA Automation — 2026-03-15*
