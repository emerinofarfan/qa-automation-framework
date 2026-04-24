# Security and Test Data Policy

## 1. Principios
- No credenciales hardcodeadas.
- No datos reales de clientes en pruebas automatizadas.
- Minimo privilegio para cuentas de prueba.
- Trazabilidad de ejecucion por entorno.

## 2. Reglas obligatorias
- Usar variables de entorno para secretos.
- Enmascarar secretos en GitLab CI/CD.
- Evitar imprimir tokens y passwords en logs.
- Rotar credenciales de prueba de forma periodica.

## 3. Datos de prueba
- Usar datasets sinteticos o anonimizados.
- Definir ownership del dataset por equipo.
- Limpiar datos creados por pruebas destructivas.

## 4. Evidencia segura
- Adjuntar evidencia funcional sin exponer datos sensibles.
- En capturas, ocultar datos personales o cuentas reales.

## 5. Incidentes
Ante fuga de secreto o dato sensible:
1. Revocar credencial de inmediato.
2. Notificar a lider tecnico y seguridad.
3. Registrar incidente y plan de remediacion.
