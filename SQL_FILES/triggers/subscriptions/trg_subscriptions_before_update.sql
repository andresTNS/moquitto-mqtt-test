-- ==========================================================
-- trg_subscriptions_before_update.sql
-- Tabla:    subscriptions
-- Evento:   BEFORE UPDATE
-- Proyecto: SubscriptionManager (MQTT Monitor)
--
-- Responsabilidades:
--   1. Normalizar topic_name si fue modificado (trim de espacios).
--
-- Notas:
--   - En escenario con FK RESTRICT en mqtt_messages, un cambio
--     de topic_name será rechazado por el motor si existen mensajes
--     asociados, ANTES de que este trigger intente normalizarlo.
--     El trigger solo actúa cuando el cambio es permitido.
--   - created_at no se permite modificar (campo inmutable por diseño).
-- ==========================================================

USE mqtt_monitor;

DELIMITER $$

DROP TRIGGER IF EXISTS trg_subscriptions_before_update $$

CREATE TRIGGER trg_subscriptions_before_update
BEFORE UPDATE ON subscriptions
FOR EACH ROW
BEGIN
    -- Normalizar topic_name solo si fue modificado
    IF NEW.topic_name <> OLD.topic_name THEN
        SET NEW.topic_name = TRIM(NEW.topic_name);
    END IF;

    -- Proteger created_at: no puede modificarse una vez asignado
    IF NEW.created_at <> OLD.created_at THEN
        SET NEW.created_at = OLD.created_at;
    END IF;
END $$

DELIMITER ;
