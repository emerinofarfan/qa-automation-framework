# Gestión de Entornos — Cómo ejecutar tests en el ambiente correcto

## Los 3 entornos de QA Automation

| Entorno | Rama GitLab | URL base | Propósito | ¿Tests destructivos? |
|---|---|---|---|---|
| **TEST** (Desarrollo) | `master` / `feature/*` | `testwebcastigos.example.pe` | Integración continua, pruebas diarias | ✅ Sí, con cuidado |
| **UAT** (Staging) | `staging` | `uatwebcastigos.example.pe` | Aceptación del negocio, pre-release | ⚠️ Solo con aprobación |
| **PRODUCCIÓN** | `production` | `webcastigos.example.pe` | Sistema real, clientes reales | ❌ NUNCA |

> **Regla absoluta:** Nunca ejecutes `@Destructive` contra producción.
> Nunca ejecutes nada contra producción sin autorización explícita del líder técnico.

---

## Configuración local — cómo apuntar a cada entorno

### Opción 1: Variables de entorno en `.env` (recomendado)

Tu archivo `.env` en `qa-automation/` controla el entorno local:

```bash
# Para TEST (trabajo diario)
TEST_BASE_URL=https://testwebcastigos.example.pe/Sample ApplicationAutomatizados/auth
TEST_USERNAME=tu_usuario_test
TEST_PASSWORD=tu_clave_test

# Para apuntar a UAT temporalmente (necesitas credenciales de UAT)
# TEST_BASE_URL=https://uatwebcastigos.example.pe/Sample ApplicationAutomatizados/auth
# TEST_USERNAME=tu_usuario_uat
# TEST_PASSWORD=tu_clave_uat
```

Descomenta la línea del entorno que necesitas y comenta la que no usas.

### Opción 2: Por parámetro en el comando (sin modificar `.env`)

```bash
# Apuntar a TEST
./gradlew test \
  -Dbase.url="https://testwebcastigos.example.pe/Sample ApplicationAutomatizados/auth" \
  -Dtest.username="mi_usuario" \
  -Dtest.password="mi_clave" \
  -Dcucumber.filter.tags="@Smoke"

# Apuntar a UAT
./gradlew test \
  -Dbase.url="https://uatwebcastigos.example.pe/Sample ApplicationAutomatizados/auth" \
  -Dtest.username="mi_usuario_uat" \
  -Dtest.password="mi_clave_uat" \
  -Dcucumber.filter.tags="@Smoke"
```

Los parámetros `-D` tienen prioridad sobre el `.env` y el `config.properties`.

---

## Configuración en GitLab CI/CD — cómo lo hace el pipeline

El pipeline usa variables configuradas en GitLab por entorno.
Joel o el administrador las configura en **Settings → CI/CD → Variables**.

### Variables por ambiente

| Variable | Entorno TEST | Entorno UAT | Entorno PROD |
|---|---|---|---|
| `TEST_BASE_URL` | URL de TEST | URL de UAT | URL de PROD |
| `TEST_USERNAME` | Usuario TEST | Usuario UAT | Usuario PROD |
| `TEST_PASSWORD` | Clave TEST (masked) | Clave UAT (masked) | Clave PROD (masked) |

Cada variable se marca como **"Protected"** para que solo la rama
correspondiente pueda usarla.

### Cómo funciona automáticamente

```
Push a feature/*  → GitLab usa variables de TEST  → corre @Smoke contra TEST
Push a staging    → GitLab usa variables de UAT   → corre @Regression contra UAT
Push a production → GitLab usa variables de PROD  → corre @Regression contra PROD
```

No necesitas hacer nada. El pipeline detecta la rama y usa las credenciales correctas.

---

## Qué corre en cada entorno y por qué

### Entorno TEST — pruebas diarias

```bash
# Lo que corre el pipeline automáticamente en cada MR:
./gradlew test -Dcucumber.filter.tags="@Smoke"

# Lo que puedes correr manualmente durante desarrollo:
./gradlew test -Dcucumber.filter.tags="@Smoke"
./gradlew test -Dcucumber.filter.tags="@Regression and not @Destructive"
```

Objetivo: detectar regresiones rápido antes de mergear.

### Entorno UAT — preproducción

```bash
# Lo que corre el pipeline en staging:
./gradlew test -Dcucumber.filter.tags="@Regression and not @Destructive"
```

Objetivo: validar que todo funciona antes de promover a producción.
El negocio también hace pruebas manuales aquí.

### Entorno PRODUCCIÓN — post-deploy

```bash
# Solo @Smoke, sin datos destructivos, inmediatamente después del deploy:
./gradlew test \
  -Dcucumber.filter.tags="@Smoke and not @Destructive" \
  -Dcucumber.execution.parallel.enabled=false
```

Objetivo: confirmar que el deploy no rompió nada crítico.
Si algún @Smoke falla en producción → rollback inmediato.

---

## Verificar contra qué entorno estás corriendo

Antes de ejecutar, confirma el entorno:

```bash
# Ver qué URL está configurada en tu .env
grep TEST_BASE_URL .env

# O ver qué retorna ConfigManager
./gradlew test -Dcucumber.filter.tags="@Smoke" --info 2>&1 | grep "base.url"
```

Allure también registra la URL en cada escenario fallido.

---

## Diferencias de datos entre entornos

| Dato | TEST | UAT | PRODUCCIÓN |
|---|---|---|---|
| Usuarios | Usuarios de prueba ficticios | Usuarios de prueba del negocio | Clientes reales |
| Cuentas | Cuentas de prueba con saldo ficticio | Cuentas de prueba del negocio | Cuentas reales |
| Transacciones | Se pueden crear y eliminar libremente | Solo con aprobación del área | NUNCA automatizar |
| Datos PII | No — datos sintéticos | Datos anonimizados | Datos reales (no tocar) |

---

## Checklist antes de ejecutar en un entorno no habitual

Antes de apuntar a UAT o producción con un test local:

- [ ] Tengo credenciales del entorno correcto (no las de TEST)
- [ ] El líder técnico autorizó la ejecución en ese entorno
- [ ] No estoy corriendo tags `@Destructive`
- [ ] Sé qué datos modificará el test y cómo restaurarlos
- [ ] Tengo el reporte de Allure listo para evidenciar la ejecución

---

*Guía de gestión de entornos v1.0 — QA Automation QA Automatización — 2026-03-15*
