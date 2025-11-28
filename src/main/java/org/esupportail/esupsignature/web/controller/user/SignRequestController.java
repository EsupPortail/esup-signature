package org.esupportail.esupsignature.web.controller.user;

import jakarta.servlet.http.HttpServletRequest;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureMailException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user/signrequests")
@EnableConfigurationProperties(GlobalProperties.class)
public class SignRequestController {

    private static final Logger logger = LoggerFactory.getLogger(SignRequestController.class);

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "signrequests";
    }

    private final UserService userService;
    private final SignRequestService signRequestService;
    private final SignBookService signBookService;

    private final OtpService otpService;

    public SignRequestController(UserService userService, SignRequestService signRequestService, SignBookService signBookService, OtpService otpService) {
        this.userService = userService;
        this.signRequestService = signRequestService;
        this.signBookService = signBookService;
        this.otpService = otpService;
    }

    @GetMapping()
    public String show() {
        return "redirect:/user";
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @PostMapping(value = "/clone/{id}")
    public String clone(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @RequestParam(value = "multipartFiles", required = false) MultipartFile[] multipartFiles, @RequestParam(value = "comment") String comment, RedirectAttributes redirectAttributes) throws EsupSignatureRuntimeException {
        Long cloneId = signBookService.clone(id, multipartFiles, comment, authUserEppn);
        redirectAttributes.addFlashAttribute("messageInfos", "La demandes a bien été refusée");
        return "redirect:/user/signrequests/" + cloneId;
    }

    @PreAuthorize("@preAuthorizeService.signRequestDelete(#id, #authUserEppn)")
    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @RequestParam(value = "definitive", required = false) Boolean definitive, RedirectAttributes redirectAttributes) {
        Long result;
        if(definitive != null && definitive) {
            result = signRequestService.deleteDefinitive(id, authUserEppn);
        } else {
            result = signRequestService.delete(id, authUserEppn);
        }
        if(result == 0L) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Suppression définitive effectuée"));
        } else if(result > 0L) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Suppression effectuée"));
            return "redirect:/user/signbooks/" + result;
        } else {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Suppression impossible car la demande à démarrée et contient encore des documents en cours de signature"));
            return "redirect:/user/signrequests/" + id;
        }
        return "redirect:/user/signbooks";
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @GetMapping(value = "/update-step/{id}/{step}")
    public String changeStepSignType(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("step") Integer step, @RequestParam(name = "signType") SignType signType) {
        SignRequest signRequest = signRequestService.getById(id);
        signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().setSignType(signType);
        return "redirect:/user/signrequests/" + id + "?form";
    }

    @PreAuthorize("@preAuthorizeService.signBookSendOtp(#id, #authUserEppn)")
    @PostMapping(value = "/send-otp/{id}/{userId}")
    @ResponseBody
    public ResponseEntity<?> sendOtp(@ModelAttribute("authUserEppn") String authUserEppn,
                          @PathVariable("id") Long id,
                          @PathVariable("userId") Long userId,
                          @RequestParam(value = "phone", required = false) String phone) {
        if(otpService.generateOtpForSignRequest(id, userId, phone, true) != null){
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.internalServerError().build();
        }
    }


    @PreAuthorize("@preAuthorizeService.signBookSendOtp(#id, #authUserEppn)")
    @PostMapping(value = "/send-otp-download/{id}/{recipientId}")
    public String sendOtpDownload(@ModelAttribute("authUserEppn") String authUserEppn,
                          @PathVariable("id") Long id,
                          @PathVariable("recipientId") Long recipientId,
                          @RequestParam(value = "phone", required = false) String phone,
                          RedirectAttributes redirectAttributes) {
        if(otpService.generateOtpForSignRequest(id, recipientId, phone, false) != null){
            redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Demande OTP envoyée"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Problème d'envoi OTP"));
        }
        return "redirect:/user/signbooks/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #authUserEppn, #authUserEppn)")
    @PostMapping(value = "/replay-notif/{id}")
    public String replayNotif(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) throws EsupSignatureMailException {
        if(signRequestService.replayNotif(id, authUserEppn)) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Votre relance a bien été envoyée"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Votre relance n'a pas été envoyée car une autre relance a déjà été émise"));
        }
        return "redirect:" + httpServletRequest.getHeader(HttpHeaders.REFERER);
    }

}
