package org.esupportail.esupsignature.web.wssecure;

import jakarta.servlet.http.HttpSession;
import org.esupportail.esupsignature.dto.view.ui.AdminUiStatusDto;
import org.esupportail.esupsignature.dto.view.ui.UiConfigDto;
import org.esupportail.esupsignature.dto.view.ui.UiCountersDto;
import org.esupportail.esupsignature.dto.view.ui.UiMeDto;
import org.esupportail.esupsignature.service.view.ui.AdminUiFetchService;
import org.esupportail.esupsignature.service.view.ui.UiFetchService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ws-secure/ui")
public class UiFetchController {

    private final UiFetchService uiFetchService;
    private final AdminUiFetchService adminUiFetchService;

    public UiFetchController(UiFetchService uiFetchService, AdminUiFetchService adminUiFetchService) {
        this.uiFetchService = uiFetchService;
        this.adminUiFetchService = adminUiFetchService;
    }

    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UiMeDto> getMe(@ModelAttribute("userEppn") String userEppn,
                                         @ModelAttribute("authUserEppn") String authUserEppn,
                                         HttpSession httpSession) {
        return ResponseEntity.ok(uiFetchService.buildUiMe(userEppn, authUserEppn, httpSession));
    }

    @GetMapping(value = "/counters", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UiCountersDto> getCounters(@ModelAttribute("userEppn") String userEppn,
                                                     @ModelAttribute("authUserEppn") String authUserEppn) {
        return ResponseEntity.ok(uiFetchService.buildUiCounters(userEppn, authUserEppn));
    }

    @GetMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UiConfigDto> getConfig(@ModelAttribute("userEppn") String userEppn,
                                                 HttpSession httpSession) {
        return ResponseEntity.ok(uiFetchService.buildUiConfig(userEppn, httpSession != null ? httpSession.getMaxInactiveInterval() : null));
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping(value = "/admin-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdminUiStatusDto> getAdminStatus() {
        return ResponseEntity.ok(adminUiFetchService.buildAdminUiStatus());
    }
}

