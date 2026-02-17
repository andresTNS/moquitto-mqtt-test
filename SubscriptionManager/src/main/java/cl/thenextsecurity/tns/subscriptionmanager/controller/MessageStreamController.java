package cl.thenextsecurity.tns.subscriptionmanager.controller;

import cl.thenextsecurity.tns.subscriptionmanager.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
@RequiredArgsConstructor
public class MessageStreamController {

    private final SseService sseService;

    /**
     * Endpoint SSE al que se conecta dashboard.js via new EventSource("/messages/stream").
     * Spring MVC gestiona automáticamente el Content-Type text/event-stream.
     * Se usa @Controller + @ResponseBody (no @RestController) para mantener
     * la coherencia arquitectónica del proyecto con controladores web clásicos.
     */
    @GetMapping(value = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter stream() {
        return sseService.addEmitter();
    }
}
