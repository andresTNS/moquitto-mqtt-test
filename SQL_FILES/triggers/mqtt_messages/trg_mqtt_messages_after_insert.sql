-- ==========================================================
-- trg_mqtt_messages_after_insert.sql
-- Tabla:    mqtt_messages
-- Evento:   AFTER INSERT
-- Proyecto: SubscriptionManager (MQTT Monitor)
--
-- Responsabilidades:
--   Mantener detected_topics sincronizado automáticamente con
--   cada mensaje recibido, sin esperar al ciclo de escaneo periódico.
--
--   Caso A — Topic ya existe en detected_topics:
--     - Incrementa message_count en 1.
--     - Actualiza last_seen con received_at del mensaje.
--     - Actualiza last_client_id con el cliente que envió el mensaje.
--     - Reactiva el topic (active = 1) si estaba marcado como inactivo.
--
--   Caso B — Topic no existe en detected_topics:
--     - Inserta un nuevo registro con first_detected = last_seen = received_at.
--     - message_count = 1, active = 1.
--
-- Notas:
--   - El UPDATE del caso A establece last_seen = NEW.received_at
--     explícitamente. El trigger trg_detected_topics_before_update
--     respeta este valor y no lo sobreescribe si ya cambió.
--   - Este trigger complementa al DiscoveryService (escaneo wildcard "#"):
--     el escaneo detecta topics nuevos en la red; este trigger actualiza
--     estadísticas en tiempo real con cada mensaje persistido.
-- ==========================================================

USE mqtt_monitor;

DELIMITER $$

DROP TRIGGER IF EXISTS trg_mqtt_messages_after_insert $$

CREATE TRIGGER trg_mqtt_messages_after_insert
AFTER INSERT ON mqtt_messages
FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM detected_topics WHERE topic_name = NEW.topic) THEN
        -- Caso A: topic conocido → actualizar estadísticas
        UPDATE detected_topics
        SET
            message_count  = message_count + 1,
            last_seen      = NEW.received_at,
            last_client_id = NEW.client_id,
            active         = 1
        WHERE topic_name = NEW.topic;
    ELSE
        -- Caso B: topic nuevo → registrar por primera vez
        INSERT INTO detected_topics (
            topic_name,
            last_client_id,
            first_detected,
            last_seen,
            active,
            message_count
        ) VALUES (
            NEW.topic,
            NEW.client_id,
            NEW.received_at,
            NEW.received_at,
            1,
            1
        );
    END IF;
END $$

DELIMITER ;
