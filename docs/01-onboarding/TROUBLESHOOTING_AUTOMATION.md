# Troubleshooting Automation

## 1. Fallo de compilacion Gradle
Causas comunes:
- Java no compatible.
- Dependencias no resueltas.

Accion:
- Validar version Java.
- Ejecutar `./gradlew --refresh-dependencies clean test`.

## 2. Fallo por timeout en UI
Causas comunes:
- Esperas inestables.
- Ambiente lento.

Accion:
- Revisar esperas explicitas.
- Verificar disponibilidad del ambiente antes de ejecutar.

## 3. Credenciales no cargadas
Causas comunes:
- Variables no definidas en shell o CI.

Accion:
- Definir `TEST_USERNAME` y `TEST_PASSWORD`.
- Verificar variables protegidas en GitLab.

## 4. Escenario flaky
Causas comunes:
- Selectores inestables.
- Dependencia de datos sucios.

Accion:
- Reemplazar selector fragil.
- Preparar datos controlados.
- Reportar causa raiz en MR.

## 5. Cuando escalar
Escalar al referente QA si:
- El bloqueo supera 2 horas.
- Hay falla recurrente sin causa clara.
- Hay riesgo de seguridad o datos sensibles.

## 6. Fallo solo en CI pero no en local

Causas comunes:
- La variable `BASE_URL` apunta a un ambiente diferente en CI.
- `TEST_USERNAME` / `TEST_PASSWORD` no estan definidas como variables protegidas en GitLab.
- El runner de CI no tiene conexion al ambiente de QA (firewall o VPN).

Accion:
1. Revisar Settings > CI/CD > Variables en el proyecto GitLab y confirmar que las variables existen y no estan restringidas a una rama incorrecta.
2. Ejecutar el job manualmente desde GitLab con "Run pipeline" pasando las variables para aislar el problema.
3. Revisar el log de CI: si el error es `Connection refused` o `ERR_NAME_NOT_RESOLVED`, el problema es de red, no de codigo.

## 7. Test pasa en secuencial pero falla en paralelo (flaky)

Causas comunes:
- El escenario modifica datos que otro escenario en paralelo esta leyendo (falta de aislamiento de datos).
- Selector que depende de un orden de carga que varia entre hilos.
- Variable de clase o estatica compartida entre escenarios.

Accion:
1. Reproducir en secuencial para confirmar que pasa: `.\gradlew test "-Dcucumber.filter.tags=@Tag" "-Dcucumber.execution.parallel.enabled=false"`
2. Si pasa en secuencial: el problema es de datos o estado compartido, no de logica de negocio.
3. Revisar que el escenario no dependa de datos creados por otro escenario.
4. Reportar como flakiness en el MR con causa raiz identificada antes de solicitar merge.
5. Etiquetar con @Destructive si el escenario modifica datos globales: se ejecutara siempre al final en modo secuencial.

## 8. ChromeDriver falla en CI con "cannot find Chrome binary"

Causas comunes:
- La imagen Docker del runner no incluye Chrome.
- La version de ChromeDriver no coincide con la version de Chrome instalada en el runner.

Accion:
1. Verificar que la imagen `gradle:8.12-jdk21` usada en el pipeline tiene Chrome instalado, o cambiar a una imagen que lo incluya (ej. `zenika/alpine-chrome` o imagen corporativa).
2. WebDriverManager descarga el driver automaticamente, pero necesita que Chrome este instalado en el PATH del runner.
3. Si el runner es Shell (no Docker): verificar `which google-chrome` o `google-chrome --version` en el job.

## 9. Pipeline cancelado por timeout en nightly

Causas comunes:
- `PARALLELISM` demasiado bajo para el volumen de escenarios y el timeout de GitLab.
- Escenarios con esperas excesivas (Thread.sleep) que inflan el tiempo total.

Accion:
1. Aumentar `PARALLELISM` via Settings > CI/CD > Variables (sin tocar codigo): empezar con 5, validar estabilidad, luego 8.
2. Revisar escenarios lentos en el reporte Allure: los mas lentos aparecen destacados.
3. No aumentar el timeout del job como primera accion: resolver la causa raiz.

## 10. Como relanzar solo los escenarios que fallaron (rerun)

El framework genera automaticamente `build/rerun.txt` con la lista de escenarios fallidos al final de cada run.

Comando para relanzar solo los fallidos en secuencial:
```powershell
.\gradlew test "-Dcucumber.features=@build/rerun.txt" "-Dcucumber.execution.parallel.enabled=false"
```

Usos:
- Si el nightly tiene 2 fallas de 80 escenarios: relanzar solo esos 2 para confirmar si son flaky.
- Si pasan en el rerun: son flaky. Aislar causa raiz antes del merge (ver item 7 de este documento).
- Si siguen fallando en el rerun: el fallo es real. Abrir incidencia con log completo.

Nota: `build/rerun.txt` se sobreescribe en cada run. Guardarlo antes de un nuevo run si necesitas referencia historica.

## 11. El preflight falla con "BASE_URL no esta definida"

Causa:
El pipeline requiere la variable `BASE_URL` definida en los secrets del proyecto GitLab.

Accion:
1. Ir al proyecto GitLab > Settings > CI/CD > Variables.
2. Crear la variable `BASE_URL` con el valor de la URL del ambiente de QA del equipo.
3. Marcar como Protected si el ambiente es produccion o staging.
4. No hardcodear la URL en `.gitlab-ci.yml` ni en archivos del repositorio.

Si el ambiente cambia (deploy nuevo, IP diferente): actualizar la variable en GitLab sin tocar codigo.
