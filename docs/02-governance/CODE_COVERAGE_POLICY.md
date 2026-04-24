# Política de Cobertura de Código — QA Automation Framework

## ¿Qué es la cobertura de código y por qué importa en banca?

La cobertura de código (code coverage) mide **qué porcentaje del código fuente
se ejecuta durante las pruebas**. La herramienta usada en este framework es
**JaCoCo** (Java Code Coverage), ya integrada en `build.gradle`.

En el contexto de QA Automation, esta política responde a dos necesidades:

1. **Calidad interna:** Garantiza que las clases de utilidad del framework
   (DriverFactory, ConfigManager, SensitiveDataMasker, etc.) estén siendo
   ejercidas por los tests y no tengan código muerto o sin validar.

2. **Auditoría y cumplimiento:** Organismos como la SBS pueden solicitar
   evidencia de que los sistemas de testing tienen controles de calidad.
   JaCoCo genera reportes en XML y HTML que pueden adjuntarse a auditorías.

---

## ¿Es obligatoria esta política para el framework de QA?

**Sí, como documentación. Opcional como gate de build en v1.0.**

La cobertura de código se mide típicamente en el sistema bajo prueba
(el backend de Spring Boot, medido por JaCoCo en `test.yml`).
En un framework de automatización QA, la cobertura mide qué tanto se usa
el propio código del framework durante la ejecución de los escenarios.

Esto tiene valor real:
- Si `SensitiveDataMasker` tiene código que nunca se ejecuta → puede
  haber una ruta de enmascaramiento que falla silenciosamente.
- Si `ConfigManager.getSlowTimeout()` nunca se llama → puede estar
  configurado incorrectamente sin que nadie lo note.

---

## Umbrales por ambiente

| Ambiente | Cobertura mínima | Acción si no se cumple |
|---|---|---|
| Desarrollo (feature branch) | 60% | Warning en reporte, no bloquea |
| Master | 65% | Bloquea el merge (build falla) |
| Staging | 70% | Bloquea la promoción |
| Producción | 70% | Bloquea el despliegue |

> **Nota para v1.0:** Los umbrales de master/staging/producción se activarán
> a partir de la v1.1 cuando la suite tenga al menos 10 escenarios reales.
> Durante la v1.0, JaCoCo genera el reporte pero no bloquea el build.

---

## Cómo generar el reporte localmente

```bash
# Ejecuta los tests y genera el reporte de cobertura
./gradlew test jacocoTestReport

# El reporte HTML se genera en:
# build/jacoco/html/index.html
# Ábrelo en el navegador para ver el detalle línea por línea.
```

---

## Cómo activar el gate de cobertura (cuando estés listo)

Agrega este bloque al final de `build.gradle` y ajusta el umbral:

```gradle
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                // Porcentaje mínimo de instrucciones cubiertas (0.65 = 65%)
                minimum = 0.65
            }
        }
    }
}

// Hacer que el build falle si no se cumple el umbral
check.dependsOn jacocoTestCoverageVerification
```

---

## Clases excluidas de la medición

Algunas clases no se incluyen en el cálculo porque son:
- Código de ejemplo/template (no van a producción)
- Runners que solo configuran Cucumber
- Clases de modelo sin lógica (POJOs)

Si necesitas excluir una clase, agrega en `build.gradle`:

```gradle
jacocoTestReport {
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                'runner/**',
                'examples/**',
                'models/**'
            ])
        }))
    }
}
```

---

## Cobertura del sistema bajo prueba (backend)

La cobertura del código de la aplicación Java (Spring Boot) es responsabilidad
del equipo de desarrollo y está configurada en `.gitlab/ci/test.yml`.
El umbral del backend se define en `backend/pom.xml` por el líder técnico.

Estos son dos mediciones independientes:

| Qué se mide | Herramienta | Responsable | Dónde se configura |
|---|---|---|---|
| Código del framework QA | JaCoCo (Gradle) | QA Automatizador | `qa-automation/build.gradle` |
| Código del backend Java | JaCoCo (Maven) | Desarrollador backend | `backend/pom.xml` |

---

## Historial de versiones

| Versión | Fecha | Cambio |
|---|---|---|
| 1.0 | 2026-03-15 | Política inicial — JaCoCo configurado, gate desactivado en v1.0 |
