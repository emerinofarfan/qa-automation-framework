# MR QA Checklist

## 1. Alcance
- [ ] El MR incluye solo cambios del objetivo definido.
- [ ] No se mezclan refactors no solicitados.

## 2. BDD y cobertura
- [ ] Existe al menos un escenario @Smoke para el flujo nuevo.
- [ ] Los escenarios usan lenguaje de negocio.
- [ ] No hay steps duplicados semanticos.

## 3. Calidad tecnica
- [ ] No se usa Thread.sleep para sincronizacion.
- [ ] Se usan esperas explicitas.
- [ ] No hay credenciales hardcodeadas.

## 4. Evidencia
- [ ] Pipeline adjunto en verde.
- [ ] Reporte de ejecucion adjunto o referenciado.
- [ ] Riesgos y supuestos documentados.

## 5. Gobernanza
- [ ] Se respetan convenciones de nombres.
- [ ] Se cumplen quality gates del ambiente objetivo.
- [ ] El MR tiene aprobaciones requeridas.
