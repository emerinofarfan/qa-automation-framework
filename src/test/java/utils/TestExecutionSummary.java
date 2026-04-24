package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                    TEST EXECUTION SUMMARY FORMATTER                       ║
 * ║          Proporciona resumen visual profesional en consola                ║
 * ║          Ideal para entornos bancarios (auditoría y trazabilidad)        ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */
public class TestExecutionSummary {

    private static final Logger logger = LoggerFactory.getLogger(TestExecutionSummary.class);
    private static final AtomicInteger scenariosPassed = new AtomicInteger(0);
    private static final AtomicInteger scenariosFailed = new AtomicInteger(0);
    private static final AtomicInteger scenariosSkipped = new AtomicInteger(0);
    private static final long startTime = System.currentTimeMillis();

    /**
     * Registra un escenario PASSED
     */
    public static void scenarioPassed(String featureName, String scenarioName) {
        scenariosPassed.incrementAndGet();
        logger.info("PASSED: {} > {}", featureName, scenarioName);
        // Imprime a consola con texto ASCII (sin emojis)
        System.out.println("[PASS] " + featureName + " > " + scenarioName);
    }

    /**
     * Registra un escenario FAILED
     */
    public static void scenarioFailed(String featureName, String scenarioName) {
        scenariosFailed.incrementAndGet();
        logger.error("FAILED: {} > {}", featureName, scenarioName);
        // Imprime a consola con texto ASCII
        System.err.println("[FAIL] " + featureName + " > " + scenarioName);
    }

    /**
     * Registra un escenario SKIPPED
     */
    public static void scenarioSkipped(String featureName, String scenarioName) {
        scenariosSkipped.incrementAndGet();
        logger.warn("SKIPPED: {} > {}", featureName, scenarioName);
        // Imprime a consola con texto ASCII
        System.out.println("[SKIP] " + featureName + " > " + scenarioName);
    }

    /**
     * Imprime el resumen final profesional
     */
    public static void printFinalSummary() {
        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;
        int totalScenarios = scenariosPassed.get() + scenariosFailed.get() + scenariosSkipped.get();
        int passPercentage = totalScenarios > 0 ? (scenariosPassed.get() * 100) / totalScenarios : 0;

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        StringBuilder summary = new StringBuilder();
        summary.append("\n");
        summary.append("================================================================================\n");
        summary.append("                     TEST EXECUTION FINAL SUMMARY                              \n");
        summary.append("================================================================================\n");
        summary.append(String.format(" Timestamp:        %s\n", timestamp));
        summary.append("--------------------------------------------------------------------------------\n");
        summary.append(String.format(" Total Scenarios:  %d\n", totalScenarios));
        summary.append(String.format(" [PASS] Passed:    %d (%d%%)\n", scenariosPassed.get(), passPercentage));
        summary.append(String.format(" [FAIL] Failed:    %d\n", scenariosFailed.get()));
        summary.append(String.format(" [SKIP] Skipped:   %d\n", scenariosSkipped.get()));
        summary.append("--------------------------------------------------------------------------------\n");
        summary.append(String.format(" Duration:         %.2f seconds\n", totalDuration / 1000.0));
        summary.append("--------------------------------------------------------------------------------\n");

        // Status general
        String status;
        if (scenariosFailed.get() == 0 && totalScenarios > 0) {
            status = "[PASS] ALL TESTS PASSED";
        } else if (scenariosFailed.get() > 0) {
            status = "[FAIL] SOME TESTS FAILED";
        } else {
            status = "[WARN] NO TESTS EXECUTED";
        }
        summary.append(String.format(" Status:           %s\n", status));
        summary.append("================================================================================\n");
        summary.append("\n");
        summary.append("Reports generated:\n");
        summary.append("  - Allure:  gradlew allureServe (to view report)\n");
        summary.append("  - Extent:  build/reports/extent-reports/\n");
        summary.append("  - Logs:    build/logs/test-execution.log\n");
        summary.append("\n");

        // Imprimir en logger (va al archivo)
        logger.info(summary.toString());

        // Imprimir directamente a consola (sin logback)
        System.out.println(summary.toString());
    }

    /**
     * Resetea los contadores (útil para múltiples ejecuciones)
     */
    public static void reset() {
        scenariosPassed.set(0);
        scenariosFailed.set(0);
        scenariosSkipped.set(0);
    }

}

