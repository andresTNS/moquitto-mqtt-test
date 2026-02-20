# SubscriptionManager — MQTT IoT Monitor

[![SubscriptionManager CI](https://github.com/andresTNS/moquitto-mqtt-test/actions/workflows/subscriptionmanager-tests.yml/badge.svg?branch=master)](https://github.com/andresTNS/moquitto-mqtt-test/actions/workflows/subscriptionmanager-tests.yml)

Repositorio de pruebas de concepto (PoC) con Mosquitto, MQTT y AWS IoT Core
para el monitoreo de dispositivos Teltonika FMC920.

## Proyectos

### SubscriptionManager
Aplicación Spring Boot para monitoreo de mensajes MQTT desde dispositivos Teltonika
FMC920 via AWS IoT Core, con persistencia en MySQL y notificaciones en tiempo real
via Server-Sent Events.

**Stack:** Spring Boot 4.0.2 · Java 25 · Maven 3.9.12 · MySQL 8 · AWS IoT Core (puerto 8883 TLS)

### SQL_FILES
Scripts y esquemas de la base de datos MySQL compartidos entre los proyectos del PoC.
