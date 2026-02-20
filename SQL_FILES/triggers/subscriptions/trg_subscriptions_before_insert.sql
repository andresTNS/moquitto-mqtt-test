-- ==========================================================
-- trg_subscriptions_before_insert.sql
-- Tabla:    subscriptions
-- Evento:   BEFORE INSERT
-- Proyecto: SubscriptionManager (MQTT Monitor)
--
-- Responsabilidades:
--   1. Normalizar topic_name (trim de espacios).
--   2. Asignar created_at si no viene informado.
--
-- Notas:
--   - La normalización garantiza que "sensor/temp " y "sensor/temp"
--     no generen duplicados que escapen al UNIQUE constraint.
--   - created_at también se asigna en @PrePersist (Java), pero el
--     trigger actúa como segunda línea de defensa para inserciones
--     directas sobre la BD.
-- ==========================================================

USE mqtt_monitor;

DELIMITER $$

DROP TRIGGER IF EXISTS trg_subscriptions_before_insert $$

CREATE TRIGGER trg_subscriptions_before_insert
BEFORE INSERT ON subscriptions
FOR EACH ROW
BEGIN
    -- Normalizar topic_name: eliminar espacios al inicio y al final
    SET NEW.topic_name = TRIM(NEW.topic_name);

    -- Asignar created_at si no viene informado
    IF NEW.created_at IS NULL THEN
        SET NEW.created_at = NOW(6);
    END IF;
END $$

DELIMITER ;
