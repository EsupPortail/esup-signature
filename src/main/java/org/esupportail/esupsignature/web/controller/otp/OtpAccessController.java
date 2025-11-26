package org.esupportail.esupsignature.web.controller.otp;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.BooleanUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.entity.Otp;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.interfaces.sms.SmsService;
import org.esupportail.esupsignature.service.security.OidcOtpSecurityService;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

@RequestMapping("/otp-access")
@Controller
public class OtpAccessController {

    private static final Logger logger = LoggerFactory.getLogger(OtpAccessController.class);

    private static final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    private final GlobalProperties globalProperties;
    private final OtpService otpService;
    private final SignBookService signBookService;
    private final UserService userService;
    private final List<SecurityService> securityServices;
    private final SmsService smsService;

    public OtpAccessController(GlobalProperties globalProperties, OtpService otpService, SignBookService signBookService, UserService userService, List<SecurityService> securityServices, @Autowired(required = false) SmsService smsService) {
        this.globalProperties = globalProperties;
        this.otpService = otpService;
        this.signBookService = signBookService;
        this.userService = userService;
        this.securityServices = securityServices;
        this.smsService = smsService;
    }

    @GetMapping(value = "/first/{urlId}")
    public String signin(@PathVariable String urlId, Model model, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes) throws NumberParseException {
        model.addAttribute("urlId", urlId);
        List<OidcOtpSecurityService> oidcOtpSecurityServices = securityServices.stream().filter(s -> (s instanceof OidcOtpSecurityService)).map(s -> (OidcOtpSecurityService) s).toList();
        Otp otp = otpService.getAndCheckOtpFromDatabase(urlId);
        if(otp != null) {
            if (!globalProperties.getSmsRequired() && !otp.isForceSms() && oidcOtpSecurityServices.isEmpty()) {
                authOtp(model, httpServletRequest, otp.getUser());
                return "redirect:/otp/signrequests/signbook-redirect/" + otp.getSignBook().getId();
            }
            if(!otp.getSignBook().getStatus().equals(SignRequestStatus.pending) && otp.isSignature()) {
                return "redirect:/otp-access/completed";
            }
            model.addAttribute("otp", otp);
            model.addAttribute("smsService", smsService);
            model.addAttribute("smsRequired", (globalProperties.getSmsRequired() || otp.isForceSms()));
            model.addAttribute("externalAuths", signBookService.getExternalAuths(otp.getSignBook().getId(), oidcOtpSecurityServices));
            httpServletRequest.getSession().setAttribute("after_oauth_redirect", "/otp/signrequests/signbook-redirect/" + otp.getSignBook().getId());
            model.addAttribute("securityServices", oidcOtpSecurityServices);
            model.addAttribute("globalProperties", globalProperties);
            return "otp/signin";
        }
        if(signBookService.renewOtp(urlId, true)) {
            return "redirect:/otp-access/expired";
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", """
                    <h2>Lien de signature erroné</h2>
                    <p>Le lien que vous utilisez n’existe plus, merci de contacter le créateur de la demande (son nom est présent dans le premier mail que vous avez reçu).</p>
                    <p>Si cette demande a déjà été signée, vous devriez avoir reçu un email de téléchargement.</p>
                    """);
            return "redirect:/otp-access/error";
        }
    }

    @GetMapping(value = "/completed")
    public String completed() {
        return "otp/completed";
    }

    @GetMapping(value = "/transfered")
    public String transfered() {
        return "otp/transfered";
    }

    @GetMapping(value = "/expired")
    public String expired() {
        return "otp/expired";
    }

    @GetMapping(value = "/error")
    public String error(HttpServletRequest httpServletRequest, Model model) {
        if(!StringUtils.hasText((String) model.getAttribute("errorMsg"))) {
            model.addAttribute("errorMsg", httpServletRequest.getSession().getAttribute("errorMsg"));
        }
        return "otp/error";
    }

    @PostMapping(value = "/phone")
    @ResponseBody
    public ResponseEntity<?> phone(@RequestParam String urlId, @RequestParam String phone) throws EsupSignatureUserException, NumberParseException {
        Otp otp = otpService.getOtpFromDatabase(urlId);
        if(otp != null) {
            User user = otp.getUser();
            User userTest = userService.getUserByPhone(phone);
            if (userTest == null || user.getEppn().equals(userTest.getEppn())) {
                Phonenumber.PhoneNumber number;
                try {
                    number = phoneUtil.parse(phone, null);
                } catch (Exception e) {
                    return ResponseEntity.internalServerError().body("Merci de saisir correctement votre numéro de mobile");
                }
                if ((!otp.getSmsSended() || otpService.getOtpFromCache(urlId) == null) && smsService != null) {
                    if (phoneUtil.isValidNumber(number)) {
                        String password = otpService.generateOtpPassword(urlId, phone);
                        logger.info("sending password by sms : " + password + " to " + phone);
                        try {
                            smsService.sendSms(phone, "Votre code de connexion esup_signature " + password);
                            otpService.setSmsSended(urlId);
                            return ResponseEntity.ok().build();
                        } catch (EsupSignatureRuntimeException e) {
                            logger.error(e.getMessage(), e);
                            return ResponseEntity.internalServerError().body(e.getMessage());
                        }
                    }
                } else {
                    return ResponseEntity.ok().body("Merci d'utiliser le code du dernier SMS reçu.");
                }
            } else {
                return ResponseEntity.internalServerError().body("Numéro de mobile déjà attribué, merci de prendre contact avec l'émetteur via le mail ci-dessus");
            }
        }
        return ResponseEntity.internalServerError().body("Une erreur c'est produite, merci de prendre contact avec l'émetteur via le mail ci-dessus");
    }

    @PostMapping
    public String auth(@RequestParam String urlId, @RequestParam String password, Model model, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) throws EsupSignatureUserException {
        Otp otp = otpService.getAndCheckOtpFromDatabase(urlId);
        if (!globalProperties.getSmsRequired() && !otp.isForceSms()) {
            authOtp(model, httpServletRequest, otp.getUser());
            return "redirect:/otp/signrequests/signbook-redirect/" + otp.getSignBook().getId();
        }
        Boolean testOtp = otpService.checkOtp(urlId, password);
        if(BooleanUtils.isTrue(testOtp)) {
            otp.setSmsSended(true);
            logger.info("otp success for : " + urlId);
            User user = otp.getUser();
            if(StringUtils.hasText(otp.getPhoneNumber())) {
                userService.updatePhone(user.getEppn(), otp.getPhoneNumber());
            }
            authOtp(model, httpServletRequest, user);
            return "redirect:/otp/signrequests/signbook-redirect/" + otp.getSignBook().getId();
        } else {
            String newPassword = otpService.generateOtpPassword(urlId, otp.getPhoneNumber());
            logger.info("sending password by sms : " + newPassword + " to " + otp.getPhoneNumber());
            try {
                smsService.sendSms(otp.getPhoneNumber(), "Votre code de connexion esup_signature " + newPassword);
                otpService.setSmsSended(urlId);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Mauvais code SMS, un nouveau code vous à été envoyé"));
            return "redirect:/otp-access/first/" + urlId;
        }
    }

    private void authOtp(Model model, HttpServletRequest httpServletRequest, User user) {
        httpServletRequest.getSession().setAttribute("securityServiceName", "sms");
        List<SimpleGrantedAuthority> simpleGrantedAuthorities = new ArrayList<>();
        simpleGrantedAuthorities.add(new SimpleGrantedAuthority("ROLE_OTP"));
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(user.getEppn(), user.getPhone(), simpleGrantedAuthorities);
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(usernamePasswordAuthenticationToken);
        userService.updateRoles(user.getEppn(), usernamePasswordAuthenticationToken.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
        HttpSession httpSession = httpServletRequest.getSession(true);
        httpSession.setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        model.addAttribute("user", user);
        model.addAttribute("authUser", user);
    }

}
