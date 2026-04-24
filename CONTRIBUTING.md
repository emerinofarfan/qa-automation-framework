# Contributing to QA Automation Framework

Spanish version: [CONTRIBUTING.es.md](CONTRIBUTING.es.md)

Source of truth: English. Keep the Spanish copy aligned when updating onboarding or contribution guidance.

## Using This Repository

### Option 1: Reference Implementation
Study the code organization and patterns for your own projects.

### Option 2: Fork & Customize
1. Fork this repository
2. Update package names
3. Adapt page objects for your application
4. Customize feature files

### Option 3: Issues & Suggestions
- Found a bug? Open an issue
- Have suggestions? Open a discussion
- Want to contribute? Submit a pull request

## Development Workflow

### Prerequisites
- Java 21+
- Gradle 8.x
- Git

### Local Setup
```bash
git clone https://github.com/emerinofarfan/qa-automation-framework.git
cd qa-automation-framework
cp .env.example .env
```

### Running Tests
```bash
./gradlew test
./gradlew allureReport
```

### Code Style
- Uses Spotless for automatic formatting
- Follows Google Java style guide
- Checkstyle validation on build

## Repository Structure

- `src/test/java/` - Test code (pages, steps, hooks, utilities)
- `src/test/resources/` - Feature files and test configuration
- `build.gradle` - Build configuration and dependencies
- `docs/` - Documentation and architecture

## Questions?

Feel free to reach out or open an issue!

---

**Enjoy using this framework!**
