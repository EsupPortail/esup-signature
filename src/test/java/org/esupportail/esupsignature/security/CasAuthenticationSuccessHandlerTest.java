package org.esupportail.esupsignature.security;

import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.security.cas.CasAuthenticationSuccessHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CasAuthenticationSuccessHandlerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingInstitutionalEmailReturnsToLoginWithClearMessage() throws Exception {
        UserService userService = mock(UserService.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("missingmail");
        when(userService.createUserWithAuthentication(
                isNull(), isNull(), isNull(), isNull(),
                org.mockito.ArgumentMatchers.same(authentication),
                org.mockito.ArgumentMatchers.eq(UserType.ldap)))
                .thenThrow(new EsupSignatureUserException(UserService.MISSING_INSTITUTIONAL_EMAIL_MESSAGE));
        CasAuthenticationSuccessHandler handler = new CasAuthenticationSuccessHandler(userService);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo("/");
        assertThat(request.getSession().getAttribute("errorMsg"))
                .isEqualTo(UserService.MISSING_INSTITUTIONAL_EMAIL_MESSAGE);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
