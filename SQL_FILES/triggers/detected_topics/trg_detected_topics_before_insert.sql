-- ==========================================================
-- trg_detected_topics_before_insert.sql
-- Tabla:    detected_topics
-- Evento:   BEFORE INSERT
-- Proyecto: SubscriptionManager (MQTT Monitor)
--
-- Responsabilidades:
--   1. Normalizar topic_name (trim de espacios).
--   2. Asignar first_detected y last_seen si no vienen informados.
--   3. Proteger message_count: no puede inicializarse en negativo.
--
-- Notas:
--   - detected_topics se puebla principalmente desde dos fuentes:
--       a) trg_mqtt_messages_after_insert (mensajes en tiempo real).
--       b) DiscoveryService.processDiscoveredTopics() (escaneo periódico).
--     En ambos casos los campos suelen venir informados, pero el trigger
--     actúa como salvaguarda para inserciones directas sobre la BD.
-- ==========================================================

USE mqtt_monitor;

DELIMITER $$

DROP TRIGGER IF EXISTS trg_detected_topics_before_insert $$

CREATE TRIGGER trg_detected_topics_before_insert
BEFORE INSERT ON detected_topics
FOR EACH ROW
BEGIN
    -- Normalizar topic_name: eliminar espacios al inicio y al final
    SET NEW.topic_name = TRIM(NEW.topic_name);

    -- Asignar first_detected si no viene informado
    IF NEW.first_detected IS NULL THEN
        SET NEW.first_detected = NOW(6);
    END IF;

    -- Asignar last_seen si no viene informado
    IF NEW.last_seen IS NULL THEN
        SET NEW.last_seen = NOW(6);
    END IF;

    -- message_count es un contador acumulativo: no puede inicializarse negativo
    IF NEW.message_count < 0 THEN
        SET NEW.message_count = 0;
    END IF;
END $$

DELIMITER ;
