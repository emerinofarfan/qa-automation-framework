# Gestión de Cambios y Participación de Comités

## Visión General

Este documento define los roles, responsabilidades y flujo de aprobaciones que participan en el ciclo de vida del pipeline CI/CD, desde el desarrollo hasta la puesta en producción. Se alinea con las mejores prácticas de ITIL v4 para la gestión de cambios.

---

## Participantes y Roles

### 1. Equipo de Desarrollo
**Responsabilidades:**
- Desarrollar funcionalidades en ramas `feature/*`.
- Escribir y mantener pruebas unitarias (cobertura mínima: 80%).
- Realizar code review en Merge Requests (peer review).
- Corregir hallazgos del análisis estático (SAST) antes de aprobar el MR.
- Documentar cambios en el CHANGELOG.

**Participación en el pipeline:**
- Activador de las etapas de test, SAST y build.
- Aprobador nivel 1 en Merge Requests hacia `develop`.

### 2. Líder Técnico / Arquitecto
**Responsabilidades:**
- Validar la calidad del código y el diseño de la solución.
- Aprobar Merge Requests hacia `develop` y `release/*`.
- Decidir cuándo crear una rama `release/*`.
- Autorizar el despliegue a UAT.

**Participación en el pipeline:**
- Aprobador del job `deploy:uat` (gate manual en GitLab).
- Revisor obligatorio en MR hacia `release/*`.

### 3. Área de Negocio (Product Owner / Usuarios Clave)
**Responsabilidades:**
- Definir y priorizar requerimientos funcionales.
- Ejecutar pruebas de aceptación de usuario (UAT) en el ambiente de UAT.
- Aprobar o rechazar la versión candidata.
- Firmar el acta de conformidad para paso a producción.

**Participación en el pipeline:**
- No interactúa directamente con GitLab.
- Su aprobación se registra como comentario en el Merge Request `staging → production` o mediante ticket en GLPI.
- Sin su conformidad, el Comité de Cambios no aprueba el despliegue.

### 4. Operaciones de TI (Infraestructura)
**Responsabilidades:**
- Mantener la plataforma de VMware vCenter y las plantillas de VMs.
- Administrar los runners de GitLab y los ambientes.
- Gestionar los accesos y credenciales (variables CI/CD).
- Monitorear la salud de los servidores JBoss EAP.
- Ejecutar o supervisar el aprovisionamiento de infraestructura.

**Participación en el pipeline:**
- Responsable de los jobs `provision:*` (activación manual).
- Monitoreo post-despliegue.
- Ejecución del rollback en caso de emergencia.

### 5. Seguridad Informática y Ciberseguridad
**Responsabilidades:**
- Definir y mantener las políticas de seguridad del pipeline.
- Revisar los reportes de SAST, detección de secretos y escaneo de dependencias.
- Aprobar o bloquear cambios con vulnerabilidades críticas o altas.
- Diseñar y validar las reglas de firewall en Panorama.
- Auditar los accesos y configuraciones de seguridad.

**Participación en el pipeline:**
- Revisor de los reportes de seguridad generados en el stage `sast`.
- Aprobador obligatorio en MR hacia `master` cuando se detectan hallazgos de seguridad.
- Responsable de los jobs `firewall:*` (validación y aprobación).
- Veto: puede bloquear un despliegue a producción si existen vulnerabilidades no mitigadas.

### 6. Comité de Cambios (CAB — Change Advisory Board)
**Composición:**
- Líder Técnico (presidente del comité para cambios tecnológicos)
- Representante de Operaciones de TI
- Representante de Seguridad Informática
- Representante del Área de Negocio (para cambios de alto impacto)
- Gerente de TI (opcional, para cambios críticos)

**Responsabilidades:**
- Evaluar el riesgo e impacto de los cambios propuestos.
- Aprobar, rechazar o solicitar modificaciones al cambio.
- Definir la ventana de mantenimiento para el despliegue.
- Validar que exista un plan de rollback documentado y probado.
- Registrar la decisión en GLPI (ticket de cambio).

---

## Flujo de Aprobaciones por Ambiente

### Desarrollo (automático)
```
Desarrollador → merge feature/* a master
  → Pipeline automático: SAST → Test → Build → Deploy Dev
  → No requiere aprobación manual de despliegue
```

### UAT (aprobación en el Merge Request)
```
Líder Técnico → abre MR: master → staging
  → Pipeline: SAST → Test → Build
  → Líder Técnico aprueba el MR → merge → deploy automático a UAT
  → Área de Negocio ejecuta pruebas UAT
  → Negocio firma conformidad
```

### Producción (aprobación del CAB en el Merge Request)
```
1. Se abre MR: staging → production
2. Se crea ticket de cambio automático en GLPI
3. CAB revisa en el Merge Request:
   - Resultados de SAST (sin críticos ni altos)
   - Cobertura de pruebas (≥80%)
   - Conformidad del negocio
   - Plan de rollback
   - Ventana de mantenimiento
4. CAB aprueba el MR → merge a production
5. Operaciones de TI → ejecuta job deploy:prod (gate manual final)
6. Post-despliegue: verificación de salud + monitoreo 24h
7. Tag automático: vX.Y.Z
```

---

## Clasificación de Cambios

| Tipo | Descripción | Aprobación | Ejemplo |
|---|---|---|---|
| **Estándar** | Cambios pre-aprobados, bajo riesgo | Automática | Actualización de textos, bug fixes menores |
| **Normal** | Cambios planificados, riesgo medio | CAB en reunión semanal | Nueva funcionalidad, actualización de dependencias |
| **Emergencia** | Corrección urgente en producción | CAB express (2 aprobadores) | Hotfix de seguridad, caída de servicio |

### Cambios Estándar (Pre-aprobados)
- Bug fixes menores que no afectan la lógica de negocio.
- Actualizaciones de documentación.
- Cambios cosméticos en el frontend.
- Pueden pasar a producción sin reunión del CAB, siempre que cumplan con SAST y pruebas.

### Cambios Normales
- Nuevas funcionalidades.
- Cambios en la lógica de negocio.
- Actualizaciones de infraestructura.
- Requieren reunión del CAB (semanal o bajo demanda).

### Cambios de Emergencia
- Vulnerabilidades de seguridad críticas (CVE con CVSS ≥ 9.0).
- Caída del servicio en producción.
- Flujo acelerado: 2 miembros del CAB pueden aprobar vía correo/chat.
- Se documenta retroactivamente en la siguiente reunión del CAB.

---

## Integración con GLPI

El pipeline crea automáticamente los siguientes registros en GLPI 10.0.18:

1. **Activos (Computers):** Cada servidor aprovisionado se registra como activo en el CMDB.
2. **Software:** La aplicación se registra con su versión actual.
3. **Tickets de Cambio:** Se crea automáticamente al intentar desplegar a producción, con:
   - Descripción del cambio
   - Link al pipeline de GitLab
   - Checklist de aprobaciones
   - Plan de rollback

El estado del ticket en GLPI se sincroniza con el avance del pipeline:
- **Nuevo:** Ticket creado, pendiente de revisión del CAB.
- **Aprobado:** CAB aprobó, listo para despliegue.
- **En proceso:** Despliegue en curso.
- **Resuelto:** Despliegue exitoso, verificación completada.
- **Cerrado:** Post-implementación revisada, sin incidentes.

---

## Matriz RACI

| Actividad | Desarrollo | Líder Técnico | Negocio | Ops TI | Seguridad | CAB |
|---|---|---|---|---|---|---|
| Desarrollo de features | **R** | A | I | — | — | — |
| Code review | R | **A** | — | — | I | — |
| Análisis SAST | R | I | — | — | **A** | — |
| Pruebas unitarias | **R** | A | — | — | — | — |
| Deploy a Desarrollo | **R** | I | — | I | — | — |
| Pruebas UAT | I | I | **R** | — | — | — |
| Deploy a UAT | I | **A** | I | R | I | — |
| Reglas de firewall | — | I | — | R | **A** | I |
| Aprovisionamiento infra | — | I | — | **R** | I | I |
| Aprobación producción | I | R | R | R | R | **A** |
| Deploy a Producción | — | I | I | **R** | I | A |
| Rollback | — | A | I | **R** | I | I |
| Registro en GLPI | — | I | — | **R** | I | I |

**R** = Responsible, **A** = Accountable, **C** = Consulted, **I** = Informed

---

## Calendario de Reuniones del CAB

| Reunión | Frecuencia | Participantes | Propósito |
|---|---|---|---|
| CAB Regular | Semanal (miércoles 10:00) | Todos | Revisar cambios normales pendientes |
| CAB Express | Bajo demanda | Min. 2 miembros | Aprobar cambios de emergencia |
| Post-Implementación | Tras cada release a prod | Todos | Revisar resultados, lecciones aprendidas |
