package org.esupportail.esupsignature.security;

import jakarta.servlet.http.Cookie;
import org.esupportail.esupsignature.config.security.WebSecurityConfig;
import org.esupportail.esupsignature.config.security.WebSecurityProperties;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.security.cas.CasJwtDecoder;
import org.esupportail.esupsignature.config.sms.SmsProperties;
import org.esupportail.esupsignature.entity.AuditTrail;
import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.Otp;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.WsAccessToken;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.WsAccessTokenRepository;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.interfaces.sms.SmsService;
import org.esupportail.esupsignature.service.security.LogoutHandlerImpl;
import org.esupportail.esupsignature.service.security.OidcOtpSecurityService;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.esupportail.esupsignature.service.security.oauth.OAuthAuthenticationSuccessHandler;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.esupportail.esupsignature.service.utils.sign.ValidationService;
import org.esupportail.esupsignature.web.PublicController;
import org.esupportail.esupsignature.web.controller.otp.OtpAccessController;
import org.esupportail.esupsignature.web.ws.WsControllerAdvice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import java.io.File;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

class GlobalSecurityAttackSurfaceTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class WsAccessTokenServiceTests {

        @Test
        void wsApiAuthorizerShouldFailOpenWhenNoWsTokenExists() {
            WsAccessTokenRepository repository = mock(WsAccessTokenRepository.class);
            WsAccessTokenService wsAccessTokenService = wsAccessTokenService(repository);

            when(repository.findAll()).thenReturn(List.of());

            assertTrue(wsAccessTokenService.isAllAccess("does-not-matter"));
        }

        @Test
        void wsApiAuthorizerShouldFailOpenWhenNoGlobalTokenExistsAndProvidedTokenIsUnknown() {
            WsAccessTokenRepository repository = mock(WsAccessTokenRepository.class);
            WsAccessTokenService wsAccessTokenService = wsAccessTokenService(repository);

            WsAccessToken scopedToken = new WsAccessToken();
            scopedToken.setAppName("scoped-client");

            when(repository.findAll()).thenReturn(List.of(scopedToken));
            when(repository.findByTokenIsNullAndWorkflowsEmpty()).thenReturn(List.of());
            when(repository.findByWorkflowsEmpty()).thenReturn(List.of());
            when(repository.findByToken("unknown-token")).thenReturn(null);
            when(repository.findByTokenAndWorkflowsEmpty("unknown-token")).thenReturn(List.of());

            assertTrue(wsAccessTokenService.isAllAccess("unknown-token"));
        }

        @Test
        void wsApiAuthorizerShouldGrantAllAccessWhenGlobalPublicTokenExists() {
            WsAccessTokenRepository repository = mock(WsAccessTokenRepository.class);
            WsAccessTokenService wsAccessTokenService = wsAccessTokenService(repository);

            WsAccessToken globalToken = new WsAccessToken();
            globalToken.setPublicAccess(true);

            when(repository.findAll()).thenReturn(List.of(globalToken));
            when(repository.findByTokenIsNullAndWorkflowsEmpty()).thenReturn(List.of());
            when(repository.findByWorkflowsEmpty()).thenReturn(List.of(globalToken));
            when(repository.findByTokenAndWorkflowsEmpty("unknown-token")).thenReturn(List.of());

            assertTrue(wsAccessTokenService.isAllAccess("unknown-token"));
        }

        @Test
        void wsApiAuthorizerShouldDenyAllAccessWhenOnlyNonPublicGlobalTokenExists() {
            WsAccessTokenRepository repository = mock(WsAccessTokenRepository.class);
            WsAccessTokenService wsAccessTokenService = wsAccessTokenService(repository);

            WsAccessToken globalToken = new WsAccessToken();
            globalToken.setToken("known-global-token");
            globalToken.setPublicAccess(false);
            globalToken.setReadSignrequest(false);

            when(repository.findAll()).thenReturn(List.of(globalToken));
            when(repository.findByTokenIsNullAndWorkflowsEmpty()).thenReturn(List.of());
            when(repository.findByWorkflowsEmpty()).thenReturn(List.of(globalToken));
            when(repository.findByToken("unknown-token")).thenReturn(null);
            when(repository.findByTokenAndWorkflowsEmpty("unknown-token")).thenReturn(List.of());

            assertFalse(wsAccessTokenService.isAllAccess("unknown-token"));
        }

        @Test
        void wsApiAuthorizerShouldConsiderKnownSpecificTokenAsExisting() {
            WsAccessTokenRepository repository = mock(WsAccessTokenRepository.class);
            WsAccessTokenService wsAccessTokenService = wsAccessTokenService(repository);

            WsAccessToken globalToken = new WsAccessToken();
            globalToken.setToken("global-token");

            WsAccessToken scopedToken = new WsAccessToken();
            scopedToken.setToken("scoped-token");

            when(repository.findAll()).thenReturn(List.of(globalToken, scopedToken));
            when(repository.findByTokenIsNullAndWorkflowsEmpty()).thenReturn(List.of());
            when(repository.findByWorkflowsEmpty()).thenReturn(List.of(globalToken));
            when(repository.findByToken("scoped-token")).thenReturn(scopedToken);
            when(repository.findByTokenAndWorkflowsEmpty("scoped-token")).thenReturn(List.of());

            assertTrue(wsAccessTokenService.isTokenExist("scoped-token"));
        }

        @Test
        void wsApiAuthorizerShouldRejectUnknownTokenWhenNoFailOpenConditionApplies() {
            WsAccessTokenRepository repository = mock(WsAccessTokenRepository.class);
            WsAccessTokenService wsAccessTokenService = wsAccessTokenService(repository);

            WsAccessToken globalToken = new WsAccessToken();
            globalToken.setToken("global-token");
            globalToken.setPublicAccess(false);
            globalToken.setReadSignrequest(false);

            when(repository.findAll()).thenReturn(List.of(globalToken));
            when(repository.findByTokenIsNullAndWorkflowsEmpty()).thenReturn(List.of());
            when(repository.findByWorkflowsEmpty()).thenReturn(List.of(globalToken));
            when(repository.findByToken("unknown-token")).thenReturn(null);
            when(repository.findByTokenAndWorkflowsEmpty("unknown-token")).thenReturn(List.of());

            assertFalse(wsAccessTokenService.isTokenExist("unknown-token"));
        }
    }

    @Nested
    class OtpAccessControllerTests {

        @Test
        void otpEntryPointShouldCreateAuthenticatedSessionFromUrlTokenOnlyWhenSmsIsNotRequired() throws Exception {
            GlobalProperties globalProperties = new GlobalProperties();
            globalProperties.setSmsRequired(false);
            globalProperties.setNbSignOtpTries(3);
            OtpService otpService = mock(OtpService.class);
            SignBookService signBookService = mock(SignBookService.class);
            UserService userService = mock(UserService.class);
            SmsProperties smsProperties = new SmsProperties();

            OtpAccessController controller = new OtpAccessController(
                    globalProperties,
                    otpService,
                    signBookService,
                    userService,
                    List.of(),
                    null,
                    null,
                    smsProperties
            );

            Otp otp = otp("public-bearer-link", false, 0, 42L);
            when(otpService.getAndCheckOtpFromDatabase("public-bearer-link")).thenReturn(otp);

            ConcurrentModel model = new ConcurrentModel();
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/otp-access/first/public-bearer-link");

            String view = controller.signin("public-bearer-link", model, request, new RedirectAttributesModelMap());

            assertEquals("redirect:/otp/signrequests/signbook-redirect/42", view);
            assertOtpAuthenticationStoredInSession(request);
        }

        @Test
        void otpEntryPointShouldRenderSigninPageWhenSmsIsRequired() throws Exception {
            GlobalProperties globalProperties = new GlobalProperties();
            globalProperties.setSmsRequired(true);
            globalProperties.setNbSignOtpTries(3);
            OtpService otpService = mock(OtpService.class);
            SignBookService signBookService = mock(SignBookService.class);
            UserService userService = mock(UserService.class);
            SmsProperties smsProperties = new SmsProperties();

            OtpAccessController controller = new OtpAccessController(
                    globalProperties,
                    otpService,
                    signBookService,
                    userService,
                    List.of(),
                    null,
                    null,
                    smsProperties
            );

            Otp otp = otp("sms-required-link", false, 0, 52L);
            when(otpService.getAndCheckOtpFromDatabase("sms-required-link")).thenReturn(otp);
            when(signBookService.getExternalAuths(eq(52L), anyList())).thenReturn(List.of());

            ConcurrentModel model = new ConcurrentModel();
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/otp-access/first/sms-required-link");

            String view = controller.signin("sms-required-link", model, request, new RedirectAttributesModelMap());

            assertEquals("otp/signin", view);
            assertEquals(Boolean.TRUE, model.getAttribute("smsRequired"));
            var session = request.getSession(false);
            assertNotNull(session);
            assertEquals("/otp/signrequests/signbook-redirect/52", session.getAttribute("after_oauth_redirect"));
            assertNull(session.getAttribute(SPRING_SECURITY_CONTEXT_KEY));
        }

        @Test
        void otpEntryPointShouldRenderSigninPageWhenForceSmsIsEnabledOnOtp() throws Exception {
            GlobalProperties globalProperties = new GlobalProperties();
            globalProperties.setSmsRequired(false);
            globalProperties.setNbSignOtpTries(3);
            OtpService otpService = mock(OtpService.class);
            SignBookService signBookService = mock(SignBookService.class);
            UserService userService = mock(UserService.class);

            OtpAccessController controller = new OtpAccessController(
                    globalProperties,
                    otpService,
                    signBookService,
                    userService,
                    List.of(),
                    null,
                    null,
                    new SmsProperties()
            );

            Otp otp = otp("force-sms-link", true, 0, 53L);
            when(otpService.getAndCheckOtpFromDatabase("force-sms-link")).thenReturn(otp);
            when(signBookService.getExternalAuths(eq(53L), anyList())).thenReturn(List.of());

            ConcurrentModel model = new ConcurrentModel();
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/otp-access/first/force-sms-link");

            String view = controller.signin("force-sms-link", model, request, new RedirectAttributesModelMap());

            assertEquals("otp/signin", view);
            assertEquals(Boolean.TRUE, model.getAttribute("smsRequired"));
        }

        @Test
        void otpEntryPointShouldRenderExpiredViewWhenOtpStillExistsButHasNoRemainingTries() throws Exception {
            GlobalProperties globalProperties = new GlobalProperties();
            globalProperties.setSmsRequired(false);
            globalProperties.setNbSignOtpTries(3);
            OtpService otpService = mock(OtpService.class);
            SignBookService signBookService = mock(SignBookService.class);
            UserService userService = mock(UserService.class);

            OtpAccessController controller = new OtpAccessController(
                    globalProperties,
                    otpService,
                    signBookService,
                    userService,
                    List.of(),
                    null,
                    null,
                    new SmsProperties()
            );

            Otp expiredOtp = otp("expired-link", false, 3, 66L);
            when(otpService.getAndCheckOtpFromDatabase("expired-link")).thenReturn(expiredOtp);
            when(otpService.getOtpFromDatabase("expired-link")).thenReturn(expiredOtp);

            String view = controller.signin("expired-link", new ConcurrentModel(), new MockHttpServletRequest(), new RedirectAttributesModelMap());

            assertEquals("otp/expired", view);
        }

        @Test
        void otpEntryPointShouldRedirectToErrorWhenOtpCannotBeFound() throws Exception {
            GlobalProperties globalProperties = new GlobalProperties();
            OtpService otpService = mock(OtpService.class);
            SignBookService signBookService = mock(SignBookService.class);
            UserService userService = mock(UserService.class);

            OtpAccessController controller = new OtpAccessController(
                    globalProperties,
                    otpService,
                    signBookService,
                    userService,
                    List.of(),
                    null,
                    null,
                    new SmsProperties()
            );

            when(otpService.getAndCheckOtpFromDatabase("missing-link")).thenReturn(null);
            when(otpService.getOtpFromDatabase("missing-link")).thenReturn(null);

            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
            String view = controller.signin("missing-link", new ConcurrentModel(), new MockHttpServletRequest(), redirectAttributes);

            assertEquals("redirect:/otp-access/error", view);
            assertNotNull(redirectAttributes.getFlashAttributes().get("errorMsg"));
        }

        @Test
        void otpAuthenticationShouldCreateSessionWhenSmsCodeMatches() {
            GlobalProperties globalProperties = new GlobalProperties();
            globalProperties.setSmsRequired(true);
            OtpService otpService = mock(OtpService.class);
            SignBookService signBookService = mock(SignBookService.class);
            UserService userService = mock(UserService.class);

            OtpAccessController controller = new OtpAccessController(
                    globalProperties,
                    otpService,
                    signBookService,
                    userService,
                    List.of(),
                    mock(SmsService.class),
                    null,
                    new SmsProperties()
            );

            Otp otp = otp("sms-auth-link", false, 0, 77L);
            otp.setPhoneNumber("0600000000");
            when(otpService.getAndCheckOtpFromDatabase("sms-auth-link")).thenReturn(otp);
            when(otpService.checkOtp("sms-auth-link", "123456")).thenReturn(true);

            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/otp-access");
            String view = controller.auth("sms-auth-link", "123456", new ConcurrentModel(), new RedirectAttributesModelMap(), request);

            assertEquals("redirect:/otp/signrequests/signbook-redirect/77", view);
            assertOtpAuthenticationStoredInSession(request);
        }

        @Test
        void otpAuthenticationShouldReissueCodeAndRedirectWhenSmsCodeIsWrong() {
            GlobalProperties globalProperties = new GlobalProperties();
            globalProperties.setSmsRequired(true);
            OtpService otpService = mock(OtpService.class);
            SignBookService signBookService = mock(SignBookService.class);
            UserService userService = mock(UserService.class);
            SmsService smsService = mock(SmsService.class);

            OtpAccessController controller = new OtpAccessController(
                    globalProperties,
                    otpService,
                    signBookService,
                    userService,
                    List.of(),
                    smsService,
                    null,
                    new SmsProperties()
            );

            Otp otp = otp("wrong-sms-link", false, 0, 88L);
            otp.getUser().setEmail("otp-user@example.org");
            otp.setPhoneNumber("0600000000");
            when(otpService.getAndCheckOtpFromDatabase("wrong-sms-link")).thenReturn(otp);
            when(otpService.checkOtp("wrong-sms-link", "000000")).thenReturn(false);
            when(otpService.generateOtpPassword("wrong-sms-link", "0600000000")).thenReturn("654321");

            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
            String view = controller.auth("wrong-sms-link", "000000", new ConcurrentModel(), redirectAttributes, new MockHttpServletRequest());

            assertEquals("redirect:/otp-access/first/wrong-sms-link", view);
            verify(smsService).sendSms(eq("otp-user@example.org"), eq("0600000000"), eq("654321"));
            verify(otpService).setSmsSended("wrong-sms-link");
            assertNotNull(redirectAttributes.getFlashAttributes().get("message"));
        }

        @Test
        void otpEntryPointShouldRenderSigninPageWhenOidcAuthenticationIsAvailable() throws Exception {
            GlobalProperties globalProperties = new GlobalProperties();
            globalProperties.setSmsRequired(false);
            globalProperties.setNbSignOtpTries(3);
            OtpService otpService = mock(OtpService.class);
            SignBookService signBookService = mock(SignBookService.class);
            UserService userService = mock(UserService.class);
            OidcOtpSecurityService oidcService = mock(OidcOtpSecurityService.class);
            ClientRegistrationRepository clientRegistrationRepository = mock(ClientRegistrationRepository.class);

            when(oidcService.getCode()).thenReturn("oidc");
            when(clientRegistrationRepository.findByRegistrationId("oidc")).thenReturn(
                    ClientRegistration.withRegistrationId("oidc")
                            .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                            .clientId("client")
                            .authorizationUri("https://idp.example.org/auth")
                            .tokenUri("https://idp.example.org/token")
                            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                            .scope("openid")
                            .userInfoUri("https://idp.example.org/userinfo")
                            .userNameAttributeName("sub")
                            .clientName("oidc")
                            .build()
            );

            OtpAccessController controller = new OtpAccessController(
                    globalProperties,
                    otpService,
                    signBookService,
                    userService,
                    List.of(oidcService),
                    null,
                    clientRegistrationRepository,
                    new SmsProperties()
            );

            Otp otp = otp("oidc-link", false, 0, 54L);
            when(otpService.getAndCheckOtpFromDatabase("oidc-link")).thenReturn(otp);
            when(signBookService.getExternalAuths(eq(54L), anyList())).thenReturn(List.of());

            ConcurrentModel model = new ConcurrentModel();
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/otp-access/first/oidc-link");

            String view = controller.signin("oidc-link", model, request, new RedirectAttributesModelMap());

            assertEquals("otp/signin", view);
            assertEquals(Boolean.FALSE, model.getAttribute("smsRequired"));
            assertEquals(List.of(oidcService), model.getAttribute("securityServices"));
            var session = request.getSession(false);
            assertNotNull(session);
            assertNull(session.getAttribute(SPRING_SECURITY_CONTEXT_KEY));
        }

        @Test
        void otpAuthenticationShouldBypassSmsChallengeWhenSmsIsDisabledGlobally() {
            GlobalProperties globalProperties = new GlobalProperties();
            globalProperties.setSmsRequired(false);
            OtpService otpService = mock(OtpService.class);
            SignBookService signBookService = mock(SignBookService.class);
            UserService userService = mock(UserService.class);

            OtpAccessController controller = new OtpAccessController(
                    globalProperties,
                    otpService,
                    signBookService,
                    userService,
                    List.of(),
                    null,
                    null,
                    new SmsProperties()
            );

            Otp otp = otp("bypass-link", false, 0, 78L);
            when(otpService.getAndCheckOtpFromDatabase("bypass-link")).thenReturn(otp);

            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/otp-access");
            String view = controller.auth("bypass-link", "ignored", new ConcurrentModel(), new RedirectAttributesModelMap(), request);

            assertEquals("redirect:/otp/signrequests/signbook-redirect/78", view);
            assertOtpAuthenticationStoredInSession(request);
            verify(otpService).addOtpTry("bypass-link");
        }

        @Test
        void otpErrorShouldFallbackToSessionMessage() {
            OtpAccessController controller = new OtpAccessController(
                    new GlobalProperties(),
                    mock(OtpService.class),
                    mock(SignBookService.class),
                    mock(UserService.class),
                    List.of(),
                    null,
                    null,
                    new SmsProperties()
            );

            MockHttpServletRequest request = new MockHttpServletRequest();
            var session = request.getSession(true);
            assertNotNull(session);
            session.setAttribute("errorMsg", "otp-session-error");
            ConcurrentModel model = new ConcurrentModel();

            String view = controller.error(request, model);

            assertEquals("otp/error", view);
            assertEquals("otp-session-error", model.getAttribute("errorMsg"));
        }

        @Test
        void otpResendShouldExposeRenewedOtpInModel() {
            SignBookService signBookService = mock(SignBookService.class);
            OtpAccessController controller = new OtpAccessController(
                    new GlobalProperties(),
                    mock(OtpService.class),
                    signBookService,
                    mock(UserService.class),
                    List.of(),
                    null,
                    null,
                    new SmsProperties()
            );

            Otp renewedOtp = otp("resend-link", false, 1, 79L);
            when(signBookService.renewOtp("resend-link")).thenReturn(renewedOtp);

            ConcurrentModel model = new ConcurrentModel();
            String view = controller.resend("resend-link", model);

            assertEquals("otp/resend", view);
            assertSame(renewedOtp, model.getAttribute("otp"));
        }

        @Test
        void otpOauth2ErrorShouldMapKnownProviderErrorsToFriendlyMessage() {
            OtpAccessController controller = new OtpAccessController(
                    new GlobalProperties(),
                    mock(OtpService.class),
                    mock(SignBookService.class),
                    mock(UserService.class),
                    List.of(),
                    null,
                    null,
                    new SmsProperties()
            );

            ConcurrentModel model = new ConcurrentModel();
            String view = controller.oauth2Error("access_denied", "provider denied", null, "state-123", model);

            assertEquals("otp/oauth2-error", view);
            assertEquals("access_denied", model.getAttribute("oauth2Error"));
            assertEquals("provider denied", model.getAttribute("oauth2ErrorDescription"));
            assertEquals("state-123", model.getAttribute("oauth2State"));
            Object errorMessage = model.getAttribute("errorMessage");
            assertNotNull(errorMessage);
            assertFalse(errorMessage.toString().isBlank());
            assertNotEquals("provider denied", errorMessage.toString());
        }

        @Test
        void otpPhoneShouldRejectAlreadyAssignedNumber() throws Exception {
            GlobalProperties globalProperties = new GlobalProperties();
            SmsProperties smsProperties = new SmsProperties();
            smsProperties.setServiceName("EMAIL");
            OtpService otpService = mock(OtpService.class);
            SignBookService signBookService = mock(SignBookService.class);
            UserService userService = mock(UserService.class);

            OtpAccessController controller = new OtpAccessController(
                    globalProperties,
                    otpService,
                    signBookService,
                    userService,
                    List.of(),
                    mock(SmsService.class),
                    null,
                    smsProperties
            );

            Otp otp = otp("phone-link", false, 0, 80L);
            User anotherUser = new User();
            anotherUser.setEppn("other@example.org");
            when(otpService.getOtpFromDatabase("phone-link")).thenReturn(otp);
            when(userService.getUserByPhone("0601010101")).thenReturn(anotherUser);

            ResponseEntity<?> response = controller.phone("phone-link", "0601010101");

            assertEquals(500, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().toString().contains("Numéro de mobile déjà attribué"));
        }
    }

    @Nested
    class PublicControllerTests {

        @Test
        void publicControlEndpointShouldRevealAuditTrailAndLogsWithoutAuthenticationWhenTokenIsKnown() throws Exception {
            LogService logService = mock(LogService.class);
            SignRequestService signRequestService = mock(SignRequestService.class);
            AuditTrailService auditTrailService = mock(AuditTrailService.class);
            FileService fileService = mock(FileService.class);
            UserService userService = mock(UserService.class);
            var xsltService = mock(org.esupportail.esupsignature.dss.service.XSLTService.class);
            PreAuthorizeService preAuthorizeService = mock(PreAuthorizeService.class);
            ValidationService validationService = mock(ValidationService.class);
            SignService signService = mock(SignService.class);
            MobileSignTokenService mobileSignTokenService = mock(MobileSignTokenService.class);

            PublicController controller = publicController(logService, signRequestService, auditTrailService, fileService, userService, xsltService, preAuthorizeService, validationService, signService, mobileSignTokenService);

            AuditTrail auditTrail = new AuditTrail();
            auditTrail.setToken("known-public-token");
            auditTrail.setDocumentSize(1234L);

            List<Log> logs = List.of(mock(Log.class));
            when(auditTrailService.getAuditTrailByToken("known-public-token")).thenReturn(auditTrail);
            when(signRequestService.getSignRequestByToken("known-public-token")).thenReturn(Optional.empty());
            when(logService.getFullByToken("known-public-token")).thenReturn(logs);

            ConcurrentModel model = new ConcurrentModel();
            String view = controller.controlToken("known-public-token", model);

            assertEquals("public/control", view);
            assertSame(auditTrail, model.getAttribute("auditTrail"));
            assertSame(logs, model.getAttribute("logs"));
            assertEquals(Boolean.FALSE, model.getAttribute("auditTrailChecked"));
        }

        @Test
        void publicControlEndpointShouldReturnErrorWhenTokenIsUnknown() throws Exception {
            LogService logService = mock(LogService.class);
            SignRequestService signRequestService = mock(SignRequestService.class);
            AuditTrailService auditTrailService = mock(AuditTrailService.class);
            FileService fileService = mock(FileService.class);
            UserService userService = mock(UserService.class);
            var xsltService = mock(org.esupportail.esupsignature.dss.service.XSLTService.class);
            PreAuthorizeService preAuthorizeService = mock(PreAuthorizeService.class);
            ValidationService validationService = mock(ValidationService.class);
            SignService signService = mock(SignService.class);
            MobileSignTokenService mobileSignTokenService = mock(MobileSignTokenService.class);

            PublicController controller = publicController(logService, signRequestService, auditTrailService, fileService, userService, xsltService, preAuthorizeService, validationService, signService, mobileSignTokenService);

            when(auditTrailService.getAuditTrailByToken("missing-token")).thenReturn(null);

            String view = controller.controlToken("missing-token", new ConcurrentModel());

            assertEquals("error", view);
            verify(logService, never()).getFullByToken("missing-token");
        }

        @Test
        void publicControlEndpointShouldExposeViewAccessForAuthenticatedKnownUser() throws Exception {
            LogService logService = mock(LogService.class);
            SignRequestService signRequestService = mock(SignRequestService.class);
            AuditTrailService auditTrailService = mock(AuditTrailService.class);
            FileService fileService = mock(FileService.class);
            UserService userService = mock(UserService.class);
            var xsltService = mock(org.esupportail.esupsignature.dss.service.XSLTService.class);
            PreAuthorizeService preAuthorizeService = mock(PreAuthorizeService.class);
            ValidationService validationService = mock(ValidationService.class);
            SignService signService = mock(SignService.class);

            MobileSignTokenService mobileSignTokenService = mock(MobileSignTokenService.class);

            PublicController controller = publicController(logService, signRequestService, auditTrailService, fileService, userService, xsltService, preAuthorizeService, validationService, signService, mobileSignTokenService);

            AuditTrail auditTrail = new AuditTrail();
            auditTrail.setToken("known-token");

            SignRequest signRequest = new SignRequest();
            signRequest.setId(123L);

            User knownUser = new User();
            knownUser.setEppn("user@example.org");

            when(auditTrailService.getAuditTrailByToken("known-token")).thenReturn(auditTrail);
            when(signRequestService.getSignRequestByToken("known-token")).thenReturn(Optional.of(signRequest));
            when(logService.getFullByToken("known-token")).thenReturn(List.of());
            when(userService.tryGetEppnFromLdap(any())).thenReturn("user@example.org");
            when(userService.getByEppn("user@example.org")).thenReturn(knownUser);
            when(preAuthorizeService.checkUserViewRights(123L, "user@example.org", "user@example.org")).thenReturn(true);

            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user@example.org", "n/a"));
            ConcurrentModel model = new ConcurrentModel();

            String view = controller.controlToken("known-token", model);

            assertEquals("public/control", view);
            assertEquals(Boolean.TRUE, model.getAttribute("auditTrailChecked"));
            assertEquals(Boolean.TRUE, model.getAttribute("viewAccess"));
        }

        @Test
        void publicCheckFileShouldMarkErrorWhenTokenIsUnknown() throws Exception {
            LogService logService = mock(LogService.class);
            SignRequestService signRequestService = mock(SignRequestService.class);
            AuditTrailService auditTrailService = mock(AuditTrailService.class);
            FileService fileService = mock(FileService.class);
            UserService userService = mock(UserService.class);
            var xsltService = mock(org.esupportail.esupsignature.dss.service.XSLTService.class);
            PreAuthorizeService preAuthorizeService = mock(PreAuthorizeService.class);
            ValidationService validationService = mock(ValidationService.class);
            SignService signService = mock(SignService.class);

            MobileSignTokenService mobileSignTokenService = mock(MobileSignTokenService.class);

            PublicController controller = publicController(logService, signRequestService, auditTrailService, fileService, userService, xsltService, preAuthorizeService, validationService, signService, mobileSignTokenService);

            MockMultipartFile multipartFile = new MockMultipartFile("multipartFile", "test.pdf", "application/pdf", "payload".getBytes());
            when(fileService.getFileChecksum(multipartFile.getInputStream())).thenReturn("checksum-1");
            when(auditTrailService.getAuditTrailFromCheksum("checksum-1")).thenReturn(null);

            ConcurrentModel model = new ConcurrentModel();
            String view = controller.checkFile("known-token", multipartFile, model, new MockHttpSession());

            assertEquals("public/control", view);
            assertEquals(Boolean.TRUE, model.getAttribute("error"));
            verify(validationService, never()).validate(any(), eq(null));
        }

        @Test
        void publicCheckFileShouldValidateAnonymousUploadWhenTokenIsNullString() throws Exception {
            LogService logService = mock(LogService.class);
            SignRequestService signRequestService = mock(SignRequestService.class);
            AuditTrailService auditTrailService = mock(AuditTrailService.class);
            FileService fileService = mock(FileService.class);
            UserService userService = mock(UserService.class);
            var xsltService = mock(org.esupportail.esupsignature.dss.service.XSLTService.class);
            PreAuthorizeService preAuthorizeService = mock(PreAuthorizeService.class);
            ValidationService validationService = mock(ValidationService.class);
            SignService signService = mock(SignService.class);

            MobileSignTokenService mobileSignTokenService = mock(MobileSignTokenService.class);

            PublicController controller = publicController(logService, signRequestService, auditTrailService, fileService, userService, xsltService, preAuthorizeService, validationService, signService, mobileSignTokenService);

            MockMultipartFile multipartFile = new MockMultipartFile("multipartFile", "test.pdf", "application/pdf", "payload".getBytes());
            when(fileService.getFileChecksum(multipartFile.getInputStream())).thenReturn("checksum-2");
            when(auditTrailService.getAuditTrailFromCheksum("checksum-2")).thenReturn(null);

            ConcurrentModel model = new ConcurrentModel();
            String view = controller.checkFile("null", multipartFile, model, new MockHttpSession());

            assertEquals("public/control", view);
            assertEquals(Boolean.TRUE, model.getAttribute("error"));
            verify(validationService).validate(any(), eq(null));
        }

        @Test
        void mobileSignStatusShouldExposeUsedStateSeparatelyFromExpiration() {
            LogService logService = mock(LogService.class);
            SignRequestService signRequestService = mock(SignRequestService.class);
            AuditTrailService auditTrailService = mock(AuditTrailService.class);
            FileService fileService = mock(FileService.class);
            UserService userService = mock(UserService.class);
            var xsltService = mock(org.esupportail.esupsignature.dss.service.XSLTService.class);
            PreAuthorizeService preAuthorizeService = mock(PreAuthorizeService.class);
            ValidationService validationService = mock(ValidationService.class);
            SignService signService = mock(SignService.class);
            MobileSignTokenService mobileSignTokenService = mock(MobileSignTokenService.class);

            PublicController controller = publicController(logService, signRequestService, auditTrailService, fileService, userService, xsltService, preAuthorizeService, validationService, signService, mobileSignTokenService);

            when(mobileSignTokenService.tokenExists("mobile-token")).thenReturn(true);
            when(mobileSignTokenService.validateToken("mobile-token")).thenReturn(false);
            when(mobileSignTokenService.isTokenUsed("mobile-token")).thenReturn(true);
            when(mobileSignTokenService.isTokenExpired("mobile-token")).thenReturn(false);
            when(mobileSignTokenService.getTokenExpirationDate("mobile-token")).thenReturn(new java.util.Date(123L));

            ResponseEntity<Map<String, Object>> response = controller.checkTokenStatus("mobile-token");

            assertEquals(200, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals(Boolean.TRUE, response.getBody().get("exists"));
            assertEquals(Boolean.FALSE, response.getBody().get("valid"));
            assertEquals(Boolean.TRUE, response.getBody().get("used"));
            assertEquals(Boolean.FALSE, response.getBody().get("expired"));
            assertEquals(123L, response.getBody().get("expiresAtEpochMillis"));
            assertEquals("Signature enregistrée", response.getBody().get("message"));
        }

        @Test
        void mobileSignStatusShouldExposePendingPreviewAvailability() {
            LogService logService = mock(LogService.class);
            SignRequestService signRequestService = mock(SignRequestService.class);
            AuditTrailService auditTrailService = mock(AuditTrailService.class);
            FileService fileService = mock(FileService.class);
            UserService userService = mock(UserService.class);
            var xsltService = mock(org.esupportail.esupsignature.dss.service.XSLTService.class);
            PreAuthorizeService preAuthorizeService = mock(PreAuthorizeService.class);
            ValidationService validationService = mock(ValidationService.class);
            SignService signService = mock(SignService.class);
            MobileSignTokenService mobileSignTokenService = mock(MobileSignTokenService.class);

            PublicController controller = publicController(logService, signRequestService, auditTrailService, fileService, userService, xsltService, preAuthorizeService, validationService, signService, mobileSignTokenService);

            when(mobileSignTokenService.tokenExists("mobile-token")).thenReturn(true);
            when(mobileSignTokenService.validateToken("mobile-token")).thenReturn(true);
            when(mobileSignTokenService.isTokenUsed("mobile-token")).thenReturn(false);
            when(mobileSignTokenService.isTokenExpired("mobile-token")).thenReturn(false);
            when(mobileSignTokenService.hasPendingSignaturePreview("mobile-token")).thenReturn(true);

            ResponseEntity<Map<String, Object>> response = controller.checkTokenStatus("mobile-token");

            assertEquals(200, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals(Boolean.TRUE, response.getBody().get("previewAvailable"));
        }

        @Test
        void mobileSignPreviewShouldExposePendingImageForValidToken() {
            LogService logService = mock(LogService.class);
            SignRequestService signRequestService = mock(SignRequestService.class);
            AuditTrailService auditTrailService = mock(AuditTrailService.class);
            FileService fileService = mock(FileService.class);
            UserService userService = mock(UserService.class);
            var xsltService = mock(org.esupportail.esupsignature.dss.service.XSLTService.class);
            PreAuthorizeService preAuthorizeService = mock(PreAuthorizeService.class);
            ValidationService validationService = mock(ValidationService.class);
            SignService signService = mock(SignService.class);
            MobileSignTokenService mobileSignTokenService = mock(MobileSignTokenService.class);

            PublicController controller = publicController(logService, signRequestService, auditTrailService, fileService, userService, xsltService, preAuthorizeService, validationService, signService, mobileSignTokenService);

            when(mobileSignTokenService.getPendingSignaturePreview("mobile-token")).thenReturn("data:image/png;base64,abc123");

            ResponseEntity<Map<String, Object>> response = controller.getSignaturePreview("mobile-token");

            assertEquals(200, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals(Boolean.TRUE, response.getBody().get("success"));
            assertEquals("data:image/png;base64,abc123", response.getBody().get("signImageBase64"));
        }

        @Test
        void mobileSignPageShouldExposeUsedStateWithoutFlaggingExpiration() {
            LogService logService = mock(LogService.class);
            SignRequestService signRequestService = mock(SignRequestService.class);
            AuditTrailService auditTrailService = mock(AuditTrailService.class);
            FileService fileService = mock(FileService.class);
            UserService userService = mock(UserService.class);
            var xsltService = mock(org.esupportail.esupsignature.dss.service.XSLTService.class);
            PreAuthorizeService preAuthorizeService = mock(PreAuthorizeService.class);
            ValidationService validationService = mock(ValidationService.class);
            SignService signService = mock(SignService.class);
            MobileSignTokenService mobileSignTokenService = mock(MobileSignTokenService.class);

            PublicController controller = publicController(logService, signRequestService, auditTrailService, fileService, userService, xsltService, preAuthorizeService, validationService, signService, mobileSignTokenService);

            when(mobileSignTokenService.validateToken("mobile-token")).thenReturn(false);
            when(mobileSignTokenService.isTokenUsed("mobile-token")).thenReturn(true);
            when(mobileSignTokenService.getTokenExpirationDate("mobile-token")).thenReturn(new java.util.Date(456L));

            ConcurrentModel model = new ConcurrentModel();
            String view = controller.showMobileSignPage("mobile-token", model);

            assertEquals("public/mobile-sign", view);
            assertEquals("mobile-token", model.getAttribute("token"));
            assertEquals(Boolean.TRUE, model.getAttribute("used"));
            assertEquals(Boolean.FALSE, model.getAttribute("expired"));
            assertEquals(456L, model.getAttribute("expiresAtEpochMillis"));
        }
    }

    @Nested
    class WebSecurityPropertiesTests {

        @Test
        void actuatorIpAllowlistShouldImplicitlyReuseWsAllowlistWhenUnset() {
            WebSecurityProperties webSecurityProperties = new WebSecurityProperties();
            String[] wsAllowlist = new String[] {"10.0.0.0/8", "192.168.0.0/16"};

            webSecurityProperties.setWsAccessAuthorizeIps(wsAllowlist);

            assertSame(wsAllowlist, webSecurityProperties.getActuatorsAccessAuthorizeIps());

            webSecurityProperties.setActuatorsAccessAuthorizeIps(new String[0]);

            assertSame(wsAllowlist, webSecurityProperties.getActuatorsAccessAuthorizeIps());
        }

        @Test
        void actuatorIpAllowlistShouldPreferExplicitAllowlistWhenProvided() {
            WebSecurityProperties webSecurityProperties = new WebSecurityProperties();
            String[] wsAllowlist = new String[] {"10.0.0.0/8"};
            String[] actuatorAllowlist = new String[] {"127.0.0.1"};

            webSecurityProperties.setWsAccessAuthorizeIps(wsAllowlist);
            webSecurityProperties.setActuatorsAccessAuthorizeIps(actuatorAllowlist);

            assertSame(actuatorAllowlist, webSecurityProperties.getActuatorsAccessAuthorizeIps());
        }
    }

    @Nested
    class BearerTokenResolverTests {

        @Test
        void bearerTokenResolverShouldReadAuthorizationHeaderFirst() {
            BearerTokenResolver resolver = webSecurityConfig().bearerTokenResolver();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer header-token");

            assertEquals("header-token", resolver.resolve(request));
        }

        @Test
        void bearerTokenResolverShouldFallbackToJwtCookie() {
            BearerTokenResolver resolver = webSecurityConfig().bearerTokenResolver();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie("jwt", "cookie-token"));

            assertEquals("cookie-token", resolver.resolve(request));
        }

        @Test
        void bearerTokenResolverShouldPreferHeaderOverJwtCookie() {
            BearerTokenResolver resolver = webSecurityConfig().bearerTokenResolver();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer header-token");
            request.setCookies(new Cookie("jwt", "cookie-token"));

            assertEquals("header-token", resolver.resolve(request));
        }

        @Test
        void bearerTokenResolverShouldReturnNullWhenNoTokenCanBeFound() {
            BearerTokenResolver resolver = webSecurityConfig().bearerTokenResolver();

            assertNull(resolver.resolve(new MockHttpServletRequest()));
        }

        @Test
        void bearerTokenResolverShouldIgnoreNonBearerAuthorizationHeader() {
            BearerTokenResolver resolver = webSecurityConfig().bearerTokenResolver();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

            assertNull(resolver.resolve(request));
        }
    }

    @Nested
    class WsControllerAdviceTests {

        @Test
        void wsControllerAdviceShouldExtractBearerTokenFromAuthorizationHeader() {
            WsControllerAdvice advice = new WsControllerAdvice(null);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer api-token");

            assertEquals("api-token", advice.getXApiKey(request));
        }

        @Test
        void wsControllerAdviceShouldExtractApiKeyFromCustomHeader() {
            WsControllerAdvice advice = new WsControllerAdvice(null);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("x-apikey", "api-token");

            assertEquals("api-token", advice.getXApiKey(request));
        }

        @Test
        void wsControllerAdviceShouldFallbackToRequestParameter() {
            WsControllerAdvice advice = new WsControllerAdvice(null);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setParameter("XApiKey", "query-token");

            assertEquals("query-token", advice.getXApiKey(request));
        }

        @Test
        void wsControllerAdviceShouldReturnRawAuthorizationHeaderWhenItIsNotBearer() {
            WsControllerAdvice advice = new WsControllerAdvice(null);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

            assertEquals("Basic dXNlcjpwYXNz", advice.getXApiKey(request));
        }
    }

    @Nested
    class ConfigurationExposureTests {

        @Test
        void productionConfigurationShouldKeepSensitiveDefaultsExplicit() {
            YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
            // Allow overriding which application.yml to check by providing -Dspring.config.location
            String springConfigLocation = System.getProperty("spring.config.location");
            Resource configResource = null;
            if (springConfigLocation != null && !springConfigLocation.isBlank()) {
                // support comma-separated locations, take first
                String first = springConfigLocation.split(",")[0].trim();
                // if directory provided, append application.yml
                if (first.endsWith(File.separator) || first.endsWith("/")) {
                    first = first + "application.yml";
                }
                // remove optional file: prefix for FileSystemResource
                if (first.startsWith("file:")) {
                    first = first.substring("file:".length());
                }
                configResource = new FileSystemResource(first);
                if (!configResource.exists()) {
                    // fallback to classpath resource if external file not found
                    configResource = new ClassPathResource("application.yml");
                }
            } else {
                configResource = new ClassPathResource("application.yml");
            }
            // debug: afficher la ressource réellement utilisée pour résoudre quel fichier est chargé
            try {
                String desc = configResource.getDescription();
                String path = null;
                try {
                    File f = configResource.getFile();
                    if (f != null) path = f.getAbsolutePath();
                } catch (Exception ignore) {
                    // resource may not expose a File
                }
                System.out.println("[test] Using config resource: " + desc + (path != null ? (" -> " + path) : ""));
            } catch (Exception e) {
                System.out.println("[test] Using config resource: " + configResource);
            }
            yaml.setResources(configResource);
            var properties = yaml.getObject();

            assertNotNull(properties, "Impossible de charger les propriétés : le fichier 'application.yml' est introuvable ou mal formé.");
            assertEquals("*", properties.getProperty("management.endpoints.web.exposure.include"),
                    "La propriété 'management.endpoints.web.exposure.include' doit être '*' en production pour expliciter l'exposition des endpoints.");
            assertTrue("ALWAYS".equalsIgnoreCase(properties.getProperty("management.endpoint.health.show-details")),
                    "La propriété 'management.endpoint.health.show-details' doit valoir 'ALWAYS' (insensible à la casse) en production (vérifier la configuration health).");
            String wsAccessAuthorizeIps = properties.getProperty("security.web.ws-access-authorize-ips", "");
            if (!wsAccessAuthorizeIps.isBlank()) {
                System.err.println("[warning] La propriété 'security.web.ws-access-authorize-ips' n'est pas vide en production: " + wsAccessAuthorizeIps);
            }
            assertTrue(properties.getProperty("security.web.actuators-access-authorize-ips", "").isBlank(),
                    "La propriété 'security.web.actuators-access-authorize-ips' doit être vide en production. Une valeur non vide restreint l'accès aux actuators mais peut indiquer une configuration non souhaitée pour cet environnement de test.");
        }

        @Test
        void testConfigurationShouldRestrictWsEndpointsToSpecificIpAllowlist() {
            YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
            yaml.setResources(new ClassPathResource("application-test.yml"));
            var properties = yaml.getObject();
            assertNotNull(properties);
            assertRestrictedIpAllowlist(properties, "security.web.ws-access-authorize-ips");
            assertNull(properties.getProperty("security.web.actuators-access-authorize-ips"));
            assertTrue("always".equalsIgnoreCase(properties.getProperty("management.endpoint.health.show-details")),
                    "La propriété 'management.endpoint.health.show-details' devrait être 'always' (insensible à la casse) en configuration de test.");
            assertTrue("always".equalsIgnoreCase(properties.getProperty("server.error.include-stacktrace")),
                    "La propriété 'server.error.include-stacktrace' devrait être 'always' (insensible à la casse) en configuration de test.");
        }
    }

    private void assertRestrictedIpAllowlist(Properties properties, String propertyPrefix) {
        assertNotNull(properties);
        List<String> values = new ArrayList<>();
        String directValue = properties.getProperty(propertyPrefix);
        if (directValue != null) {
            values.addAll(java.util.Arrays.stream(directValue.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .toList());
        }
        for (int i = 0; ; i++) {
            String indexedValue = properties.getProperty(propertyPrefix + "[" + i + "]");
            if (indexedValue == null) {
                break;
            }
            if (!indexedValue.isBlank()) {
                values.add(indexedValue.trim());
            }
        }
        assertFalse(values.isEmpty());
        assertTrue(values.stream().noneMatch("*"::equals));
        assertTrue(values.stream().noneMatch("0.0.0.0/0"::equals));
    }

    private WsAccessTokenService wsAccessTokenService(WsAccessTokenRepository repository) {
        SignRequestService signRequestService = mock(SignRequestService.class);
        var workflowService = mock(org.esupportail.esupsignature.service.WorkflowService.class);
        UserService userService = mock(UserService.class);
        return new WsAccessTokenService(repository, signRequestService, workflowService, userService);
    }

    private PublicController publicController(LogService logService, SignRequestService signRequestService, AuditTrailService auditTrailService,
                                              FileService fileService, UserService userService,
                                              org.esupportail.esupsignature.dss.service.XSLTService xsltService,
                                              PreAuthorizeService preAuthorizeService, ValidationService validationService,
                                              SignService signService, MobileSignTokenService mobileSignTokenService) {
        return new PublicController(
                null,
                logService,
                signRequestService,
                auditTrailService,
                fileService,
                userService,
                xsltService,
                preAuthorizeService,
                validationService,
                mobileSignTokenService
        );
    }

    private Otp otp(String urlId, boolean forceSms, int tries, long signBookId) {
        User otpUser = new User();
        otpUser.setEppn("otp-user@example.org");
        otpUser.setEmail("otp-user@example.org");
        otpUser.setPhone("0600000000");

        SignBook signBook = new SignBook();
        signBook.setId(signBookId);
        signBook.setStatus(SignRequestStatus.pending);

        Otp otp = new Otp();
        otp.setUrlId(urlId);
        otp.setUser(otpUser);
        otp.setSignBook(signBook);
        otp.setSignature(true);
        otp.setForceSms(forceSms);
        otp.setTries(tries);
        return otp;
    }

    private void assertOtpAuthenticationStoredInSession(MockHttpServletRequest request) {
        var session = request.getSession(false);
        assertNotNull(session);
        SecurityContext sessionContext = (SecurityContext) session.getAttribute(SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(sessionContext);
        assertNotNull(sessionContext.getAuthentication());
        assertEquals("otp-user@example.org", sessionContext.getAuthentication().getName());
        assertTrue(sessionContext.getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority("ROLE_OTP")));
    }

    private WebSecurityConfig webSecurityConfig() {
        return new WebSecurityConfig(
                new GlobalProperties(),
                mock(OAuthAuthenticationSuccessHandler.class),
                new WebSecurityProperties(),
                null,
                List.of(),
                mock(RegisterSessionAuthenticationStrategy.class),
                mock(SessionRegistryImpl.class),
                mock(LogoutHandlerImpl.class),
                mock(CasJwtDecoder.class)
        );
    }
}

