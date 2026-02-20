# Contributing to this Project

Thank you for your interest in contributing! This is a Proof of Concept (PoC) focused on
MQTT, AWS IoT Core, and Teltonika FMC920 devices.

## How to Contribute

### Reporting Bugs
Please use the [Bug Report](.github/ISSUE_TEMPLATE/bug_report.yml) issue template.
Include as much detail as possible: environment, steps to reproduce, expected vs actual behavior.

### Proposing New Features
Please use the [Feature Request](.github/ISSUE_TEMPLATE/feature_request.yml) issue template.
Explain the motivation and the expected benefit for the PoC.

### Submitting Code Changes

1. **Fork** the repository and create a branch from `master`
2. **Branch naming:** use `feature/description`, `fix/description`, or `chore/description`
3. **Write tests** for any new functionality
4. **Run unit tests** before submitting: `cd SubscriptionManager && ./mvnw test -Dgroups='!integration'`
5. **Commit format:** follow [Conventional Commits](https://www.conventionalcommits.org/)
6. **Open a Pull Request** using the provided template

### Commit Message Format

```
type(scope): short description

Body with more detail if needed.
```

Valid types: `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `perf`, `style`, `build`, `ci`

### Code Style
- Java 25, Spring Boot 4.0.2
- Follow existing patterns in the codebase
- Document public methods in Spanish (project convention)
- Unit tests must not require external services (AWS IoT, MySQL)

### Integration Tests
Tests marked `@Tag("integration")` require AWS IoT Core credentials and a real MySQL instance.
These are intentionally excluded from CI. Do not remove the `@Tag("integration")` annotation.

---

# Cómo Contribuir (Español)

Gracias por tu interés en contribuir. Este es un PoC centrado en MQTT, AWS IoT Core
y dispositivos Teltonika FMC920.

## Cómo Contribuir

### Reportar Errores
Usa la plantilla [Bug Report](.github/ISSUE_TEMPLATE/bug_report.yml).
Incluye el entorno, pasos para reproducir el error, y el comportamiento esperado vs el real.

### Proponer Nuevas Funcionalidades
Usa la plantilla [Feature Request](.github/ISSUE_TEMPLATE/feature_request.yml).
Explica la motivación y el beneficio para el PoC.

### Enviar Cambios de Código

1. **Fork** del repositorio y crear una rama desde `master`
2. **Nombre de la rama:** usar `feature/descripcion`, `fix/descripcion` o `chore/descripcion`
3. **Escribe tests** para cualquier funcionalidad nueva
4. **Ejecuta los tests unitarios** antes de enviar: `cd SubscriptionManager && ./mvnw test -Dgroups='!integration'`
5. **Formato de commits:** seguir [Conventional Commits](https://www.conventionalcommits.org/)
6. **Abre un Pull Request** usando la plantilla proporcionada

### Formato de Mensaje de Commit

```
tipo(módulo): descripción corta

Cuerpo con más detalle si es necesario.
```

Tipos válidos: `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `perf`, `style`, `build`, `ci`

### Estilo de Código
- Java 25, Spring Boot 4.0.2
- Seguir los patrones existentes en el código
- Documentar métodos públicos en español (convención del proyecto)
- Los tests unitarios no deben requerir servicios externos (AWS IoT, MySQL)

### Tests de Integración
Los tests marcados con `@Tag("integration")` requieren credenciales de AWS IoT Core
y una instancia real de MySQL. Están excluidos del CI intencionalmente.
No elimines la anotación `@Tag("integration")`.
