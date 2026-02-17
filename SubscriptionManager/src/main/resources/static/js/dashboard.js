/**
 * MQTT Monitor - Dashboard JavaScript
 *
 * Responsabilidad unica: conectar via SSE al endpoint /messages/stream
 * y renderizar los mensajes en tiempo real en el DOM.
 * No usa frameworks, no hace fetch, no construye nada mas.
 */

(function () {
  "use strict";

  var MAX_MESSAGES = 100;
  var messageLog = document.getElementById("message-log");

  if (!messageLog) {
    console.error("No se encontro el elemento #message-log en el DOM.");
    return;
  }

  // --- Conexion SSE ---
  var eventSource = new EventSource("/messages/stream");

  /**
   * Evento "message": llega un mensaje MQTT en tiempo real.
   * Formato esperado del JSON: { topic, clientId, payload, timestamp }
   */
  eventSource.addEventListener("message", function (event) {
    try {
      var data = JSON.parse(event.data);
      addMessageToLog(data);
    } catch (e) {
      console.error("Error al parsear evento SSE:", e);
    }
  });

  /**
   * Evento "topics-updated": el servidor termino un escaneo de descubrimiento.
   * Se recarga la pagina para reflejar los nuevos topics en la tabla.
   */
  eventSource.addEventListener("topics-updated", function () {
    location.reload();
  });

  /**
   * Handler de error de la conexion SSE.
   */
  eventSource.onerror = function (err) {
    console.error("Error en la conexion SSE:", err);
  };

  // --- Funciones auxiliares ---

  /**
   * Crea un elemento DOM con la informacion del mensaje y lo inserta
   * al inicio del log. Limita el log a MAX_MESSAGES elementos.
   */
  function addMessageToLog(data) {
    // Remover mensaje de "vacio" si existe
    var emptyMsg = messageLog.querySelector(".message-log-empty");
    if (emptyMsg) {
      emptyMsg.remove();
    }

    var item = document.createElement("div");
    item.className = "message-item";

    // Header del mensaje
    var header = document.createElement("div");
    header.className = "message-header";

    var timestamp = document.createElement("span");
    timestamp.className = "msg-timestamp";
    timestamp.textContent = formatTimestamp(data.timestamp);

    var topic = document.createElement("span");
    topic.className = "msg-topic";
    topic.textContent = data.topic || "N/A";

    var client = document.createElement("span");
    client.className = "msg-client";
    client.textContent = "Client: " + (data.clientId || "unknown");

    header.appendChild(timestamp);
    header.appendChild(topic);
    header.appendChild(client);

    // Payload
    var payload = document.createElement("pre");
    payload.className = "message-payload";
    payload.textContent = formatPayload(data.payload);

    item.appendChild(header);
    item.appendChild(payload);

    // Insertar al inicio
    messageLog.insertBefore(item, messageLog.firstChild);

    // Limitar cantidad de elementos
    while (messageLog.children.length > MAX_MESSAGES) {
      messageLog.removeChild(messageLog.lastChild);
    }
  }

  /**
   * Formatea un timestamp ISO a formato legible local.
   */
  function formatTimestamp(ts) {
    if (!ts) return new Date().toLocaleString();
    try {
      return new Date(ts).toLocaleString();
    } catch (e) {
      return ts;
    }
  }

  /**
   * Intenta formatear el payload como JSON con indentacion.
   * Si no es JSON valido, retorna el string tal cual.
   */
  function formatPayload(payload) {
    if (!payload) return "";
    try {
      var parsed = JSON.parse(payload);
      return JSON.stringify(parsed, null, 2);
    } catch (e) {
      return payload;
    }
  }
})();
