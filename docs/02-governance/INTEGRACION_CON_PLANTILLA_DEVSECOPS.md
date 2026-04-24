# Integracion con plantilla-devsecops

## 1. Objetivo
Definir como integrar este framework base QA dentro de la estructura corporativa de Joel en GitLab.

## 1.1 Version de referencia
- CI/CD: GitLab 18 on-premise.
- Flujo: feature/* -> master -> staging -> production.
- Ambientes: Desarrollo (master), UAT (staging), Produccion (production).

## 2. Estructura objetivo
La carpeta QA debe vivir dentro del repositorio plantilla:

- plantilla-devsecops/
- plantilla-devsecops/.gitlab/ci/
- plantilla-devsecops/ansible/
- plantilla-devsecops/backend/
- plantilla-devsecops/frontend/
- plantilla-devsecops/qa-automation/

Pipeline principal esperado en el repositorio integrado plantilla-devsecops
(estos archivos viven en la plantilla principal, no en este repo QA aislado):
- .gitlab/ci/infrastructure.yml
- .gitlab/ci/firewall.yml
- .gitlab/ci/glpi.yml
- .gitlab/ci/sast.yml
- .gitlab/ci/test.yml
- .gitlab/ci/build.yml
- .gitlab/ci/deploy.yml
- .gitlab/ci/qa-automation.yml

## 3. Responsabilidad por capa
- ansible: provision, firewall, deploy, inventarios.
- backend/frontend: aplicacion.
- qa-automation: validacion funcional automatizada post-deploy.

## 4. Integracion CI recomendada
En `.gitlab/ci/test.yml` agregar jobs de QA funcional o incluir la plantilla:

- `.gitlab/ci/qa-automation.yml`

Ejemplo de include en `.gitlab-ci.yml` principal:

```yaml
include:
  - local: '.gitlab/ci/infrastructure.yml'
  - local: '.gitlab/ci/firewall.yml'
  - local: '.gitlab/ci/glpi.yml'
  - local: '.gitlab/ci/sast.yml'
  - local: '.gitlab/ci/build.yml'
  - local: '.gitlab/ci/deploy.yml'
  - local: '.gitlab/ci/test.yml'
  - local: '.gitlab/ci/qa-automation.yml'
```

Si se prefiere centralizar en `test.yml`, usar este patron:

```yaml
qa-smoke:
  stage: test
  script:
    - cd qa-automation
    - ./gradlew test "-Dcucumber.filter.tags=@Smoke"

qa-regression:
  stage: test
  rules:
    - if: '$CI_PIPELINE_SOURCE == "schedule"'
  script:
    - cd qa-automation
    - ./gradlew test "-Dcucumber.filter.tags=@Regression"
```

## 5. Gate corporativo minimo
- MR a master: qa-smoke en verde.
- Promocion a staging: smoke + critical en verde.
- Promocion a production: regression en verde y aprobaciones CAB.

Aprobaciones por ambiente:
- master: 1 peer review.
- staging: 2 aprobaciones (lider tecnico).
- production: 3 aprobaciones (CAB).

## 6. Variables minimas para QA en GitLab
- TEST_USERNAME
- TEST_PASSWORD
- BASE_URL

Todas deben configurarse como variables protegidas cuando aplique.

## 6.1 Variables corporativas comunes de la plantilla DevSecOps
Infraestructura y seguridad:
- VCENTER_HOST, VCENTER_USER, VCENTER_PASSWORD, VCENTER_DATACENTER
- PANORAMA_HOST, PANORAMA_API_KEY
- GLPI_URL, GLPI_API_TOKEN, GLPI_APP_TOKEN

Despliegue y plataforma:
- JBOSS_ADMIN_USER, JBOSS_ADMIN_PASSWORD
- ANSIBLE_SSH_PRIVATE_KEY

Analisis estatico (opcional):
- CX_BASE_URI, CX_TENANT, CX_CLIENT_ID, CX_CLIENT_SECRET

## 6.2 Politica de secretos
- Variables sensibles como Protected y Masked cuando aplique.
- No exponer secretos en logs de pipeline.
- Rotacion periodica de credenciales de prueba y despliegue.

## 7. Prerrequisitos operativos de integracion
- Usuario deploy en servidores destino con clave SSH de pipeline.
- Configuracion sudo no interactiva para automatizacion Ansible.
- Imagenes base disponibles en Container Registry interno.
- Runner con acceso al registry interno y politicas de confianza TLS.

## 8. Resultado esperado
El pipeline DevSecOps valida despliegue tecnico y este framework valida comportamiento funcional de negocio.
