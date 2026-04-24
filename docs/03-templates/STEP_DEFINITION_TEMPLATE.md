# Step Definition Template

```java
package steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.WebDriver;
import pages.LoginPage;

public class LoginSteps {

    private final WebDriver driver;
    private final LoginPage loginPage;

    // TestContext es un ejemplo; usar el mecanismo real del framework para obtener el driver.
    public LoginSteps(TestContext context) {
        this.driver = context.getDriver();
        this.loginPage = new LoginPage(driver);
    }

    @Given("que el usuario abre la aplicacion")
    public void queElUsuarioAbreLaAplicacion() {
        loginPage.abrirAplicacion(System.getenv("BASE_URL"));
    }

    @When("ingresa credenciales validas")
    public void ingresaCredencialesValidas() {
        loginPage.ingresarCredencialesValidas(
            System.getenv("TEST_USERNAME"),
            System.getenv("TEST_PASSWORD")
        );
    }

    @Then("visualiza el home principal")
    public void visualizaElHomePrincipal() {
        loginPage.validarHomeVisible();
    }
}
```

## Reglas rapidas
- Un metodo por step.
- Nombre del metodo con intencion funcional.
- Sin Thread.sleep.
- Sin logica de negocio compleja en la clase de steps.
- Obtener el driver desde contexto/hook centralizado, no crearlo en steps.
