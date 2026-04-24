# Contribuir al Framework de Automatización QA

Versión en inglés: [CONTRIBUTING.md](CONTRIBUTING.md)

Fuente de verdad: inglés. Esta copia en español se mantiene para onboarding, colaboración local y difusión bilingüe.

## Cómo usar este repositorio

### Opción 1: Implementación de referencia
Estudia la organización del código y los patrones para tus propios proyectos.

### Opción 2: Fork y personalización
1. Haz fork de este repositorio
2. Actualiza los nombres de paquete
3. Adapta las page objects a tu aplicación
4. Personaliza los feature files

### Opción 3: Issues y sugerencias
- ¿Encontraste un bug? Abre un issue
- ¿Tienes sugerencias? Abre una discusión
- ¿Quieres contribuir? Envía un pull request

## Flujo de trabajo de desarrollo

### Requisitos previos
- Java 21+
- Gradle 8.x
- Git

### Configuración local
```bash
git clone https://github.com/emerinofarfan/qa-automation-framework.git
cd qa-automation-framework
cp .env.example .env
```

### Ejecución de pruebas
```bash
./gradlew test
./gradlew allureReport
```

### Estilo de código
- Usa Spotless para formateo automático
- Sigue la guía de estilo de Java
- Checkstyle valida el build

## Estructura del repositorio

- `src/test/java/` - código de pruebas (pages, steps, hooks, utils)
- `src/test/resources/` - feature files y configuración de pruebas
- `build.gradle` - configuración de build y dependencias
- `docs/` - documentación y arquitectura

## ¿Preguntas?

Puedes escribir un issue o contactarme directamente.

---

**Disfruta usando este framework.**
