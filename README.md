# QA Automation Framework

An enterprise-grade test automation framework built with Java 21, Gradle, Selenium, and Cucumber. Production-tested architecture suitable for financial services, healthcare, and regulated industries.

## 🎯 What This Framework Demonstrates

✅ **BDD Approach**: Feature files in Gherkin language for clear test scenarios
✅ **Selenium 4**: Modern web automation with latest WebDriver API
✅ **Parallel Execution**: 3-thread parallelism for faster feedback loops
✅ **Page Object Model**: Scalable, maintainable test structure
✅ **Allure Reporting**: Comprehensive test reports with screenshots and metrics
✅ **Code Quality**: JaCoCo coverage tracking, Spotless formatting, Checkstyle validation
✅ **Security**: CVE scanning with automated dependency management
✅ **Enterprise Patterns**: Real-world test architecture from production environments

## 🚀 Quick Start

### Prerequisites
- **Java**: 21 or higher
- **Git**: For cloning the repository
- **Browser**: Chrome, Firefox, or Edge (Selenium compatible)

### Setup (3 Steps)

```bash
# 1. Clone the repository
git clone https://github.com/emerinofarfan/qa-automation-framework.git
cd qa-automation-framework

# 2. Copy environment template
cp .env.example .env
# Edit .env with your target application URL

# 3. Run tests
./gradlew test --tags=@Smoke
```

### Generate Reports

```bash
# Run tests and generate Allure report
./gradlew test allureReport
open build/reports/allure/allureReport/index.html

# Code coverage report
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

## 📁 Project Structure

```
src/test/
├── java/
│   ├── config/              # Environment and test configuration
│   ├── hooks/               # Cucumber setup/teardown (Before/After)
│   ├── models/              # Data transfer objects and test models
│   ├── pages/               # Page Object Model classes
│   ├── steps/               # Cucumber step definitions
│   ├── utils/               # Utility functions and helpers
│   └── runner/              # Test runner configuration
└── resources/
    ├── features/            # BDD feature files (Gherkin)
    └── test/                # Test configuration files
```

## 🏗️ Architecture Highlights

### Page Object Model
Encapsulates page-specific selectors and actions, reducing test fragility:
```java
public class LoginPage {
    private WebDriver driver;
    private By usernameField = By.id("username");
    
    public void login(String username, String password) {
        // Page-specific actions
    }
}
```

### Feature Files (BDD)
Human-readable test scenarios in Gherkin:
```gherkin
Feature: User Authentication
  Scenario: Valid login
    Given user is on the login page
    When user enters valid credentials
    Then user is redirected to dashboard
```

### Parallel Execution
Default: **3 concurrent threads** for faster feedback
- Configurable in `build.gradle`: `parallel = true`, `maxWorkers = 3`
- Perfect for CI/CD pipelines

## 📊 Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 LTS | Language and runtime |
| Gradle | 8.x | Build automation |
| Selenium | 4.18.1 | Web automation |
| Cucumber | 7.18.1 | BDD framework |
| JUnit | 5 | Test execution |
| Allure | 2.27.0 | Test reporting |

## 🔒 Quality & Security

- ✅ CVE scanning with automated dependency updates
- ✅ Code formatting with Spotless
- ✅ Static analysis with Checkstyle
- ✅ Code coverage tracking with JaCoCo
- ✅ Comprehensive logging with Logback

## 🎓 How to Adapt This Framework

### 1. Change Package Name
Replace `com.example.qaautomation` with your organization's package name

### 2. Update Configuration
Edit `.env` with your application details:
```properties
BASE_URL=https://your-app.com
API_BASE_URL=https://api.your-app.com
TEST_USERNAME=testuser
TEST_PASSWORD=testpassword
BROWSER=chrome
```

### 3. Add Test Scenarios
1. Create `.feature` files in `src/test/resources/features/`
2. Implement step definitions in `src/test/java/steps/`
3. Create page objects in `src/test/java/pages/`

## 📚 Learning Resources

This framework demonstrates:
- **Test Architecture**: Building automation at scale
- **Design Patterns**: Page Object Model, dependency injection
- **Best Practices**: Parallel execution, comprehensive reporting
- **Code Quality**: Professional testing metrics

## 🤝 Contributing

Fork this repository and adapt it for your projects:
1. Fork the repo
2. Create feature branch
3. Implement your changes
4. Test thoroughly
5. Submit pull request

## 📄 License

MIT License - Feel free to use in your projects

## 👨‍💻 Author

**Emerino Farfán**
- GitHub: [@emerinofarfan](https://github.com/emerinofarfan)
- Portfolio: [Your Portfolio URL]
- LinkedIn: [Your LinkedIn Profile]

---

**Last Updated**: 2026-04-24 | **Version**: 1.0.0 - Portfolio Edition

Proyecto de portafolio para mostrar capacidades de automatizacion QA y liderazgo tecnico.