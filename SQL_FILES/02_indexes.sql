-- ==========================================================
-- 02_indexes.sql
-- Creación de índices
-- Proyecto: SubscriptionManager (MQTT Monitor)
--
-- PREREQUISITO: Ejecutar 01_schema.sql antes que este archivo.
--
-- Criterios de inclusión:
--   [JPA]  Índice definido en anotación @Index de la entidad Java.
--   [REPO] Índice derivado de queries en los repositorios Spring Data.
--   [PERF] Índice adicional para mejorar rendimiento en consultas compuestas.
-- ==========================================================

USE mqtt_monitor;

-- ----------------------------------------------------------
-- ÍNDICES: subscriptions
-- ----------------------------------------------------------

-- [JPA]  Filtrado por estado activo (findByActiveTrue)
CREATE INDEX idx_subscriptions_active
    ON subscriptions (active);

-- [PERF] Validación de suscripción activa por topic (consulta más frecuente:
--        ¿existe una suscripción activa para este topic?)
CREATE INDEX idx_subscriptions_active_topic
    ON subscriptions (active, topic_name);


-- ----------------------------------------------------------
-- ÍNDICES: mqtt_messages
-- ----------------------------------------------------------

-- [JPA]  Búsqueda de mensajes por topic (findByTopic)
CREATE INDEX idx_mqtt_messages_topic
    ON mqtt_messages (topic);

-- [JPA]  Búsqueda de mensajes por cliente (findByClientId)
CREATE INDEX idx_mqtt_messages_client_id
    ON mqtt_messages (client_id);

-- [JPA]  Búsqueda y ordenación por fecha de recepción
--        (findByReceivedAtBetween, findTop100ByOrderByReceivedAtDesc)
CREATE INDEX idx_mqtt_messages_received_at
    ON mqtt_messages (received_at);

-- [JPA]  Búsqueda de mensajes por topic en rango de fechas
--        (findByTopicAndReceivedAtBetween)
CREATE INDEX idx_mqtt_messages_topic_received_at
    ON mqtt_messages (topic, received_at);

-- [REPO] Búsqueda de mensajes por cliente en rango de fechas
--        (patrón frecuente en consultas de auditoría por dispositivo)
CREATE INDEX idx_mqtt_messages_client_received_at
    ON mqtt_messages (client_id, received_at);


-- ----------------------------------------------------------
-- ÍNDICES: detected_topics
-- ----------------------------------------------------------

-- [JPA]  Filtrado por estado activo (findByActiveTrue)
CREATE INDEX idx_detected_topics_active
    ON detected_topics (active);

-- [JPA]  Ordenación por última vez visto (findAllByOrderByLastSeenDesc)
CREATE INDEX idx_detected_topics_last_seen
    ON detected_topics (last_seen);

-- [PERF] Listado de topics activos ordenado por actividad reciente
--        (consulta de dashboard: activos más recientes primero)
CREATE INDEX idx_detected_topics_active_last_seen
    ON detected_topics (active, last_seen);