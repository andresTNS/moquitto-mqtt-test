package cl.thenextsecurity.tns.subscriptionmanager.controller;

import cl.thenextsecurity.tns.subscriptionmanager.service.DiscoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    /**
     * Lanza un escaneo de descubrimiento inmediato en un virtual thread
     * para no bloquear el hilo HTTP mientras dura el Thread.sleep() interno.
     * Sigue el patrón PRG: el redirect llega antes de que termine el escaneo.
     */
    @PostMapping("/discovery/scan")
    public String scanNow() {
        log.info("Escaneo manual solicitado desde la interfaz web");
        Thread.ofVirtual().start(discoveryService::runDiscovery);
        return "redirect:/";
    }

    /**
     * Actualiza la configuración de descubrimiento automático en tiempo de ejecución.
     * El checkbox de "enabled" no se envía cuando está desmarcado (comportamiento HTML),
     * por eso su valor por defecto es false.
     */
    @PostMapping("/discovery/configure")
    public String configure(
            @RequestParam(defaultValue = "false") boolean enabled,
            @RequestParam long interval,
            @RequestParam long duration) {

        discoveryService.updateConfig(enabled, interval, duration);
        log.info("Configuración de discovery actualizada — enabled={}, interval={}ms, duration={}ms",
                enabled, interval, duration);

        return "redirect:/";
    }
}
