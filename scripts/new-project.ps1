param(
  [Parameter(Mandatory = $true)]
  [string]$ProjectName,

  [Parameter(Mandatory = $false)]
  [string]$BaseUrl = "https://tuaplicacion.example.pe",

  [Parameter(Mandatory = $false)]
  [string]$AppName = $ProjectName
)

# ─── Validacion ───────────────────────────────────────────────────────────────
if ($ProjectName -notmatch '^[a-z][a-z0-9\-]+$') {
    Write-Error "ProjectName debe ser lowercase con guiones. Ej: castigos-web, creditos-ui"
    exit 1
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$projectRoot = Join-Path $repoRoot "projects\$ProjectName"

if (Test-Path $projectRoot) {
    Write-Error "El directorio '$projectRoot' ya existe. Elige otro nombre."
    exit 1
}

Write-Host ""
Write-Host "Scaffolding proyecto: $ProjectName" -ForegroundColor Cyan
Write-Host "Base URL            : $BaseUrl"
Write-Host "Destino             : $projectRoot"
Write-Host ""

# ─── Estructura de directorios ────────────────────────────────────────────────
$dirs = @(
    "src\test\java\pages",
    "src\test\java\steps",
    "src\test\resources\features"
)
foreach ($dir in $dirs) {
    New-Item -ItemType Directory -Force (Join-Path $projectRoot $dir) | Out-Null
}

# ─── config.properties ───────────────────────────────────────────────────────
@"
# Configuracion del proyecto $ProjectName
# Editar base.url segun el ambiente objetivo (DEV / QA / STAGING).
# No hardcodear credenciales: usar TEST_USERNAME / TEST_PASSWORD como variables de entorno.
base.url=$BaseUrl
browser=chrome
# browser.headless: true para CI/CD (se activa automaticamente cuando CI=true).
browser.headless=false
timeout.default=15
timeout.fast=5
timeout.slow=30
"@ | Set-Content (Join-Path $projectRoot "src\test\resources\config.properties") -Encoding UTF8

# ─── Feature @Smoke de ejemplo ───────────────────────────────────────────────
@"
@Smoke
Feature: Validacion inicial de $AppName

  # Este escenario es el camino feliz minimo que debe pasar en cada MR.
  # Reemplazar los steps con acciones reales de tu aplicacion.
  # Regla: cada Scenario debe ser independiente (sin dependencia de orden ni de datos de otros escenarios).

  Scenario: La pagina de inicio carga correctamente
    Given el usuario abre la aplicacion
    When la pagina principal se renderiza
    Then el titulo de la pagina es visible
"@ | Set-Content (Join-Path $projectRoot "src\test\resources\features\${AppName}Smoke.feature") -Encoding UTF8

# ─── Page Object base del proyecto ───────────────────────────────────────────
$pageClass = ($AppName -replace '[-_]([a-z])', { $_.Groups[1].Value.ToUpper() })
$pageClass = $pageClass.Substring(0,1).ToUpper() + $pageClass.Substring(1) + "Page"

@"
package pages;

import org.openqa.selenium.By;

/**
 * Page Object para $AppName.
 *
 * Extiende BasePage para heredar driver thread-safe, explicit waits y helpers de interaccion.
 * Regla: solo acciones UI y lecturas de estado aqui. Sin aserciones de negocio.
 */
public class ${pageClass} extends BasePage {

    // ─── Selectores ───────────────────────────────────────────────────────
    // Preferencia: id > data-testid > name > css > xpath (ultimo recurso).
    private static final By TITULO_PRINCIPAL = By.cssSelector("h1");

    // ─── Acciones ─────────────────────────────────────────────────────────

    public boolean esTituloVisible() {
        return isElementPresent(TITULO_PRINCIPAL);
    }
}
"@ | Set-Content (Join-Path $projectRoot "src\test\java\pages\${pageClass}.java") -Encoding UTF8

# ─── Step Definitions ─────────────────────────────────────────────────────────
$stepsClass = ($AppName -replace '[-_]([a-z])', { $_.Groups[1].Value.ToUpper() })
$stepsClass = $stepsClass.Substring(0,1).ToUpper() + $stepsClass.Substring(1) + "Steps"

@"
package steps;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import pages.${pageClass};

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step Definitions para $AppName.
 *
 * Regla: orquestar acciones, no implementarlas.
 * Toda logica UI va en el Page Object. Solo aserciones de negocio aqui.
 * No usar Thread.sleep: las esperas estan en BasePage y en el Page Object.
 */
public class ${stepsClass} {

    private final ${pageClass} page = new ${pageClass}();

    @Dado("el usuario abre la aplicacion")
    public void elUsuarioAbreLaAplicacion() {
        // BasePage.navigateTo() se llama en Hooks.setUp(); el driver ya esta listo.
        // Si necesitas navegar a una URL especifica del flujo, llamar page.navigateTo(url).
    }

    @Cuando("la pagina principal se renderiza")
    public void laPaginaPrincipalSeRenderiza() {
        // Agregar aqui cualquier espera o accion necesaria para que la pagina cargue.
    }

    @Entonces("el titulo de la pagina es visible")
    public void elTituloDeLaPaginaEsVisible() {
        assertThat(page.esTituloVisible())
            .as("El titulo principal de la pagina debe estar visible")
            .isTrue();
    }
}
"@ | Set-Content (Join-Path $projectRoot "src\test\java\steps\${stepsClass}.java") -Encoding UTF8

# ─── .gitkeep para que git trackee el directorio features vacio ─────────────
New-Item -ItemType File -Force (Join-Path $projectRoot "src\test\resources\features\.gitkeep") | Out-Null

# ─── Resumen ─────────────────────────────────────────────────────────────────
Write-Host "Estructura creada:" -ForegroundColor Green
Write-Host "  src/test/resources/config.properties"
Write-Host "  src/test/resources/features/${AppName}Smoke.feature"
Write-Host "  src/test/java/pages/${pageClass}.java"
Write-Host "  src/test/java/steps/${stepsClass}.java"
Write-Host ""
Write-Host "Proximos pasos:" -ForegroundColor Yellow
Write-Host "  1. Definir TEST_USERNAME y TEST_PASSWORD en tu entorno (no en codigo)."
Write-Host "  2. Editar base.url en config.properties si necesitas un ambiente diferente."
Write-Host "  3. Ejecutar primer test secuencial:"
Write-Host "     .\gradlew test '-Dcucumber.filter.tags=@Smoke' '-Dcucumber.execution.parallel.enabled=false'"
Write-Host "  4. Ver QUICK_RUN.md para todos los comandos disponibles."
Write-Host ""
