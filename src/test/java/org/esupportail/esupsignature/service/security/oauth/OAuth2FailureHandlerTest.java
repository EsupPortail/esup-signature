package org.esupportail.esupsignature.service.security.oauth;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.entity.Otp;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.esupportail.esupsignature.web.controller.otp.OtpAccessController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuth2FailureHandlerTest {

    @Test
    void cancellationReturnsToOtpEntryAndClearsRedirects() throws Exception {
        MockHttpServletRequest request = cancelledRequest();
        request.setParameter("state", "state-123=");
        request.setParameter("iss", "https://oidc.franceconnect.gouv.fr/api/v2");
        request.getSession().setAttribute(OAuth2FailureHandler.AFTER_OAUTH_FAILURE_REDIRECT, "/otp-access/first/url-token");
        request.getSession().setAttribute("after_oauth_redirect", "/otp/signrequests/signbook-redirect/42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new OAuth2FailureHandler().onAuthenticationFailure(request, response,
                new AuthenticationServiceException("[access_denied] User auth aborted"));

        assertThat(response.getRedirectedUrl()).isEqualTo("/otp-access/first/url-token?oauth2_cancelled=true");
        assertThat(request.getSession().getAttribute(OAuth2FailureHandler.AFTER_OAUTH_FAILURE_REDIRECT)).isNull();
        assertThat(request.getSession().getAttribute("after_oauth_redirect")).isNull();
    }

    @Test
    void cancellationPreservesExistingQueryParameters() throws Exception {
        MockHttpServletRequest request = cancelledRequest();
        request.getSession().setAttribute(OAuth2FailureHandler.AFTER_OAUTH_FAILURE_REDIRECT, "/otp-access/first/url-token?existing=true");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new OAuth2FailureHandler().onAuthenticationFailure(request, response, new AuthenticationServiceException("access_denied"));

        assertThat(response.getRedirectedUrl()).isEqualTo("/otp-access/first/url-token?existing=true&oauth2_cancelled=true");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"https://example.org", "//example.org", "relative-path"})
    void cancellationWithoutSafeRedirectUsesErrorPage(String redirect) throws Exception {
        MockHttpServletRequest request = cancelledRequest();
        if (redirect != null) {
            request.getSession().setAttribute(OAuth2FailureHandler.AFTER_OAUTH_FAILURE_REDIRECT, redirect);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();

        new OAuth2FailureHandler().onAuthenticationFailure(request, response, new AuthenticationServiceException("access_denied"));

        assertThat(response.getRedirectedUrl()).isEqualTo("/otp-access/oauth2?error=access_denied&error_description=User+auth+aborted");
    }

    @Test
    void providerErrorKeepsErrorPageAndEncodedParameters() throws Exception {
        MockHttpServletRequest request = cancelledRequest();
        request.setParameter("error", "server_error");
        request.setParameter("error_description", "Provider unavailable");
        request.setParameter("state", "state-123=");
        request.getSession().setAttribute(OAuth2FailureHandler.AFTER_OAUTH_FAILURE_REDIRECT, "/otp-access/first/url-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new OAuth2FailureHandler().onAuthenticationFailure(request, response, new AuthenticationServiceException("provider failure"));

        assertThat(response.getRedirectedUrl()).isEqualTo("/otp-access/oauth2?error=server_error&error_description=Provider+unavailable&state=state-123%3D");
    }

    @Test
    void otpEntryStoresReturnUrlAndDisplaysTranslatedCancellationMessage() throws Exception {
        GlobalProperties globalProperties = new GlobalProperties();
        globalProperties.setSmsRequired(true);
        globalProperties.setNbViewOtpTries(3);
        OtpService otpService = mock(OtpService.class);
        SignBookService signBookService = mock(SignBookService.class);
        SignBook signBook = new SignBook();
        signBook.setId(42L);
        signBook.setStatus(SignRequestStatus.pending);
        Otp otp = new Otp();
        otp.setSignature(false);
        otp.setTries(0);
        otp.setSignBook(signBook);
        when(otpService.getAndCheckOtpFromDatabase("url-token")).thenReturn(otp);
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/messages");
        OtpAccessController controller = new OtpAccessController(globalProperties, otpService,
                signBookService, mock(UserService.class), List.of(), null, messageSource);
        MockHttpServletRequest request = new MockHttpServletRequest();
        ConcurrentModel model = new ConcurrentModel();

        assertThat(controller.signin("url-token", model, request, new RedirectAttributesModelMap())).isEqualTo("otp/signin");
        assertThat(request.getSession().getAttribute(OAuth2FailureHandler.AFTER_OAUTH_FAILURE_REDIRECT)).isEqualTo("/otp-access/first/url-token");
        assertThat(model.getAttribute("message")).isNull();

        request.setParameter("oauth2_cancelled", "true");
        assertThat(controller.signin("url-token", model, request, new RedirectAttributesModelMap())).isEqualTo("otp/signin");
        JsMessage message = (JsMessage) model.getAttribute("message");
        assertThat(message).isNotNull();
        assertThat(message.getType()).isEqualTo("info");
        assertThat(message.getText()).contains("FranceConnect annulée", "réessayer");
    }

    private MockHttpServletRequest cancelledRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/franceconnect");
        request.setParameter("error", "access_denied");
        request.setParameter("error_description", "User auth aborted");
        return request;
    }
}
