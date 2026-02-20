# Security Policy

## Supported Versions

This is a Proof of Concept (PoC) repository. Security updates are applied to the
latest version on the `master` branch only.

| Version | Supported |
|---------|-----------|
| Latest (master) | ✅ |
| Older branches | ❌ |

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub Issues.**

If you discover a security vulnerability, especially related to:
- AWS IoT Core credential handling
- TLS/SSL certificate management (BouncyCastle, PEM files)
- MQTT broker authentication
- SQL injection or data exposure risks

Please report it privately by:
1. Opening a [GitHub Security Advisory](https://github.com/andresTNS/moquitto-mqtt-test/security/advisories/new)
2. Or contacting the maintainer directly via GitHub profile

### What to Include in Your Report

- Type of vulnerability
- File paths and line numbers involved
- Step-by-step reproduction instructions
- Potential impact assessment
- Suggested fix (if any)

### Response Timeline

- **Acknowledgment:** within 48 hours
- **Initial assessment:** within 7 days
- **Fix or mitigation:** depends on severity

We ask that you give us reasonable time to address the issue before any public disclosure.

## Security Considerations for This Project

This project handles:
- **AWS IoT Core certificates** (CA, device certificate, private key) — stored locally,
  never committed to the repository (covered by `.gitignore`)
- **MySQL credentials** — stored in `application.properties`, never committed
- **MQTT connections** — secured with mutual TLS authentication over port 8883

If you find credentials accidentally committed to the repository history,
please report it immediately.

---

# Política de Seguridad (Español)

## Versiones Soportadas

Este es un repositorio de Prueba de Concepto (PoC). Las actualizaciones de seguridad
se aplican únicamente a la última versión en la rama `master`.

| Versión | Soportada |
|---------|-----------|
| Última (master) | ✅ |
| Ramas anteriores | ❌ |

## Reportar una Vulnerabilidad

**Por favor no reportes vulnerabilidades de seguridad a través de GitHub Issues públicos.**

Si descubres una vulnerabilidad de seguridad, especialmente relacionada con:
- Manejo de credenciales de AWS IoT Core
- Gestión de certificados TLS/SSL (BouncyCastle, archivos PEM)
- Autenticación del broker MQTT
- Riesgos de inyección SQL o exposición de datos

Por favor repórtala de forma privada mediante:
1. Abriendo un [GitHub Security Advisory](https://github.com/andresTNS/moquitto-mqtt-test/security/advisories/new)
2. O contactando al mantenedor directamente a través del perfil de GitHub

### Qué Incluir en tu Reporte

- Tipo de vulnerabilidad
- Rutas de archivos y números de línea involucrados
- Instrucciones de reproducción paso a paso
- Evaluación del impacto potencial
- Corrección sugerida (si la tienes)

### Tiempos de Respuesta

- **Acuse de recibo:** dentro de 48 horas
- **Evaluación inicial:** dentro de 7 días
- **Corrección o mitigación:** depende de la gravedad

Pedimos que nos des un tiempo razonable para resolver el problema antes de cualquier
divulgación pública.

## Consideraciones de Seguridad de Este Proyecto

Este proyecto maneja:
- **Certificados de AWS IoT Core** (CA, certificado del dispositivo, clave privada) —
  almacenados localmente, nunca commitados al repositorio (cubierto por `.gitignore`)
- **Credenciales de MySQL** — almacenadas en `application.properties`, nunca commitadas
- **Conexiones MQTT** — aseguradas con autenticación TLS mutua en el puerto 8883

Si encuentras credenciales accidentalmente commitadas en el historial del repositorio,
repórtalo inmediatamente.
