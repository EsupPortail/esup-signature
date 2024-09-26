package org.esupportail.esupsignature.web.otp;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.entity.Otp;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.interfaces.sms.SmsService;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

@RequestMapping("/otp-access")
@Controller

public class OtpAccessController {

    private static final Logger logger = LoggerFactory.getLogger(OtpAccessController.class);

    @Resource
    private GlobalProperties globalProperties;

    @Resource
    private OtpService otpService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private UserService userService;

    @Resource
    AuthenticationManager authenticationManager;

    private final SmsService smsService;

    public OtpAccessController(@Autowired(required = false) SmsService smsService) {
        this.smsService = smsService;
    }

    @GetMapping(value = "/first/{urlId}")
    public String signin(@PathVariable String urlId, Model model, HttpServletRequest httpServletRequest) throws NumberParseException {
        model.addAttribute("urlId", urlId);
        Otp otp = otpService.getOtpFromDatabase(urlId);
        if(otp != null) {
            User user = otp.getUser();
            if(globalProperties.getSmsRequired() || otp.isForceSms()) {
                if (!otp.getSmsSended() && smsService != null) {
                    if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                        Phonenumber.PhoneNumber number = PhoneNumberUtil.getInstance().parse(user.getPhone(), "FR");
                        if (PhoneNumberUtil.getInstance().isValidNumber(number)) {
                            String password = otpService.generateOtpPassword(urlId);
                            logger.info("sending password by sms : " + password + " to " + otp.getPhoneNumber());
                            try {
                                smsService.sendSms(user.getPhone(), "Votre code de connexion esup_signature " + password);
                            } catch (EsupSignatureRuntimeException e) {
                                logger.error(e.getMessage(), e);
                            }
                            otp.setSmsSended(true);
                            return "otp/signin";
                        }
                    }
                    return "otp/enter-phonenumber";
                }
            } else if(!globalProperties.getSmsRequired() && !otp.isForceSms()) {
                Otp cachedOtp =  otpService.getAndCheckOtpFromCache(urlId);
                if(cachedOtp != null && urlId.equals(cachedOtp.getUrlId())) {
                    authOtp(model, httpServletRequest, user);
                    return "redirect:/otp/signrequests/signbook-redirect/" + otp.getSignBook().getId();
                }
            }
        }
        if(signBookService.renewOtp(urlId)) {
            return "redirect:/otp-access/expired";
        } else {
            return "redirect:/otp-access/error";
        }
    }

    @GetMapping(value = "/expired")
    public String expired() {
        return "otp/expired";
    }

    @GetMapping(value = "/error")
    public String error() {
        return "otp/error";
    }

    @PostMapping(value = "/phone")
    public String phone(@RequestParam String urlId, @RequestParam String phone, Model model, RedirectAttributes redirectAttributes) throws EsupSignatureUserException {
        model.addAttribute("urlId", urlId);
        Otp otp = otpService.getOtpFromDatabase(urlId);
        if(otp != null) {
            User user = otp.getUser();
            User userTest = userService.getUserByPhone(phone);
            if (userTest == null || user.getEppn().equals(userTest.getEppn())) {
                if(!otp.getSmsSended() && smsService != null) {
                    Pattern pattern = Pattern.compile("^(\\d{2}[- .]?){5}$");
                    Matcher matcher = pattern.matcher(phone);
                    if(matcher.matches()) {
                        String password = otpService.generateOtpPassword(urlId);
                        logger.info("sending password by sms : " + password + " to " + phone);
                        try {
                            smsService.sendSms(phone, "Votre code de connexion esup-signature " + password);
                        } catch(EsupSignatureRuntimeException e) {
                            logger.error(e.getMessage(), e);
                        }
                        otp.setPhoneNumber(phone);
                        return "otp/signin";
                    } else {
                        redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Numéro de mobile incorrect"));
                        return "redirect:/otp-access/first/" + urlId;
                    }
                }
                redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Merci de saisir le code reçu par SMS"));
                return "redirect:/otp-access/first/" + urlId;
            } else {
                redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Ce numéro ne peut pas être utilisé"));
                return "redirect:/otp-access/first/" + urlId;
            }
        } else {
            return "redirect:/denied/" + urlId;
        }
    }

    @PostMapping
    public String auth(@RequestParam String urlId, @RequestParam String password, Model model, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) throws EsupSignatureUserException {
        Boolean testOtp = otpService.checkOtp(urlId, password);
        if(testOtp != null) {
            if (testOtp) {
                Otp otp = otpService.getOtpFromCache(urlId);
                otp.setSmsSended(true);
                logger.info("otp success for : " + urlId);
                User user = otp.getUser();
                if(StringUtils.hasText(otp.getPhoneNumber())) {
                    userService.updatePhone(user.getEppn(), otp.getPhoneNumber());
                }
                authOtp(model, httpServletRequest, user);
                return "redirect:/otp/signrequests/signbook-redirect/" + otp.getSignBook().getId();
            } else {
                model.addAttribute("result", "KO");
                redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Mauvais code SMS, un nouveau code vous à été envoyé"));
                return "redirect:/otp-access/first/" + urlId;
            }
        } else {
            return "redirect:/otp-access/first/" + urlId;
        }
    }

    private void authOtp(Model model, HttpServletRequest httpServletRequest, User user) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(user.getEppn(), "");
        Authentication authentication = authenticationManager.authenticate(usernamePasswordAuthenticationToken);
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);
        userService.updateRoles(user.getEppn(), authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
        HttpSession httpSession = httpServletRequest.getSession(true);
        httpSession.setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        model.addAttribute("user", user);
        model.addAttribute("authUser", user);
    }

}
