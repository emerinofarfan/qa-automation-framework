# Naming Conventions

## 0. Fuente normativa

Para reglas de codificacion Java (diseno de clases, estilo de metodos, complejidad,
manejo de excepciones, etc.), este framework adopta como fuente principal el
**STD codificacion JAVA** corporativo de QA Automation.

Este documento no reemplaza ese estandar. Lo complementa con convenciones
especificas de automatizacion QA (features, steps, pages, runner y tags).

## 1. Feature files
Formato recomendado:
- <capacidad_negocio>.feature

Ejemplos:
- login.feature
- gestion_propuestas_castigo.feature

## 2. Step classes
Formato:
- <Capacidad>Steps.java

Ejemplos:
- LoginSteps.java
- PropuestasCastigoSteps.java

## 3. Page classes
Formato:
- <Pantalla>Page.java

Ejemplos:
- LoginPage.java
- HomePage.java

## 4. Runner classes
Formato:
- <Contexto>Runner.java

Ejemplo:
- Runner.java (base)
- RegressionRunner.java (si se separan suites)

## 5. Metodos Java — regla de idioma por capa

El idioma del nombre del metodo depende de la capa donde vive, no de preferencia personal.

| Capa | Idioma | Por que |
|------|--------|---------|
| `features/` (Gherkin) | Español | El negocio lo lee y lo valida |
| `steps/` (Step Definitions) | Español | Mapea directamente con el texto del feature |
| `pages/` (Page Objects) | Español | Describe acciones del usuario en el sistema |
| `pages/BasePage.java` | Inglés | Infraestructura técnica, no es del dominio |
| `utils/`, `core/` | Inglés | Framework reutilizable, no es del dominio |

### Por que BasePage esta en ingles

`clickElement()`, `write()`, `findVisible()` no saben nada del negocio de QA Automation.
Son metodos tecnicos que existen igual en cualquier framework de automatizacion del mundo.
Estan en ingles porque son infraestructura, como lo son las clases de Selenium o JUnit.

### Por que Pages y Steps estan en espanol

`presionarBotonIngresar()`, `ingresarUsuario()`, `validarPanelPrincipal()` describen
acciones del usuario en el sistema de QA Automation. Si un analista funcional lee el nombre,
lo entiende. Deben estar en el mismo idioma que el feature que los invoca.

### La frontera entre capas

```java
// Page Object — espanol: habla del negocio
public void presionarBotonIngresar() {
    clickElement(BOTON_INGRESAR);   // BasePage — ingles: infraestructura tecnica
}
```

Llamar desde espanol hacia ingles es correcto: estas cruzando la frontera entre
la capa de dominio (tu codigo) y la capa de infraestructura (el framework).

Lo que NUNCA es correcto es mezclar dentro del mismo nombre:

```java
// MAL — mezcla en el mismo nombre
public void clickBotonIngresar() { }   // "click" ingles + "Boton/Ingresar" espanol

// BIEN — espanol puro en la capa de dominio
public void presionarBotonIngresar() { }

// BIEN — ingles puro en BasePage
public void clickElement(By locator) { }
```

### Reglas de nombrado

- lowerCamelCase orientado a accion.
- Nombres autoexplicativos, sin abreviaturas ambiguas.
- Nunca mezclar espanol e ingles dentro del mismo nombre de metodo.
- El idioma lo determina la capa, no la preferencia del QA.

Ejemplos correctos en Pages/Steps:
- `ingresarCredencialesValidas()`
- `abrirModuloPropuestas()`
- `validarMensajeError()`
- `presionarBotonIngresar()`

## 6. Variables y constantes
- Variables locales: lowerCamelCase.
- Constantes: UPPER_SNAKE_CASE.

## 7. Reglas para mantener consistencia
- No usar abreviaturas ambiguas.
- No mezclar espanol e ingles en el mismo nombre.
- Mantener prefijos/sufijos consistentes por tipo de clase.
- Si hay conflicto entre este documento y el STD codificacion JAVA,
  prevalece el STD codificacion JAVA.
