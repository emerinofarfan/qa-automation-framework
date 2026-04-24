# QA Automation Standard v1.0 - QA Automation

## 1. Objetivo
Establecer un estandar unico para que todos los equipos QA automaticen pruebas con la misma arquitectura, las mismas reglas y la misma evidencia.

## 2. Alcance
Aplica a cualquier equipo QA que use esta plantilla para automatizacion funcional web.

Incluye:
- Estandar BDD con Cucumber.
- Estandar tecnico Selenium + Java.
- Convenciones de nombres.
- Quality gates para CI/CD.
- Reglas minimas para Merge Request.

No incluye (porque ya esta normado por corporativo):
- Reglas generales de codificacion Java fuera del contexto QA.
- Convenciones de arquitectura backend/frontend de aplicaciones.

Referencia corporativa obligatoria:
- **STD codificacion JAVA** (QA Automation).

## 3. Principios del estandar
- Reutilizacion antes que duplicacion.
- Lenguaje de negocio en features.
- Confiabilidad de ejecucion antes que volumen de casos.
- Trazabilidad completa de evidencia.
- Regla corporativa: sin quality gate, no merge.

## 4. Arquitectura base obligatoria
Estructura minima esperada por proyecto:

- src/test/java/core
- src/test/java/pages
- src/test/java/steps
- src/test/java/hooks
- src/test/java/runner
- src/test/java/utils
- src/test/resources/features
- src/test/resources/config.properties

## 5. Definicion de Ready (DoR) para automatizar
Un caso funcional entra a automatizacion solo si:
- Tiene criterio de aceptacion claro y verificable.
- Tiene datos de prueba definidos.
- Tiene ambiente objetivo disponible.
- Tiene identificadores UI estables (id, data-testid o equivalente).

## 6. Definicion de Done (DoD) para cerrar automatizacion
Una automatizacion se considera terminada solo si:
- Compila y ejecuta localmente.
- Corre en CI sin fallas.
- Reporta evidencia en Allure.
- Cumple convenciones de nombre.
- Cumple STD codificacion JAVA para la parte de codigo Java.
- Cumple quality gates del ambiente.

## 7. Responsabilidades operativas
- QA Arquitectura: define y versiona este estandar.
- Equipo QA de proyecto: implementa pages/steps/features del dominio.
- Lider tecnico: aprueba MR segun gates y criticidad.

## 8. Matriz oficial de madurez por rol

Esta matriz es obligatoria como referencia de adopcion en el framework.
No reemplaza evaluaciones de desempeno de RR.HH.; define expectativas tecnicas
minimas por rol para operar con calidad en automatizacion bancaria.

| Rol | Nivel Inicial | Nivel Operativo | Nivel Avanzado |
|---|---|---|---|
| QA Ejecutor | Ejecuta `@Smoke` en secuencial, usa plantillas y respeta estructura base. | Implementa features/steps/pages completos sin duplicidad y con evidencia de MR. | Optimiza estabilidad, propone refactors y reduce flakiness sistematicamente. |
| QA Senior / Referente Tecnico | Guia a equipos en estandares base y revisa cumplimiento de MR checklist. | Gobierna diseno de suites por tags, paralelismo y uso de datos no sensibles. | Define estrategia de escalamiento (capacidad, riesgo, cobertura) y criterios de calidad por dominio. |
| Lider QA | Exige quality gates y aprobaciones por ambiente en los flujos del equipo. | Gestiona prioridades de automatizacion por riesgo de negocio y deuda tecnica. | Toma decisiones de liberacion con evidencia auditable y gestion de excepciones. |
| QA Arquitectura | Custodia arquitectura base, convenciones y politicas de seguridad del framework. | Estandariza integracion con CI/CD corporativo y controles de trazabilidad. | Versiona el estandar, define roadmap transversal y regula cambios estructurales multi-equipo. |

Reglas de uso institucional:
- Toda iniciativa nueva debe identificar rol objetivo y nivel esperado de salida.
- Los planes de onboarding deben usar progresion: Inicial -> Operativo -> Avanzado.
- En auditorias internas, la evidencia de adopcion se valida con MR, pipeline y checklist.

## 9. Versionado del estandar
- MAJOR: cambios incompatibles.
- MINOR: nuevas reglas o capacidades compatibles.
- PATCH: correcciones de redaccion o ajustes menores.

## 10. Cumplimiento
Toda excepcion debe registrarse en el MR con justificacion tecnica, riesgo y fecha objetivo de regularizacion.
