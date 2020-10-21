package org.esupportail.esupsignature.web.sse;

import ch.rasc.sse.eventbus.SseEvent;
import ch.rasc.sse.eventbus.SseEventBus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RequestMapping("sse")
@Controller
public class SseController {

    private final SseEventBus eventBus;

    public SseController(SseEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @GetMapping
    public SseEmitter globalSseEmitter() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String clientId = auth.getName().split("@")[0];
        return this.eventBus.createSseEmitter(clientId, 5_000L, SseEvent.DEFAULT_EVENT, "global", "user", "sign");
    }

}
