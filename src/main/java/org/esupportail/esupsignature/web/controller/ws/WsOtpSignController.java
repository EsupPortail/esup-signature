package org.esupportail.esupsignature.web.controller.ws;

import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.security.Otp;
import org.esupportail.esupsignature.service.security.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;

@RequestMapping("/ws/otp-sign")
@Controller
@Transactional
public class WsOtpSignController {

    private static final Logger logger = LoggerFactory.getLogger(WsOtpSignController.class);

    @Resource
    private SignRequestRepository signRequestRepository;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private OtpService otpService;

    @GetMapping(value = "/{urlId}")
    public String otpSign(@PathVariable String urlId, Model model) {
        Otp otp = otpService.getOtp(urlId);
        if(otp != null) {
            model.addAttribute("urlId", urlId);
            return "ws/otp-sign";
        } else {
            return "redirect:/denied/" + urlId;
        }
    }

    @PostMapping
    public String sign(@RequestParam String urlId, @RequestParam String password, Model model, RedirectAttributes redirectAttributes) throws EsupSignatureException {
        Boolean testOtp = otpService.checkOtp(urlId, password);
        if(testOtp != null) {
            if (testOtp) {
                Otp otp = otpService.getOtp(urlId);
                logger.info("otp success for : " + urlId);
                SignRequest signRequest = signRequestRepository.findById(otp.getSignRequestId()).get();
                signRequestService.serverSign(signRequest);
                otpService.clearOTP(urlId);
                model.addAttribute("result", "OK");
            } else {
                model.addAttribute("result", "KO");
                redirectAttributes.addFlashAttribute("messageError", "Mauvais mot de passe");
                return "redirect:/ws/otp-sign/" + urlId;
            }
            return "ws/otp-sign-end";
        } else {
            return "redirect:/denied/" + urlId;
        }
    }

}
