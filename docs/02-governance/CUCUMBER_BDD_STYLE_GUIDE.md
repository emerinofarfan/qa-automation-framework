# Guia de Estilo Cucumber BDD — Estandar de Calidad Mundial

> Este documento define el estandar obligatorio para escribir escenarios Cucumber en QA Automation.
> No es una guia de referencia opcional. Es el codigo de conducta del equipo de automatizacion.
>
> **Nivel de aplicacion:** Todo archivo `.feature` que entre al repositorio debe cumplir este estandar.
> Si un MR contiene features que no cumplen, el code owner tiene autoridad para bloquearlo.

---

## Por que importa escribir buenos escenarios

Un escenario Cucumber mal escrito crea tres problemas que se acumulan con el tiempo:

1. **Nadie entiende que esta probando.** Cuando el test falla en produccion a las 2am, el oncall no puede saber si el problema es funcional o tecnico.
2. **El mantenimiento se vuelve imposible.** Un escenario tecnico es fragil: cualquier cambio en el HTML lo rompe.
3. **Pierde su valor como documentacion.** El area de negocio, auditoria (SBS) y el equipo de desarrollo no pueden leer tests tecnicos.

Un escenario bien escrito es una especificacion funcional ejecutable. Si el gerente de operaciones puede leerlo y entender que valida el sistema, esta bien escrito.

---

## Regla de oro

> **Un escenario describe QUE debe hacer el sistema desde la perspectiva del negocio, nunca COMO lo hace tecnicamente.**

---

## Parte 1: Estructura del Feature

### 1.1 Encabezado del Feature

Cada archivo `.feature` representa una **capacidad de negocio**, no una pantalla ni un modulo tecnico.

**Correcto — capacidad de negocio:**
```gherkin
Feature: Inicio de sesion de usuarios autorizados
  Como usuario del sistema de QA Automation
  Quiero autenticarme con mis credenciales
  Para acceder a las funcionalidades segun mi perfil
```

**Incorrecto — pantalla tecnica:**
```gherkin
Feature: Login page
Feature: Pantalla auth
Feature: Test del formulario de inicio de sesion
```

**Regla:** El nombre del Feature debe poder completar la frase: *"El sistema permite que..."*
- ✓ "El sistema permite que... los usuarios autorizados inicien sesion"
- ✗ "El sistema permite que... la pantalla de login funcione"

### 1.2 Descripcion del Feature (opcional pero recomendada)

El bloque `Como... Quiero... Para...` no es decorativo. Documenta quien usa la funcionalidad y cual es el valor que aporta. En QA Automation, esto conecta directamente con los requerimientos de negocio y los casos de uso aprobados.

Si el Feature tiene mas de 5 escenarios, es obligatorio incluir la descripcion.

### 1.3 Un Feature, una capacidad

| Mal — mezcla capacidades | Bien — una capacidad por feature |
|---|---|
| `Feature: Login y recuperacion de clave` | `Feature: Inicio de sesion de usuarios` |
| `Feature: CRUD de clientes` | `Feature: Registro de nuevo cliente` |
| `Feature: Operaciones de cuenta` | `Feature: Consulta de saldo de cuenta` |

Si un feature tiene mas de 10-12 escenarios, es probable que deba dividirse en dos features.

---

## Parte 2: Nombrado de Escenarios

### 2.1 La formula del nombre de escenario

Un buen nombre de escenario tiene esta estructura:

```
[Accion del usuario o del sistema] [resultado esperado observable]
```

No incluye el nombre de la pantalla, el metodo HTTP, ni el ID del elemento HTML.

**Formulas que funcionan:**
- `Login exitoso con credenciales validas redirige al panel principal`
- `Busqueda por DNI inexistente muestra mensaje informativo`
- `Transferencia entre cuentas propias registra el movimiento correctamente`
- `Acceso sin autenticacion redirige al formulario de login`

**Formulas que no funcionan:**
- `Test login OK` ← vago, no describe el resultado
- `Verificar que el campo usuario acepta texto` ← tecnico, no es negocio
- `Login_exitoso_v2_final` ← nombre de archivo, no escenario
- `Cuando el usuario hace click en el boton` ← es un paso, no un escenario

### 2.2 Regla del titulo predictivo

El nombre del escenario debe ser suficiente para saber si el test paso o fallo **sin leer los pasos**.

Cuando Allure o el pipeline muestre:
```
FAILED: Login exitoso con credenciales validas redirige al panel principal
```
Sabes exactamente que capacidad esta rota. Eso tiene valor en una auditoria SBS.

---

## Parte 3: Given / When / Then

### 3.1 Given — contexto inicial

`Given` establece el **estado del mundo** antes de que ocurra la accion. No es una accion del usuario.

**Correcto — establece un estado:**
```gherkin
Given el usuario se encuentra en la pantalla de inicio de sesion
Given existe un cliente con DNI "12345678" registrado en el sistema
Given el usuario ha iniciado sesion como operador de cajas
```

**Incorrecto — es una accion, no un estado:**
```gherkin
Given el usuario abre Chrome
Given el usuario navega a la URL https://sistema.example.pe
Given el usuario hace clic en el menu de clientes
```

**Regla:** Si el `Given` contiene verbos de accion (click, navegar, abrir, escribir), probablemente es un `When` disfrazado.

**Excepcion aceptada:** Cuando la navegacion inicial es el Background compartido, se acepta:
```gherkin
Background:
  Given el usuario navega al portal de la aplicacion
```
Aqui la navegacion es precondicion compartida, no parte de lo que se esta probando.

### 3.2 When — la accion principal

`When` describe **una sola accion principal** del usuario o del sistema. Es el nucleo del escenario.

**Un When, una accion:**
```gherkin
When el usuario ingresa su usuario y contrasena correctos y hace clic en Ingresar
```
Este paso tiene DOS acciones. Dividir:
```gherkin
When ingresa su usuario "operador01" y contrasena valida
And hace clic en el boton Ingresar
```

**Correcto:**
```gherkin
When el operador registra una transferencia de S/. 500.00 a la cuenta destino
When el cliente solicita el estado de cuenta del mes anterior
When el supervisor aprueba la operacion pendiente
```

**Incorrecto:**
```gherkin
When se escribe en el campo id="txtUsuario" el valor "admin"
When se hace click en el elemento con css ".btn-submit"
When se espera 3 segundos y luego se verifica la pagina
```

### 3.3 Then — el resultado verificable

`Then` describe el **resultado observable y verificable** desde la perspectiva del usuario o del negocio.

**Debe ser verificable de forma objetiva:**
```gherkin
Then el sistema muestra el panel principal con el nombre del usuario
Then se registra el movimiento en el historial de transacciones
Then el sistema muestra el mensaje "Credenciales incorrectas. Intente nuevamente."
Then el saldo de la cuenta origen disminuye en S/. 500.00
```

**No es verificable u observable por el negocio:**
```gherkin
Then el elemento div.success tiene display:block
Then el response HTTP es 200
Then el metodo loginService.authenticate() fue llamado
Then no hay errores en consola
```

**Regla:** Si el `Then` requiere conocer el HTML, el codigo fuente o los logs del servidor, esta mal escrito.

### 3.4 And — extension natural

`And` y `But` continuan el contexto del paso anterior (`Given And`, `When And`, `Then And`).

```gherkin
Given el usuario se encuentra en la pantalla de inicio de sesion
  And el sistema esta disponible y responde correctamente
When ingresa credenciales invalidas
  And hace clic en Ingresar tres veces consecutivas
Then el sistema bloquea temporalmente el acceso a la cuenta
  And muestra el mensaje de bloqueo por intentos fallidos
  But no revela si el usuario existe o no en el sistema
```

`But` es util para expresar lo que NO debe ocurrir. En banca, los controles negativos son tan importantes como los positivos.

---

## Parte 4: Anti-patrones prohibidos

### Anti-patron 1: El escenario tecnico

Es el anti-patron mas comun y el mas danino. Mezcla la descripcion de negocio con detalles de implementacion.

**Prohibido:**
```gherkin
Scenario: Test login
  Given abrir url "https://sistema.example.pe/auth/login"
  When escribir en xpath "//input[@id='usuario']" el valor "admin"
  And escribir en id "clave" el valor "pass123"
  And hacer click en css ".btn.btn-primary[type='submit']"
  Then verificar que el elemento con id "panel-home" es visible
  And la url contiene "/home"
```

**Correcto:**
```gherkin
@Smoke @REQ-AUTH-001
Scenario: Login exitoso con credenciales validas redirige al panel principal
  Given el usuario se encuentra en la pantalla de inicio de sesion
  When ingresa sus credenciales de operador validas
  Then el sistema lo redirige al panel principal con su nombre visible
```

**Como detectarlo:** Si al leer el escenario mencionas palabras como `xpath`, `css`, `id=`, `url`, `click`, `escribir en campo`, `elemento`, `div`, `button` — es un escenario tecnico.

### Anti-patron 2: El scenario dios

Un escenario que prueba multiples comportamientos a la vez. Cuando falla, no sabes cual de los comportamientos fallo.

**Prohibido:**
```gherkin
Scenario: Flujo completo del sistema
  Given el usuario abre la aplicacion
  When hace login
  And busca un cliente por DNI
  And edita sus datos
  And guarda los cambios
  And cierra sesion
  Then el sistema funciona correctamente
```

Este escenario tiene 6 comportamientos distintos mezclados y un `Then` que no verifica nada especifico.

**Correcto — un comportamiento por escenario:**
```gherkin
Scenario: Login exitoso con credenciales validas
  ...

Scenario: Busqueda de cliente por DNI retorna resultados
  ...

Scenario: Actualizacion de datos de cliente registra el cambio
  ...
```

**Regla:** Si el nombre del escenario incluye la palabra "y" conectando dos acciones, probablemente debe dividirse.

### Anti-patron 3: El Given que es un When

Ejecutar acciones en el `Given` que forman parte de lo que se esta probando.

**Prohibido:**
```gherkin
Scenario: Usuario puede ver el historial de transacciones
  Given el usuario abre la aplicacion
  And hace clic en login
  And escribe su usuario y contrasena
  And hace clic en Ingresar
  And navega al menu Cuentas
  And hace clic en Historial
  When selecciona el rango de fechas del ultimo mes
  Then el sistema muestra las transacciones del periodo
```

Los primeros 6 pasos son todas acciones de usuario, no contexto. El escenario esta probando todo el flujo de navegacion, no el historial.

**Correcto:**
```gherkin
Scenario: Historial del ultimo mes muestra las transacciones del periodo
  Given el usuario autenticado se encuentra en la seccion de historial de su cuenta
  When selecciona el rango de fechas del ultimo mes
  Then el sistema muestra las transacciones del periodo seleccionado
```

El estado previo (login, navegacion) es infraestructura del test, no lo que se prueba. Va en el Background o en un Hook.

### Anti-patron 4: El Then sin verificacion real

Pasos que no verifican nada observable o que verifican algo vago.

**Prohibido:**
```gherkin
Then el sistema funciona
Then todo esta correcto
Then el usuario ve la pantalla
Then se completa el proceso
Then no hay errores
```

**Correcto:**
```gherkin
Then el sistema muestra el mensaje "Operacion registrada exitosamente"
Then el comprobante de la transaccion es visible con el numero de operacion
Then el saldo de la cuenta refleja el nuevo monto disponible
Then el historial registra la operacion con la hora y monto correctos
```

**Regla:** Si el `Then` no puede automatizarse con un assertion especifico, es porque no describe un resultado verificable.

### Anti-patron 5: Datos hardcodeados sin contexto de negocio

Usar datos especificos sin que el escenario explique su significado de negocio.

**Prohibido:**
```gherkin
When ingresa "admin" en usuario y "P@ss1234" en clave
Then ve el elemento con texto "Bienvenido, admin"
```

**Correcto:**
```gherkin
When ingresa las credenciales de un operador de cajas autorizado
Then el sistema muestra el panel de operaciones con su nombre
```

O si el dato es necesario para el escenario:
```gherkin
When busca el cliente con DNI "12345678"
Then el sistema muestra los datos del cliente "Juan Perez Rios"
```

En este caso el DNI es el insumo de la busqueda y tiene sentido que sea especifico.

### Anti-patron 6: Pasos compuestos con multiples acciones

Un paso Gherkin = una accion o verificacion. Nunca dos.

**Prohibido:**
```gherkin
When el usuario escribe su DNI, selecciona el tipo de cuenta y hace clic en continuar
Then el sistema muestra el saldo y el historial del ultimo mes y el nombre del titular
```

**Correcto:**
```gherkin
When el usuario ingresa su DNI en el formulario de consulta
And selecciona el tipo de cuenta "Ahorro"
And confirma la consulta
Then el sistema muestra el saldo actual de la cuenta
And muestra el historial de transacciones del ultimo mes
And muestra el nombre completo del titular
```

---

## Parte 5: Background

### 5.1 Cuando usar Background

`Background` es un bloque de pasos que se ejecuta antes de CADA escenario del feature. Usarlo cuando:
- Todos (o la gran mayoria) de los escenarios del feature parten del mismo estado
- El estado inicial es infraestructura del test, no lo que se prueba

```gherkin
Feature: Gestion de clientes

  Background:
    Given el usuario autenticado accede al modulo de Clientes

  Scenario: Busqueda por DNI retorna los datos del cliente
    When busca el cliente con DNI "12345678"
    Then el sistema muestra el nombre y datos del cliente

  Scenario: Busqueda con DNI inexistente muestra mensaje informativo
    When busca el cliente con DNI "99999999"
    Then el sistema indica que no se encontraron resultados
```

### 5.2 Cuando NO usar Background

- Cuando solo la mitad de los escenarios necesitan ese estado inicial
- Cuando el estado inicial ES lo que se prueba en algun escenario
- Cuando el Background tiene mas de 3 pasos (senala que el escenario esta mal diseñado)

---

## Parte 6: Scenario Outline — datos multiples

`Scenario Outline` ejecuta el mismo flujo con diferentes conjuntos de datos. Ideal para validar reglas de negocio con multiples casos.

### 6.1 Cuando usar Scenario Outline

- Mismo flujo, distintos valores de entrada con distintos resultados esperados
- Validacion de limites y rangos (montos minimos/maximos, formatos de DNI, etc.)
- Equivalencia de clases de prueba

```gherkin
@Regression @REQ-AUTH-003
Scenario Outline: Login con credenciales invalidas muestra mensaje de error apropiado

  Given el usuario se encuentra en la pantalla de inicio de sesion
  When intenta ingresar con usuario "<usuario>" y contrasena "<contrasena>"
  Then el sistema muestra el mensaje "<mensaje_esperado>"

  Examples:
    | usuario       | contrasena  | mensaje_esperado                              |
    | operador01    | claveWrong  | Credenciales incorrectas. Intente nuevamente. |
    | inexistente   | cualquier   | Credenciales incorrectas. Intente nuevamente. |
    | operador01    |             | La contrasena es obligatoria.                 |
    |               | cualquier   | El usuario es obligatorio.                    |
```

### 6.2 Nombrando el Scenario Outline

El nombre debe describir el patron, no un caso especifico. Evitar nombres como:
- ✗ `Login con usuario malo`
- ✗ `Probar varios logins`
- ✓ `Login con credenciales invalidas muestra mensaje de error apropiado`
- ✓ `Transferencia con monto fuera del rango permitido es rechazada`

### 6.3 Columnas de la tabla Examples

- Nombres de columna en minusculas con guion bajo: `| monto_transferencia |`
- Evitar columnas tecnicas: `| expected_css_class |` esta mal
- Maximo 5-6 columnas. Si necesitas mas, el escenario esta haciendo demasiado
- Al menos 3 filas para justificar el uso de Outline. Si solo tienes 2 casos, usa dos Scenarios separados

---

## Parte 7: Tags — estrategia de ejecucion y trazabilidad

### 7.1 Tags obligatorios

Cada escenario debe tener al menos un tag de **estrategia** y al menos un tag de **trazabilidad**.

| Tag | Significado | Cuando usar |
|---|---|---|
| `@Smoke` | Ruta critica minima | Escenarios que validan que el sistema basico funciona. Deben pasar antes de cualquier MR. |
| `@Regression` | Cobertura completa | Flujos secundarios, variaciones, casos borde. Se ejecutan antes de subir a staging/produccion. |
| `@Critical` | Bloquea liberacion | Flujos de dinero, seguridad, cumplimiento normativo. Fallo = no se libera. |
| `@Negativo` | Caso de error esperado | Validaciones de error, mensajes de rechazo, limites. |
| `@Destructive` | Modifica datos globales | Crea/elimina registros que afectan otros tests. Se ejecuta al final, en secuencial. |
| `@UIValidation` | Solo presentacion visual | Verifica textos, estilos, responsive. Bajo impacto funcional. |
| `@Wip` | En desarrollo | No se ejecuta en pipeline. Solo durante desarrollo local. |

### 7.2 Tags de trazabilidad (obligatorios en produccion)

| Tag | Significado | Ejemplo |
|---|---|---|
| `@REQ-XXX` | Vincula al requerimiento funcional aprobado | `@REQ-AUTH-001` |
| `@HU-XXX` | Vincula a la historia de usuario en el backlog | `@HU-042` |
| `@BUG-XXX` | Cubre un defecto especifico encontrado | `@BUG-127` |

En QA Automation, los tags `@REQ-` son obligatorios para flujos que involucren:
- Operaciones financieras (transferencias, depositos, retiros)
- Autenticacion y control de acceso
- Reporteria regulatoria (SBS)
- Cierre de caja y conciliacion

### 7.3 Orden correcto de tags

```gherkin
@Critical @Smoke @REQ-TRF-001 @HU-087
Scenario: Transferencia entre cuentas propias registra el movimiento
```

Orden: estrategia primero (criticidad), luego trazabilidad.

### 7.4 Tags que no se permiten

- Tags inventados sin estar en este estandar: `@importante`, `@revisar`, `@arreglar`
- Tags de persona: `@juan`, `@pedro` (el codigo no tiene duenos individuales)
- Tags tecnicos: `@selenium`, `@chrome`, `@api`
- `@Wip` en ramas que van a master (el pipeline debe fallar si detecta @Wip activo)

---

## Parte 8: Lenguaje — reglas de redaccion

### 8.1 Idioma

Todos los features en **espanol**. El sistema es de QA Automation, el area de negocio habla espanol, la documentacion de auditoria es en espanol.

Excepcion: nombres propios del sistema que esten en ingles en la interfaz (ej: "Dashboard", "Home") pueden usarse tal cual si asi aparecen en pantalla.

### 8.2 Sujeto gramatical

Ser consistente con el sujeto dentro de un feature. Elegir uno y mantenerlo:

**Opcion A — el usuario como sujeto:**
```gherkin
Given el usuario se encuentra en el panel principal
When el usuario solicita el estado de cuenta
Then el usuario recibe el documento en pantalla
```

**Opcion B — voz pasiva / sistema como sujeto:**
```gherkin
Given el panel principal esta visible
When se solicita el estado de cuenta
Then el sistema muestra el documento de estado de cuenta
```

No mezclar estilos dentro del mismo feature.

El equipo de QA Automation usa la **Opcion A** como estandar por ser mas legible para el area de negocio.

### 8.3 Verbos en tiempo presente

```gherkin
# Correcto — tiempo presente
Given el usuario se encuentra en la pantalla de login
When ingresa sus credenciales
Then el sistema muestra el panel principal

# Incorrecto — tiempo pasado o futuro
Given el usuario estaba en la pantalla de login
When ingreso sus credenciales
Then el sistema mostrara el panel principal
```

### 8.4 Evitar ambiguedad

| Palabra ambigua | Alternativa especifica |
|---|---|
| "correcto" | "con formato valido" o "previamente registrado" |
| "funciona" | "muestra el mensaje de confirmacion" |
| "ok" | Nunca usar en features |
| "normal" | Describir que significa normal |
| "apropiado" | Describir que es apropiado |
| "valido" | "con el formato requerido por el sistema" |
| "exitoso" | "retorna al panel principal" o el resultado concreto |

---

## Parte 9: Feature file — organizacion

### 9.1 Estructura de carpetas

```
src/test/resources/features/
├── auth/
│   ├── Login.feature
│   └── RecuperacionClave.feature
├── clientes/
│   ├── BusquedaCliente.feature
│   ├── RegistroCliente.feature
│   └── ActualizacionCliente.feature
├── cuentas/
│   ├── ConsultaSaldo.feature
│   └── HistorialTransacciones.feature
├── operaciones/
│   ├── Transferencias.feature
│   └── Depositos.feature
└── Demo.feature          ← solo para demostracion del framework
```

Los features se agrupan por modulo de negocio, no por tecnologia ni por tipo de test.

### 9.2 Un archivo, una capacidad

Un feature file contiene escenarios de una sola capacidad de negocio.

**Mal — mezcla capacidades:**
```
Clientes.feature  ← tiene busqueda, registro, edicion y eliminacion mezclados
```

**Bien — una capacidad por archivo:**
```
BusquedaCliente.feature
RegistroCliente.feature
ActualizacionCliente.feature
```

### 9.3 Maximo de escenarios por feature

- **Recomendado:** 3-7 escenarios
- **Maximo aceptable:** 12 escenarios
- **Si supera 12:** dividir en dos features por subcapacidad

---

## Parte 10: Checklist de revision antes del MR

Antes de hacer MR con features nuevos o modificados, verificar cada punto:

### Feature
- [ ] El nombre del Feature describe una capacidad de negocio, no una pantalla tecnica
- [ ] Incluye descripcion `Como... Quiero... Para...` si tiene mas de 5 escenarios
- [ ] Tiene `Background` solo si todos los escenarios comparten el mismo estado inicial

### Escenarios
- [ ] El nombre del escenario es predictivo: al leerlo sabes que comportamiento valida
- [ ] Cada escenario valida un solo comportamiento de negocio
- [ ] El `Given` establece un estado, no ejecuta acciones de usuario
- [ ] El `When` tiene una sola accion principal
- [ ] El `Then` es verificable con un assertion especifico y objetivo
- [ ] No hay referencias tecnicas: sin xpath, css, id=, url, click, elemento, div

### Tags
- [ ] Cada escenario tiene al menos un tag de estrategia (@Smoke/@Regression/@Critical)
- [ ] Cada escenario tiene al menos un tag de trazabilidad (@REQ- o @HU-)
- [ ] No hay @Wip en escenarios que van a master
- [ ] Los escenarios que modifican datos tienen @Destructive

### Lenguaje
- [ ] Todo en espanol
- [ ] Sujeto consistente en todo el feature
- [ ] Sin palabras ambiguas: "correcto", "ok", "normal", "funciona", "exitoso" sin contexto
- [ ] Verbos en tiempo presente

### Organizacion
- [ ] El archivo esta en la carpeta correcta del modulo
- [ ] El nombre del archivo refleja la capacidad (no `Test1.feature`, no `Login_v2.feature`)

---

## Parte 11: Ejemplos completos — del mal al bien

### Ejemplo 1: Feature de login

**Version mala (no pasar este MR):**
```gherkin
Feature: Test login sistema

  Scenario: login ok
    Given abrir url https://sistema.example.pe/login
    When escribir en id=usuario el valor "admin"
    And escribir en id=password el valor "12345"
    And click en button.submit
    Then verificar que div#home es visible
    And url es https://sistema.example.pe/home

  Scenario: login malo
    Given abrir url sistema
    When escribir usuario incorrecto y clave incorrecta
    Then error
```

Problemas: nombre vago, pasos tecnicos (xpath/id/url), Then no verificable, no tiene tags, datos hardcodeados sin contexto.

**Version correcta:**
```gherkin
@Login
Feature: Inicio de sesion de usuarios autorizados
  Como usuario del sistema de QA Automation
  Quiero autenticarme con mis credenciales
  Para acceder a las operaciones segun mi perfil

  Background:
    Given el usuario se encuentra en la pantalla de inicio de sesion

  @Smoke @REQ-AUTH-001
  Scenario: Login exitoso con credenciales validas redirige al panel principal
    When ingresa sus credenciales de operador validas
    Then el sistema muestra el panel principal con el nombre del usuario

  @Regression @Negativo @REQ-AUTH-002
  Scenario: Login con contrasena incorrecta muestra mensaje de error sin revelar detalles
    When intenta ingresar con una contrasena incorrecta
    Then el sistema muestra el mensaje de credenciales incorrectas
    But no revela si el usuario existe o no en el sistema

  @Regression @Negativo @REQ-AUTH-003
  Scenario Outline: Login con campos vacios muestra validacion del campo requerido
    When intenta ingresar sin completar el campo "<campo>"
    Then el sistema muestra el mensaje de campo obligatorio para "<campo>"

    Examples:
      | campo      |
      | usuario    |
      | contrasena |
```

### Ejemplo 2: Feature de operacion financiera

**Version mala:**
```gherkin
Feature: Transferencias

  Scenario: Hacer transferencia
    Given usuario logueado
    When hace transferencia de 100 a cuenta 123456
    Then transferencia ok
```

**Version correcta:**
```gherkin
@Transferencias
Feature: Transferencia entre cuentas propias
  Como cliente autenticado de QA Automation
  Quiero transferir fondos entre mis propias cuentas
  Para gestionar mi dinero sin ir a una agencia

  Background:
    Given el cliente autenticado se encuentra en el modulo de transferencias

  @Critical @Smoke @REQ-TRF-001 @HU-042
  Scenario: Transferencia exitosa entre cuentas propias registra el movimiento
    Given la cuenta origen tiene saldo suficiente para la operacion
    When el cliente transfiere S/. 200.00 de su cuenta de ahorro a su cuenta corriente
    Then el sistema confirma la operacion con un numero de comprobante
    And el saldo de la cuenta origen refleja el nuevo monto disponible
    And el movimiento aparece en el historial de ambas cuentas

  @Critical @Negativo @REQ-TRF-002
  Scenario: Transferencia con saldo insuficiente es rechazada con mensaje claro
    Given la cuenta origen tiene un saldo de S/. 50.00
    When el cliente intenta transferir S/. 200.00
    Then el sistema rechaza la operacion por saldo insuficiente
    And el saldo de la cuenta no se modifica
    And no se genera ningun comprobante de operacion

  @Regression @Negativo @REQ-TRF-003
  Scenario Outline: Transferencia con monto fuera de limites es rechazada
    When el cliente intenta transferir S/. <monto>
    Then el sistema muestra el mensaje "<mensaje>"

    Examples:
      | monto      | mensaje                                           |
      | 0.00       | El monto minimo de transferencia es S/. 1.00      |
      | -100.00    | El monto debe ser un valor positivo               |
      | 50001.00   | El monto supera el limite diario de transferencia |
```

---

## Parte 12: Referencia rapida — en la pantalla al escribir

```
DADO (Given)   → estado inicial, precondicion, contexto
               → sin verbos de accion del usuario
               → "el usuario SE ENCUENTRA EN..."
               → "el sistema TIENE registrado..."

CUANDO (When)  → UNA sola accion principal
               → verbo de accion: ingresa, selecciona, confirma, solicita
               → sin detalles tecnicos

ENTONCES (Then) → resultado OBSERVABLE por el negocio
               → especifico y verificable con assertion
               → "el sistema MUESTRA...", "el historial REGISTRA..."
               → sin html, sin url, sin codigo

Y (And)        → extiende el paso anterior
               → no cambia de Given a When sin el keyword correcto

PERO (But)     → lo que NO debe ocurrir
               → util para controles negativos en banca
```

---

## Referencia de documentos relacionados

- [SELENIUM_JAVA_PRACTICES.md](SELENIUM_JAVA_PRACTICES.md) — como implementar los steps en Java
- [JAVA_DIRECTORY_STRUCTURE_STANDARD.md](JAVA_DIRECTORY_STRUCTURE_STANDARD.md) — donde va cada archivo
- [TRAZABILIDAD_REQUISITOS.md](TRAZABILIDAD_REQUISITOS.md) — convencion completa de tags @REQ-/@HU-/@BUG-
- [MR_QA_CHECKLIST.md](MR_QA_CHECKLIST.md) — checklist completo de revision de MR
- [docs/03-templates/FEATURE_TEMPLATE.feature](../03-templates/FEATURE_TEMPLATE.feature) — plantilla lista para usar

---

*Guia de Estilo Cucumber BDD v2.0 — QA Automation QA Automatizacion — 2026-03-15*
*Aplicacion obligatoria a todos los archivos .feature en el repositorio.*
