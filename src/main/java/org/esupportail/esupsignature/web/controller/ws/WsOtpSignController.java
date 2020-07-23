package org.esupportail.esupsignature.web.controller.ws;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.security.otp.Otp;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.esupportail.esupsignature.service.sms.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

@RequestMapping("/ws/otp-sign")
@Controller
@Transactional
public class WsOtpSignController {

    private static final Logger logger = LoggerFactory.getLogger(WsOtpSignController.class);

    @Resource
    private OtpService otpService;

    @Resource
    private UserService userService;

    @Resource
    AuthenticationManager authenticationManager;

    @Resource
    private GlobalProperties globalProperties;

    @ModelAttribute(value = "globalProperties")
    public GlobalProperties getGlobalProperties() {
        return this.globalProperties;
    }

    @Resource
    private UserRepository userRepository;

    @Resource
    private SmsService smsService;

    @GetMapping(value = "/{urlId}")
    public String otpSign(@PathVariable String urlId, Model model) throws EsupSignatureException {
        Otp otp = otpService.getOtp(urlId);
        if(otp != null) {
            if(!otp.isSmsSended()) {
                smsService.sendSms(otp.getPhoneNumber(), "Votre code de sécutité esup-signature : " + otp.getPassword());
                otp.setSmsSended(true);
            }
            model.addAttribute("urlid", urlId);
            return "ws/otp-sign";
        } else {
            return "redirect:/denied/" + urlId;
        }
    }

    @PostMapping
    public String sign(@RequestParam String urlId, @RequestParam String password, Model model, RedirectAttributes redirectAttributes, HttpServletRequest req) throws EsupSignatureUserException {
        Boolean testOtp = otpService.checkOtp(urlId, password);
        if(testOtp != null) {
            if (testOtp) {
                Otp otp = otpService.getOtp(urlId);
                logger.info("otp success for : " + urlId);
                User user = userService.getUserByEmail(otp.getEmail());
                userRepository.save(user);
                UsernamePasswordAuthenticationToken authReq = new UsernamePasswordAuthenticationToken(otp.getPhoneNumber(), "");
                Authentication auth = authenticationManager.authenticate(authReq);
                SecurityContext sc = SecurityContextHolder.getContext();
                sc.setAuthentication(auth);
                HttpSession session = req.getSession(true);
                session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, sc);
                model.addAttribute("user", user);
                model.addAttribute("authUser", user);
                return "redirect:/user/signrequests/" + otp.getSignRequestId();
            } else {
                model.addAttribute("result", "KO");
                redirectAttributes.addFlashAttribute("messageError", "Mauvais mot de passe");
                return "redirect:/ws/otp-sign/" + urlId;
            }
        } else {
            return "redirect:/denied/" + urlId;
        }
    }

}
