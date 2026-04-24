package runner;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * ╔════════════════════════════════════════════════════════════════════════════╗
 * ║          RUNNER PRINCIPAL — Framework-Automatizacion QA Base         ║
 * ║          Proyecto  : Framework-Automatizacion                        ║
 * ║          Stack     : Selenium + Cucumber + JUnit Platform + Allure   ║
 * ╚════════════════════════════════════════════════════════════════════════════╝
 *
 * Este runner usa JUnit Platform Suite para ejecución paralela y Allure para reportes.
 *
 * - Cada escenario corre en su propio hilo (aislamiento por ThreadLocal).
 * - Integración lista para pipelines CI/CD y buenas prácticas QA.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")

// ── Parámetros estructurales (no sobreescribibles desde CLI) ──────────────
// glue y plugin son fijos; tags y paralelismo viven en junit-platform.properties.
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "steps,hooks")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,
        value = "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm,"
              + "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:,"
              + "runner.TestExecutionListener,"
              // rerun: genera una lista de los escenarios que fallaron.
              // Permite relanzar solo los fallidos con:
              //   .\gradlew test "-Dcucumber.features=@build/rerun.txt"
              // Util para flakiness aislada sin re-ejecutar toda la suite.
              + "rerun:build/rerun.txt")

public class Runner {
    // El ciclo de vida del driver se gestiona en hooks.Hooks (@Before / @After).
    // Tags, paralelismo y features se configuran en junit-platform.properties
    // y pueden sobreescribirse con -D desde la línea de comandos.
    //
    // RETRY de escenarios fallidos:
    //   Despues de un run con fallas, relanzar solo los fallidos:
    //   .\gradlew test "-Dcucumber.features=@build/rerun.txt" "-Dcucumber.execution.parallel.enabled=false"
    //   Si pasan en el rerun: son flaky. Reportar causa raiz antes del merge.
}
