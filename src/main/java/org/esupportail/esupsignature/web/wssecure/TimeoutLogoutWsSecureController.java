package org.esupportail.esupsignature.web.wssecure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.esupportail.esupsignature.service.security.LocalSessionLogoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ws-secure")
public class TimeoutLogoutWsSecureController {

    private final LocalSessionLogoutService localSessionLogoutService;

    public TimeoutLogoutWsSecureController(LocalSessionLogoutService localSessionLogoutService) {
        this.localSessionLogoutService = localSessionLogoutService;
    }

    @PostMapping("/timeout-logout")
    public ResponseEntity<Void> timeoutLogout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        localSessionLogoutService.logoutCurrentSession(request, response, authentication);
        return ResponseEntity.noContent().build();
    }
}
