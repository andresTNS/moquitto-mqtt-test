-- ==========================================================
-- trg_mqtt_messages_before_insert.sql
-- Tabla:    mqtt_messages
-- Evento:   BEFORE INSERT
-- Proyecto: SubscriptionManager (MQTT Monitor)
--
-- Responsabilidades:
--   1. Validar que qos sea 0, 1 o 2 (estándar MQTT).
--   2. Normalizar client_id (trim; "unknown" si viene vacío/NULL).
--   3. Asignar received_at si no viene informado.
--
-- Notas:
--   - La validación de QoS es un control que Java no implementa
--     explícitamente; el trigger actúa como garantía a nivel de BD.
--   - client_id = "unknown" es el valor por defecto definido en la
--     entidad Java (@Builder.Default). El trigger refuerza esto para
--     inserciones directas sobre la BD.
--   - received_at también se asigna en @PrePersist (Java); el trigger
--     es la segunda línea de defensa.
-- ==========================================================

USE mqtt_monitor;

DELIMITER $$

DROP TRIGGER IF EXISTS trg_mqtt_messages_before_insert $$

CREATE TRIGGER trg_mqtt_messages_before_insert
BEFORE INSERT ON mqtt_messages
FOR EACH ROW
BEGIN
    -- Validar QoS: solo se permiten valores 0, 1 o 2 (protocolo MQTT)
    IF NEW.qos IS NOT NULL AND NEW.qos NOT IN (0, 1, 2) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'QoS inválido: debe ser 0, 1 o 2 según el protocolo MQTT';
    END IF;

    -- Normalizar client_id: trim y fallback a "unknown"
    IF NEW.client_id IS NULL OR TRIM(NEW.client_id) = '' THEN
        SET NEW.client_id = 'unknown';
    ELSE
        SET NEW.client_id = TRIM(NEW.client_id);
    END IF;

    -- Asignar received_at si no viene informado
    IF NEW.received_at IS NULL THEN
        SET NEW.received_at = NOW(6);
    END IF;
END $$

DELIMITER ;
