package runner;

import io.cucumber.plugin.EventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.TestRunFinished;
import utils.TestExecutionSummary;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║              CUCUMBER EVENT LISTENER — Final Test Summary                ║
 * ║    Se ejecuta automáticamente al terminar TODOS los escenarios            ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Este listener recibe eventos de Cucumber durante la ejecución.
 * Cuando se dispara el evento TestRunFinished (fin de todos los tests),
 * imprime el resumen final profesional.
 */
public class TestExecutionListener implements EventListener {

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestRunFinished.class, this::onTestRunFinished);
    }

    /**
     * Se ejecuta cuando Cucumber termina la ejecución de TODOS los escenarios
     */
    private void onTestRunFinished(TestRunFinished event) {
        // Imprimir resumen final
        TestExecutionSummary.printFinalSummary();
    }
}

