package cl.thenextsecurity.tns.subscriptionmanager.controller;

import cl.thenextsecurity.tns.subscriptionmanager.service.SseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MessageStreamController.class)
@DisplayName("MessageStreamController — tests de capa web")
class MessageStreamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SseService sseService;

    // =========================================================================
    // GET /messages/stream
    // =========================================================================

    @Test
    @DisplayName("GET /messages/stream retorna HTTP 200")
    void stream_retornaHttp200() throws Exception {
        when(sseService.addEmitter()).thenReturn(new SseEmitter());

        mockMvc.perform(get("/messages/stream"))
                .andExpect(status().isOk());
    }

    // TODO: Este test falla intermitentemente con @WebMvcTest en Spring Boot 4.0.2.
    // SseEmitter es asíncrono y el Content-Type no siempre se escribe antes de que
    // MockMvc lea la respuesta. Revisar en rama dedicada el uso de asyncDispatch().
    @Test
    @DisplayName("GET /messages/stream retorna Content-Type text/event-stream")
    void stream_retornaContentTypeTextEventStream() throws Exception {
        when(sseService.addEmitter()).thenReturn(new SseEmitter());

        mockMvc.perform(get("/messages/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    @Test
    @DisplayName("GET /messages/stream delega a sseService.addEmitter()")
    void stream_delegaASseServiceAddEmitter() throws Exception {
        when(sseService.addEmitter()).thenReturn(new SseEmitter());

        mockMvc.perform(get("/messages/stream"));

        verify(sseService).addEmitter();
    }
}
