e-- ==========================================================
-- 01_schema.sql
-- Creación de base de datos y tablas
-- Proyecto: SubscriptionManager (MQTT Monitor)
-- Motor:    MySQL 8.x
-- Charset:  utf8mb4 / utf8mb4_unicode_ci
--
-- ORDEN DE EJECUCIÓN:
--   1. 01_schema.sql   ← este archivo
--   2. 02_indexes.sql
--   3. triggers/subscriptions/trg_subscriptions_before_insert.sql
--   4. triggers/subscriptions/trg_subscriptions_before_update.sql
--   5. triggers/mqtt_messages/trg_mqtt_messages_before_insert.sql
--   6. triggers/mqtt_messages/trg_mqtt_messages_after_insert.sql
--   7. triggers/detected_topics/trg_detected_topics_before_insert.sql
--   8. triggers/detected_topics/trg_detected_topics_before_update.sql
-- ==========================================================

CREATE DATABASE IF NOT EXISTS mqtt_monitor
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE mqtt_monitor;

-- ----------------------------------------------------------
-- TABLA: subscriptions
-- Propósito: Topics a los que el sistema se suscribe activamente.
--            Solo los mensajes de topics con active = 1 se persisten.
--
-- Relaciones:
--   → Referenciada por mqtt_messages.topic (FK restrictiva)
--
-- Notas:
--   - Se crea primero porque mqtt_messages la referencia via FK.
--   - created_at se asigna via @PrePersist en la entidad Java
--     o via trigger (trg_subscriptions_before_insert).
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS subscriptions (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    topic_name VARCHAR(500) NOT NULL,
    active     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at DATETIME(6)  NOT NULL,

    CONSTRAINT pk_subscriptions       PRIMARY KEY (id),
    CONSTRAINT uq_subscriptions_topic UNIQUE      (topic_name)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ----------------------------------------------------------
-- TABLA: mqtt_messages
-- Propósito: Mensajes MQTT recibidos de topics suscritos.
--
-- Relaciones:
--   → mqtt_messages.topic FK → subscriptions.topic_name
--     ON DELETE RESTRICT: no se puede borrar una suscripción con mensajes.
--     ON UPDATE RESTRICT: no se puede renombrar el topic si tiene mensajes.
--
-- Notas:
--   - La validación de suscripción activa ocurre también en Java
--     (MessageStorageService), pero la FK garantiza consistencia a nivel de BD.
--   - qos admite NULL (el broker puede no informarlo).
--   - received_at se asigna via @PrePersist o trigger before_insert.
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS mqtt_messages (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    topic       VARCHAR(500) NOT NULL,
    client_id   VARCHAR(255) NOT NULL DEFAULT 'unknown',
    payload     TEXT         NOT NULL,
    qos         INT          NULL,
    received_at DATETIME(6)  NOT NULL,

    CONSTRAINT pk_mqtt_messages PRIMARY KEY (id),
    CONSTRAINT fk_mqtt_messages_topic
        FOREIGN KEY (topic)
        REFERENCES subscriptions(topic_name)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ----------------------------------------------------------
-- TABLA: detected_topics
-- Propósito: Topics observados durante el escaneo wildcard "#".
--            Son independientes de subscriptions: un topic puede
--            estar detectado sin estar suscrito, y viceversa.
--
-- Relaciones:
--   - Sin FK explícita hacia subscriptions (independencia intencional).
--   - El trigger trg_mqtt_messages_after_insert la mantiene sincronizada
--     automáticamente con la actividad real de mensajes.
--
-- Notas:
--   - message_count: contador acumulativo de apariciones en escaneos.
--   - active: lo gestiona DiscoveryService al comparar con el escaneo actual.
--   - first_detected / last_seen se asignan via @PrePersist o trigger.
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS detected_topics (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    topic_name     VARCHAR(500) NOT NULL,
    last_client_id VARCHAR(255) NULL,
    first_detected DATETIME(6)  NOT NULL,
    last_seen      DATETIME(6)  NOT NULL,
    active         TINYINT(1)   NOT NULL DEFAULT 1,
    message_count  BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_detected_topics       PRIMARY KEY (id),
    CONSTRAINT uq_detected_topics_topic UNIQUE      (topic_name)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;