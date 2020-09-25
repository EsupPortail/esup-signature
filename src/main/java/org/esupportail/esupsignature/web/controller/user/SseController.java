package org.esupportail.esupsignature.web.controller.user;

import ch.rasc.sse.eventbus.SseEvent;
import ch.rasc.sse.eventbus.SseEventBus;
import org.esupportail.esupsignature.entity.User;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RequestMapping("user/sse")
@Controller
public class SseController {

    private final SseEventBus eventBus;

    private Map<String, String> globalEventBusMap = new HashMap<>();

    public SseController(SseEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @GetMapping
    public SseEmitter globalSseEmitter(@ModelAttribute("user") User user) {
        return this.eventBus.createSseEmitter(user.getEppn(), 30_000L, SseEvent.DEFAULT_EVENT, "global", "user");
    }

}