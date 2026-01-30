package org.esupportail.esupsignature.web.controller.otp;

import jakarta.servlet.http.HttpServletRequest;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.entity.enums.EmailAlertFrequency;
import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;

@CrossOrigin(origins = "*")
@RequestMapping("/otp/users")
@Controller
public class OtpUserController {

    private static final Logger logger = LoggerFactory.getLogger(OtpUserController.class);

    private final UserService userService;

    public OtpUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public String update(@ModelAttribute("authUserEppn") String authUserEppn, @RequestParam(value = "signImageBase64", required=false) String signImageBase64,
                         @RequestParam(value = "name") String name,
                         @RequestParam(value = "firstname") String firstname,
                         @RequestParam(value = "emailAlertFrequency", required=false) EmailAlertFrequency emailAlertFrequency,
                         @RequestParam(value = "emailAlertHour", required=false) Integer emailAlertHour,
                         @RequestParam(value = "emailAlertDay", required=false) DayOfWeek emailAlertDay,
                         @RequestParam(value = "multipartKeystore", required=false) MultipartFile multipartKeystore,
                         RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) throws Exception {
        userService.updateUser(authUserEppn, name, firstname, signImageBase64, emailAlertFrequency, emailAlertHour, emailAlertDay, multipartKeystore, null, false);
        redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Vos paramètres ont été enregistrés"));
        String referer = httpServletRequest.getHeader(HttpHeaders.REFERER);
        return "redirect:" + referer;
    }

    @DeleteMapping("/delete-sign/{id}")
    public String deleteSign(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable long id, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) {
        userService.deleteSign(authUserEppn, id);
        redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Signature supprimée"));
        String referer = httpServletRequest.getHeader(HttpHeaders.REFERER);
        return "redirect:" + referer;
    }

    @GetMapping("/set-default-sign-image/{signImageNumber}")
    public String setDefaultSignImage(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("signImageNumber") Integer signImageNumber) {
        userService.setDefaultSignImage(authUserEppn, signImageNumber);
        return "redirect:/otp/users";
    }

    @GetMapping("/mark-intro-as-read/{name}")
    public String markIntroAsRead(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable String name, HttpServletRequest httpServletRequest) {
        logger.info("user " + authUserEppn + " mark intro " + name + " as read");
        userService.disableIntro(authUserEppn, name);
        String referer = httpServletRequest.getHeader(HttpHeaders.REFERER);
        return "redirect:" + referer;
    }

}
