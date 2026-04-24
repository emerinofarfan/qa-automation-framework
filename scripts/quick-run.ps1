param(
    [Parameter(Mandatory = $false, HelpMessage = "Suite a ejecutar: smoke, auth, regression, all, destructive")]
    [ValidateSet('smoke', 'auth', 'regression', 'all', 'destructive')]
    [string]$Suite = 'smoke',

    [Parameter(Mandatory = $false, HelpMessage = "Cantidad de hilos para ejecucion paralela")]
    [ValidateRange(1, 16)]
    [int]$Threads = 3,

    [Parameter(Mandatory = $false, HelpMessage = "Deshabilita ejecucion paralela")]
    [switch]$Sequential
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$gradlewPath = Join-Path $repoRoot 'gradlew'

if (-not (Test-Path $gradlewPath)) {
    throw "No se encontro gradlew en la raiz esperada: $repoRoot"
}

Write-Host "== Framework Base QA - QA Automation ==" -ForegroundColor Cyan
Write-Host "Suite: $Suite" -ForegroundColor Yellow

$gradleTask = 'test'
$args = @()

switch ($Suite) {
    'smoke' {
        $args += '"-Dcucumber.filter.tags=@Smoke"'
    }
    'auth' {
        $args += '"-Dcucumber.filter.tags=@UI and @Auth and @Regression"'
    }
    'regression' {
        $args += '"-Dcucumber.filter.tags=@Regression"'
    }
    'all' {
        # Sin filtro de tags: ejecuta toda la suite definida por el proyecto.
    }
    'destructive' {
        $gradleTask = 'testDestructive'
    }
}

if ($Sequential.IsPresent) {
    $args += '"-Dcucumber.execution.parallel.enabled=false"'
} elseif ($Suite -ne 'destructive') {
    $args += '"-Dcucumber.execution.parallel.enabled=true"'
    $args += ('"-Dcucumber.execution.parallel.config.fixed.parallelism={0}"' -f $Threads)
}

$command = ".\\gradlew {0} {1}" -f $gradleTask, ($args -join ' ')
Write-Host "Comando:" -ForegroundColor Green
Write-Host "  $command"
Write-Host ""

Push-Location $repoRoot
try {
    Invoke-Expression $command
} finally {
    Pop-Location
}

if ($LASTEXITCODE -ne 0) {
    throw "La ejecucion finalizo con codigo $LASTEXITCODE"
}

Write-Host "Ejecucion completada correctamente." -ForegroundColor Green
