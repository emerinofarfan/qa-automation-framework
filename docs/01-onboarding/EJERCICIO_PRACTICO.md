# Ejercicio Práctico — Tu primer test automatizado real

> Este ejercicio es para hacerse después de la Sesión 1 con el capacitador.
> Objetivo: escribir un test completo (Feature + Steps + Page Object) para
> una funcionalidad real del sistema, sin copiar el demo.

---

## Instrucciones generales

1. **No copies el demo.** El ejercicio es para practicar, no para replicar.
2. **Trabaja en una rama nueva:** `git checkout -b feature/ejercicio-tu-nombre`
3. **Pide ayuda si llevas más de 15 minutos atascado** en el mismo punto.
4. Al terminar, corre el test. Si pasa: perfecto. Si falla: intenta diagnosticar
   con `GUIA_DEPURACION.md` antes de pedir ayuda.

---

## El ejercicio: Automatizar el flujo de búsqueda de un cliente

### Contexto

El sistema tiene una pantalla de búsqueda de clientes donde un usuario
ingresa un DNI o nombre y el sistema muestra los resultados.

Tu tarea es automatizar dos escenarios:
1. Búsqueda exitosa con un DNI válido
2. Búsqueda que no retorna resultados

---

## Paso 1: Escribe el Feature (15 min)

Crea el archivo:
```
src/test/resources/features/BusquedaCliente.feature
```

El feature debe tener:
- [ ] Tag de Feature apropiado
- [ ] `Background` con la navegación al portal
- [ ] Escenario @Smoke para búsqueda exitosa con DNI válido
- [ ] Escenario @Negativo para búsqueda sin resultados
- [ ] Tags de trazabilidad (@REQ- o @HU- según lo que el capacitador indique)
- [ ] Lenguaje natural en español, verbos en pasado o infinitivo

**Pista:** Revisa `Demo.feature` y `FEATURE_TEMPLATE.feature` para recordar
la estructura. El `Background` ya está en el demo.

**Señal de que está bien:**
- Alguien del área de negocio puede leer tu feature y entenderlo sin saber de código.
- Los escenarios describen QUÉ debe pasar, no CÓMO hacerlo.

---

## Paso 2: Encuentra los locators (20 min)

Antes de escribir el Page Object, necesitas saber los IDs/CSS de los elementos.

Abre el sistema en Chrome y ve a la pantalla de búsqueda.

Para cada elemento, usa F12 (DevTools → Inspector) y busca:

| Elemento | ¿Qué buscar en el HTML? | Ejemplo esperado |
|---|---|---|
| Campo de búsqueda (DNI o nombre) | `id=` o `name=` del `<input>` | `By.id("txtBusqueda")` |
| Botón de buscar | `type="submit"` o `id=` del `<button>` | `By.cssSelector("button[type='submit']")` |
| Tabla/lista de resultados | `id=` o `class=` del contenedor | `By.id("tablaResultados")` |
| Mensaje "sin resultados" | `id=` o `class=` del elemento de error | `By.cssSelector(".no-results")` |

Anota los locators. Los necesitas en el siguiente paso.

**Pista para elegir el mejor locator:**
```
id     → primera opción (más estable)
name   → segunda opción
data-* → tercera opción (ej. data-testid)
css    → cuarta opción
xpath  → último recurso (menos estable)
```

---

## Paso 3: Crea el Page Object (25 min)

Crea el archivo:
```
src/test/java/pages/BusquedaClientePage.java
```

Debe extender `BasePage` e incluir:
- [ ] Constantes privadas para los locators encontrados en el Paso 2
- [ ] Constructor que llama a `super()`
- [ ] Método `buscarPorDni(String dni)` que escribe en el campo y hace click
- [ ] Método `resultadosVisibles()` que retorna `boolean`
- [ ] Método `mensajeSinResultadosVisible()` que retorna `boolean`

**Pista:** Copia la estructura de `LoginPage.java` y adapta los locators
y métodos a la pantalla de búsqueda. El patrón es el mismo.

**Señal de que está bien:**
- No hay `By.` en los métodos públicos, solo en las constantes privadas.
- Los métodos públicos tienen nombres que el negocio entendería.
- El constructor solo tiene `super()`.

---

## Paso 4: Crea los Steps (20 min)

Crea el archivo:
```
src/test/java/steps/BusquedaClienteSteps.java
```

Debe incluir:
- [ ] Campo de instancia: `private final BusquedaClientePage busquedaPage = new BusquedaClientePage()`
- [ ] `@Dado` para la navegación (puedes reusar el mismo texto del demo
  si usa el mismo `Background`)
- [ ] `@Cuando` para "busca el cliente con DNI {string}"
- [ ] `@Entonces` para verificar resultados visibles
- [ ] `@Entonces` para verificar mensaje de sin resultados
- [ ] Assertions con AssertJ: `assertThat(...).as("mensaje descriptivo").isTrue()`

**Pista:** Copia la estructura de `DemoLoginSteps.java` y adapta los métodos.
El patrón es idéntico.

**Señal de que está bien:**
- No hay `By.`, `driver.`, ni `WebElement` en los steps.
- Las anotaciones `@Cuando` y `@Entonces` coinciden exactamente con el texto del feature.

---

## Paso 5: Corre el test (10 min)

```bash
./gradlew test -Dcucumber.filter.tags="@Smoke" -Dcucumber.execution.parallel.enabled=false
```

**Si pasa:**
```
BUILD SUCCESSFUL
✓ 1 scenario passed
```
¡Listo! Continúa al Paso 6.

**Si falla:** Abre `docs/01-onboarding/GUIA_DEPURACION.md` y sigue el árbol
de decisión. Intenta diagnosticar solo. Si en 15 minutos no avanzas, pide ayuda.

---

## Paso 6: Revisión con el capacitador (15 min)

Muestra tu solución al capacitador. Él evaluará:

| Criterio | ¿Qué evalúa? | Peso |
|---|---|---|
| El test pasa | Funcionalidad básica | Obligatorio |
| Feature en español legible | Calidad BDD | 20% |
| Locators usando id/css (no xpath puro) | Buenas prácticas | 20% |
| Page Object sin lógica de negocio | Separación de capas | 20% |
| Steps sin referencias a Selenium | Separación de capas | 20% |
| Tags correctos (@Smoke, @Negativo, @REQ-) | Trazabilidad y gobernanza | 20% |

---

## Criterios de evaluación detallados

### Feature (¿cómo sabe el capacitador que está bien?)

**Bien escrito:**
```gherkin
@Smoke @REQ-015
Scenario: Búsqueda exitosa por DNI retorna los datos del cliente
  Given el usuario navega al portal de la aplicación
  When busca el cliente con DNI "12345678"
  Then debe mostrarse al menos un resultado en la tabla
```

**Mal escrito (muy técnico, no BDD):**
```gherkin
Scenario: Test búsqueda
  Given abrir url sistema
  When escribir "12345678" en campo id=txtDni y clickear button#btnBuscar
  Then verificar que div.results no está vacío
```

---

### Page Object (¿cómo sabe el capacitador que está bien?)

**Bien:**
```java
public boolean resultadosVisibles() {
    return isElementVisible(TABLA_RESULTADOS);
}
```

**Mal (lógica de negocio en el PO):**
```java
public boolean busquedaFueExitosa() {
    // Verificando que hay resultados Y que el mensaje de éxito dice "encontrado"
    return isElementVisible(TABLA_RESULTADOS)
        && find(MENSAJE).getText().contains("encontrado");
}
```

---

### Steps (¿cómo sabe el capacitador que está bien?)

**Bien:**
```java
@Entonces("debe mostrarse al menos un resultado en la tabla")
public void debeMostrarseResultado() {
    assertThat(busquedaPage.resultadosVisibles())
        .as("Debe haber resultados después de una búsqueda válida")
        .isTrue();
}
```

**Mal (Selenium en el step):**
```java
@Entonces("debe mostrarse al menos un resultado en la tabla")
public void debeMostrarseResultado() {
    WebElement tabla = driver.findElement(By.id("tablaResultados"));
    assertThat(tabla.isDisplayed()).isTrue();
}
```

---

## Extensión opcional (para quien termina antes)

Si terminaste antes del tiempo, agrega:

1. Un tercer escenario `@Negativo @REQ-016` para búsqueda con DNI con formato inválido
   (letras en lugar de números). Debería mostrar un mensaje de validación.

2. Agrega el tag `@BUG-001` a cualquier escenario si descubres un comportamiento
   inesperado durante la automatización (¡los bugs se encuentran al automatizar!).

---

*Ejercicio práctico v1.0 — QA Automation QA Automatización — 2026-03-15*
