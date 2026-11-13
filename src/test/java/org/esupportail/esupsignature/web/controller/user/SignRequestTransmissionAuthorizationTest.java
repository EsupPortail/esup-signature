package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SignRequestTransmissionAuthorizationTest {
    @Configuration
    @EnableMethodSecurity
    static class SecurityConfiguration { }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requiresRoleAndViewRightsAndUsesTheAuthenticatedSender() throws Exception {
        SignRequestService requests = mock(SignRequestService.class);
        PreAuthorizeService rights = mock(PreAuthorizeService.class);
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(SecurityConfiguration.class);
            context.registerBean(SignRequestService.class, () -> requests);
            context.registerBean(SignBookService.class, () -> mock(SignBookService.class));
            context.registerBean(OtpService.class, () -> mock(OtpService.class));
            context.registerBean("preAuthorizeService", PreAuthorizeService.class, () -> rights);
            context.registerBean("messageSource", ResourceBundleMessageSource.class, () -> {
                ResourceBundleMessageSource source = new ResourceBundleMessageSource();
                source.setBasename("i18n/messages");
                source.setDefaultEncoding("UTF-8");
                return source;
            });
            context.registerBean(SignRequestController.class);
            context.refresh();
            SignRequestController controller = context.getBean(SignRequestController.class);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "otp", "unused", List.of(new SimpleGrantedAuthority("ROLE_OTP"))));
            assertThatThrownBy(() -> controller.transmitEmail(42L, "recipient@example.org", "Test comment", "effective", "actual", new RedirectAttributesModelMap()))
                    .isInstanceOf(AccessDeniedException.class);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "actual", "unused", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            assertThatThrownBy(() -> controller.transmitEmail(42L, "recipient@example.org", "Test comment", "effective", "actual", new RedirectAttributesModelMap()))
                    .isInstanceOf(AccessDeniedException.class);
            verify(requests, never()).transmitSignedDocument(anyLong(), anyString(), anyString(), any());
            when(rights.signRequestView(42L, "effective", "actual")).thenReturn(true);
            when(requests.transmitSignedDocument(42L, "recipient@example.org", "actual", "Test comment")).thenReturn(true);
            RedirectAttributesModelMap attributes = new RedirectAttributesModelMap();
            assertThat(controller.transmitEmail(42L, "recipient@example.org", "Test comment", "effective", "actual", attributes))
                    .isEqualTo("redirect:/user/signrequests/42");
            verify(requests).transmitSignedDocument(42L, "recipient@example.org", "actual", "Test comment");
            assertThat(attributes.getFlashAttributes()).containsKey("message");
            assertThat(controller.loginForControl("token", attributes, new org.springframework.mock.web.MockHttpSession())).isEqualTo("redirect:/public/control/{token}");
            assertThat(attributes.getAttribute("token")).isEqualTo("token");
        }
    }
}
