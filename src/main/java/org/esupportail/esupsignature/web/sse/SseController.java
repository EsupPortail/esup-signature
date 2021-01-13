package org.esupportail.esupsignature.web.sse;

import ch.rasc.sse.eventbus.SseEvent;
import ch.rasc.sse.eventbus.SseEventBus;
import org.esupportail.esupsignature.service.event.EventService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;

@RequestMapping("sse")
@Controller
public class SseController {

    @Resource
    private EventService eventService;

    private final SseEventBus eventBus;

    public SseController(SseEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @GetMapping("/{sseId}")
    public SseEmitter globalSseEmitter(@PathVariable("sseId") String sseId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        eventService.getSseClientIds().put(auth.getName().split("@")[0], sseId);
        return this.eventBus.createSseEmitter(sseId, 150_000L, SseEvent.DEFAULT_EVENT, "global", "user", "sign");
    }

}
