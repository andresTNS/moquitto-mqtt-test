-- ==========================================================
-- trg_detected_topics_before_update.sql
-- Tabla:    detected_topics
-- Evento:   BEFORE UPDATE
-- Proyecto: SubscriptionManager (MQTT Monitor)
--
-- Responsabilidades:
--   1. Actualizar last_seen automáticamente si message_count incrementó
--      Y el caller no proporcionó un last_seen nuevo.
--   2. Proteger message_count: es un contador acumulativo, no puede
--      decrementar (evita correcciones manuales que rompan consistencia).
--   3. Proteger first_detected: campo inmutable una vez asignado.
--   4. Normalizar topic_name si fue modificado (trim de espacios).
--
-- Notas sobre interacción con trg_mqtt_messages_after_insert:
--   Cuando ese trigger ejecuta:
--       UPDATE detected_topics SET message_count = message_count + 1,
--                                  last_seen = NEW.received_at, ...
--   el motor dispara este BEFORE UPDATE con NEW.last_seen ya seteado
--   al valor de received_at (diferente de OLD.last_seen).
--   La condición "NEW.last_seen = OLD.last_seen" detecta este caso
--   y evita sobreescribir el last_seen que ya viene informado.
--   Solo se asigna NOW(6) cuando alguien incrementa message_count
--   sin actualizar last_seen explícitamente (p.ej. update manual).
-- ==========================================================

USE mqtt_monitor;

DELIMITER $$

DROP TRIGGER IF EXISTS trg_detected_topics_before_update $$

CREATE TRIGGER trg_detected_topics_before_update
BEFORE UPDATE ON detected_topics
FOR EACH ROW
BEGIN
    -- Actualizar last_seen solo si message_count incrementó
    -- Y el caller no proporcionó un last_seen nuevo
    IF NEW.message_count > OLD.message_count
       AND NEW.last_seen = OLD.last_seen THEN
        SET NEW.last_seen = NOW(6);
    END IF;

    -- Proteger message_count: es acumulativo, no puede decrementar
    IF NEW.message_count < OLD.message_count THEN
        SET NEW.message_count = OLD.message_count;
    END IF;

    -- Proteger first_detected: campo inmutable
    IF NEW.first_detected <> OLD.first_detected THEN
        SET NEW.first_detected = OLD.first_detected;
    END IF;

    -- Normalizar topic_name si fue modificado
    IF NEW.topic_name <> OLD.topic_name THEN
        SET NEW.topic_name = TRIM(NEW.topic_name);
    END IF;
END $$

DELIMITER ;
