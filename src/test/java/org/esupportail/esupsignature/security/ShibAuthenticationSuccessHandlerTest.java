package org.esupportail.esupsignature.security;

import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.security.shib.ShibAuthenticationSuccessHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShibAuthenticationSuccessHandlerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void newAccountWithoutMailReturnsToLoginWithClearMessage() throws Exception {
        UserService userService = mock(UserService.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("newuser@univ.fr");
        when(userService.createUserWithAuthentication(
                eq("newuser@univ.fr"), eq("Name"), eq("Firstname"), isNull(),
                same(authentication), eq(UserType.shib)))
                .thenThrow(new EsupSignatureUserException(UserService.MISSING_INSTITUTIONAL_EMAIL_MESSAGE));
        ShibAuthenticationSuccessHandler handler = new ShibAuthenticationSuccessHandler();
        ReflectionTestUtils.setField(handler, "userService", userService);
        ReflectionTestUtils.setField(handler, "registerSessionAuthenticationStrategy", mock(RegisterSessionAuthenticationStrategy.class));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("sn", "Name");
        request.addHeader("givenName", "Firstname");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo("/");
        assertThat(request.getSession().getAttribute("errorMsg"))
                .isEqualTo(UserService.MISSING_INSTITUTIONAL_EMAIL_MESSAGE);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
