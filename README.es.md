# Framework de Automatización QA

Framework de automatización de pruebas de nivel empresarial construido con Java 21, Gradle, Selenium y Cucumber. La arquitectura está pensada para ambientes regulados, servicios financieros y equipos QA que necesitan escalabilidad, trazabilidad y calidad sostenida.

Versión en inglés: [README.md](README.md)

Fuente de verdad: inglés. Esta versión en español se mantiene para onboarding, adopción local y presentaciones bilingües.

## 🎯 Qué demuestra este framework

✅ **BDD**: archivos Feature en Gherkin para escenarios legibles
✅ **Selenium 4**: automatización moderna de web con la API más reciente
✅ **Ejecución paralela**: paralelismo de 3 hilos para retroalimentación más rápida
✅ **Page Object Model**: estructura escalable y mantenible
✅ **Allure Reporting**: reportes completos con evidencia y métricas
✅ **Calidad de código**: JaCoCo, Spotless y Checkstyle
✅ **Seguridad**: análisis de CVE y gestión automatizada de dependencias
✅ **Patrones empresariales**: arquitectura real aplicada desde entornos productivos

## 🚀 Inicio rápido

### Prerrequisitos
- **Java**: 21 o superior
- **Git**: para clonar el repositorio
- **Navegador**: Chrome, Firefox o Edge compatibles con Selenium

### Configuración (3 pasos)

```bash
# 1. Clonar el repositorio
git clone https://github.com/emerinofarfan/qa-automation-framework.git
cd qa-automation-framework

# 2. Copiar plantilla de variables
cp .env.example .env
# Editar .env con la URL objetivo de tu aplicación

# 3. Ejecutar pruebas
./gradlew test "-Dcucumber.filter.tags=@Smoke"

# Opcional: ejecutar suite completa con destructivos (opt-in explícito)
./gradlew testAll
```

### Generar reportes

```bash
# Ejecutar pruebas y generar Allure
./gradlew test allureReport
open build/reports/allure/allureReport/index.html

# Reporte de cobertura
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

## 📁 Estructura del proyecto

```
src/test/
├── java/
│   ├── config/              # Configuración de ambiente y pruebas
│   ├── hooks/               # Setup/teardown de Cucumber
│   ├── models/              # Modelos y objetos de datos
│   ├── pages/               # Clases Page Object Model
│   ├── steps/               # Definición de pasos Cucumber
│   ├── utils/               # Utilidades y helpers
│   └── runner/              # Configuración de ejecución
└── resources/
    ├── features/            # Features BDD en Gherkin
    └── test/                # Archivos de configuración de pruebas
```

## 🏗️ Aspectos destacados de arquitectura

### Page Object Model
Encapsula selectores y acciones de cada pantalla, reduciendo fragilidad:
```java
public class LoginPage {
    private WebDriver driver;
    private By usernameField = By.id("username");
    
    public void login(String username, String password) {
        // Acciones específicas de la pantalla
    }
}
```

### Feature Files (BDD)
Escenarios legibles por negocio en Gherkin:
```gherkin
Feature: Autenticación de usuario
  Scenario: Inicio de sesión válido
    Given el usuario está en la página de login
    When ingresa credenciales válidas
    Then es redirigido al dashboard
```

### Paralelismo
Configuración por defecto: **3 hilos concurrentes**
- Configurable en `build.gradle`: `parallel = true`, `maxWorkers = 3`
- Ideal para pipelines CI/CD

## 📊 Stack tecnológico

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| Java | 21 LTS | Lenguaje y runtime |
| Gradle | 8.x | Automatización de build |
| Selenium | 4.18.1 | Automatización web |
| Cucumber | 7.18.1 | Framework BDD |
| JUnit | 5 | Ejecución de pruebas |
| Allure | 2.27.0 | Reportería de pruebas |

## 🔒 Calidad y seguridad

- ✅ Análisis de CVE con actualizaciones automatizadas
- ✅ Formateo con Spotless
- ✅ Análisis estático con Checkstyle
- ✅ Trazabilidad de cobertura con JaCoCo
- ✅ Logging completo con Logback

## 🎓 Cómo adaptar este framework

### 1. Cambiar el paquete
Reemplaza `com.example.qaautomation` por el nombre de paquete de tu organización.

### 2. Actualizar configuración
Edita `.env` con los datos de tu aplicación:
```properties
BASE_URL=https://tu-app.com
API_BASE_URL=https://api.tu-app.com
TEST_USERNAME=testuser
TEST_PASSWORD=testpassword
BROWSER=chrome
```

### 3. Agregar escenarios
1. Crear archivos `.feature` en `src/test/resources/features/`
2. Implementar pasos en `src/test/java/steps/`
3. Crear páginas en `src/test/java/pages/`

## 📚 Recursos de aprendizaje

Este framework demuestra:
- **Arquitectura de pruebas**: automatización a escala
- **Patrones de diseño**: Page Object Model, inyección de dependencias
- **Buenas prácticas**: paralelismo, reportes completos
- **Calidad de código**: métricas y disciplina técnica

## 🤝 Contribución

Haz fork de este repositorio y adáptalo a tus necesidades:
1. Haz fork del repositorio
2. Crea una rama de funcionalidad
3. Implementa tus cambios
4. Ejecuta pruebas
5. Envía un pull request

## 📄 Licencia

Licencia MIT. Puedes usar este framework en tus proyectos.

## 👨‍💻 Autor

**Emerino Farfán**
- GitHub: [@emerinofarfan](https://github.com/emerinofarfan)
- Portafolio: [Tu URL de portafolio]
- LinkedIn: [Tu perfil de LinkedIn]

---

**Última actualización**: 2026-04-24 | **Versión**: 1.0.0 - Edición Portafolio
