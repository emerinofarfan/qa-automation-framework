# Selenium and Java Practices

## 1. Selenium - reglas obligatorias
- Prohibido Thread.sleep para sincronizacion funcional.
- Usar esperas explicitas centralizadas.
- Encapsular selectores y acciones en Page Objects.
- No colocar aserciones de negocio en Page Objects.
- Capturar evidencia automatica en fallas criticas.

## 2. Selectores UI
Orden recomendado de estabilidad:
1. id unico.
2. data-testid o atributo dedicado.
3. name estable.
4. css selector controlado.
5. xpath como ultimo recurso.

## 3. Java - convenciones tecnicas
- Una clase publica por archivo.
- Metodos cortos y orientados a intencion.
- Nombres de metodos en lowerCamelCase.
- Nombres de clases en UpperCamelCase.
- Evitar logica compleja en Step Definitions.

## 4. Distribucion de responsabilidades
- Step Definitions: orquestan el flujo BDD.
- Page Objects: acciones UI y lecturas de estado.
- Utils/Core: soporte tecnico transversal.
- Hooks: setup, teardown, evidencia.

## 5. Manejo de datos
- Evitar credenciales hardcodeadas.
- Usar variables de entorno o archivo de configuracion.
- Separar datos de prueba de la logica de steps.

## 6. Confiabilidad
- Reintentos limitados y controlados.
- Reportar causa raiz de falla (locator, timeout, dato invalido, ambiente).
- Corregir flakiness antes de ampliar cobertura.

## 7. Ejecucion paralela - reglas y responsabilidades del QA

El framework ya gestiona el aislamiento por hilo. Como QA debes respetar estas reglas para que la paralelizacion funcione.

### Como funciona internamente
- Cada escenario corre en su propio hilo del pool de Cucumber.
- Cada hilo tiene su propia instancia de ChromeDriver (ThreadLocal).
- Los steps dentro de un mismo escenario siempre son secuenciales.
- No hay estado compartido entre escenarios en paralelo.

### Lo que nunca debes hacer en paralelo
- Variables estaticas mutables en pages o steps (rompen aislamiento de hilos).
- Archivos de datos compartidos que se escriben durante el test.
- Escenarios que dependan del orden de ejecucion de otros escenarios.
- Thread.sleep para sincronizar entre escenarios (prohibido en todos los casos).

### Lo que si puedes y debes hacer
- Disenar cada escenario como una unidad independiente y autocontenida.
- Usar datos propios por escenario (no compartir cuentas o registros entre escenarios).
- Etiquetar con @Destructive los escenarios que modifican datos globales: se ejecutan siempre al final en modo secuencial.

### Paralelism recomendado
- Local Windows: 2-3 hilos maximo para evitar flakiness por recursos.
- CI/CD runner dedicado: 3-5 hilos segun RAM/CPU disponible.
- Si un test falla solo en paralelo pero pasa en secuencial: revisar datos compartidos o race conditions en selectores.

### Como depurar fallas en paralelo
1. Reproducir el escenario en secuencial:
   .\gradlew test "-Dcucumber.filter.tags=@NombreTag" "-Dcucumber.execution.parallel.enabled=false"
2. Si pasa en secuencial y falla en paralelo: el problema es de datos o selectores, no del codigo funcional.
3. Reportar como flakiness con causa raiz en el MR antes de hacer merge.

## 8. Planificacion de capacidad al escalar (10 → 100+ escenarios)

Antes de aumentar `parallelism`, calcular RAM requerida por el runner.

### Formula

    RAM requerida = (N_hilos × RAM_Chrome) + JVM_overhead + buffer_20%

| Modo Chrome   | RAM por instancia | Cuando usarlo                              |
|---------------|-------------------|--------------------------------------------|
| No-headless   | ~400-600 MB       | Desarrollo local (ver browser)             |
| Headless=new  | ~200-300 MB       | CI/CD siempre (automatico cuando CI=true)  |

JVM + Gradle + Cucumber overhead: ~600-800 MB fijos.

### Tabla de referencia por ambiente

| parallelism | Modo       | RAM minima runner | Escenarios en vuelo | Uso recomendado         |
|-------------|------------|-------------------|---------------------|-------------------------|
| 2           | local      | 2 GB              | 2                   | Depuracion local        |
| 3           | local/CI   | 3 GB              | 3                   | Default base            |
| 5           | CI         | 3 GB headless     | 5                   | Smoke + Critical        |
| 8           | CI dedicado| 5 GB headless     | 8                   | Regression nightly      |
| 12          | CI XL      | 7 GB headless     | 12                  | Maximo recomendado      |

No usar `parallelism > 12` con Selenium. Mas hilos != mas velocidad pasado ese punto:
Chrome empieza a competir por CPU y los tiempos de respuesta del DOM aumentan.

### Proceso para escalar de 10 a 100 escenarios

1. **Verificar que cada escenario es independiente** — datos propios, sin orden implicito.
2. **Etiquetar correctamente** — @Smoke, @Critical, @Regression, @Destructive.
3. **Calcular RAM del runner** usando la formula anterior con el parallelism objetivo.
4. **Ejecutar primero en secuencial** para baseline:
   `.\gradlew test "-Dcucumber.filter.tags=@Critical" "-Dcucumber.execution.parallel.enabled=false"`
5. **Aumentar parallelism gradualmente**: 3 → 5 → 8. Validar flakiness en cada paso.
6. **Nunca aumentar parallelism en CI sin modificar el runner tag** del GitLab job.
7. **Escenarios @Destructive siempre secuenciales** — el task `testDestructive` lo garantiza.

### Configurar parallelism por ambiente sin cambiar el archivo base

El archivo `junit-platform.properties` define `parallelism=3` como default seguro.
Para CI o local con mas recursos, sobreescribir desde CLI sin tocar el archivo:

    # CI con runner de 8 GB (5 hilos)
    .\gradlew test "-Dcucumber.execution.parallel.config.fixed.parallelism=5"

    # Runner dedicado nightly (8 hilos)
    .\gradlew test "-Dcucumber.execution.parallel.config.fixed.parallelism=8"

No modificar `junit-platform.properties` para cambios temporales o de ambiente.
Ese archivo es el contrato base del framework.
