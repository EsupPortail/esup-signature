package org.esupportail.esupsignature.web.otp;

import org.esupportail.esupsignature.entity.enums.EmailAlertFrequency;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.DayOfWeek;
import java.util.Arrays;

@CrossOrigin(origins = "*")
@RequestMapping("/otp/users")
@Controller
public class OtpUserController {

    @Resource
    private UserService userService;

    @GetMapping
    public String updateForm(@ModelAttribute("authUserEppn") String authUserEppn, Model model, @RequestParam(value = "referer", required=false) String referer, HttpServletRequest request) {
        model.addAttribute("emailAlertFrequencies", Arrays.asList(EmailAlertFrequency.values()));
        model.addAttribute("daysOfWeek", Arrays.asList(DayOfWeek.values()));
        model.addAttribute("uiParams", userService.getUiParams(authUserEppn));
        if(referer != null && !"".equals(referer) && !"null".equals(referer)) {
            model.addAttribute("referer", request.getHeader(HttpHeaders.REFERER));
        }
        model.addAttribute("activeMenu", "settings");
        return "user/users/update";
    }

    @PostMapping
    public String update(@ModelAttribute("authUserEppn") String authUserEppn, @RequestParam(value = "signImageBase64", required=false) String signImageBase64,
                         @RequestParam(value = "emailAlertFrequency", required=false) EmailAlertFrequency emailAlertFrequency,
                         @RequestParam(value = "emailAlertHour", required=false) Integer emailAlertHour,
                         @RequestParam(value = "emailAlertDay", required=false) DayOfWeek emailAlertDay,
                         @RequestParam(value = "multipartKeystore", required=false) MultipartFile multipartKeystore, RedirectAttributes redirectAttributes) throws Exception {
        userService.updateUser(authUserEppn, signImageBase64, emailAlertFrequency, emailAlertHour, emailAlertDay, multipartKeystore);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Vos paramètres ont été enregistrés"));
        return "redirect:/otp/users/";
    }

    @GetMapping("/delete-sign/{id}")
    public String deleteSign(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable long id, RedirectAttributes redirectAttributes) {
        userService.deleteSign(authUserEppn, id);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Signature supprimée"));
        return "redirect:/otp/users/";
    }

    @GetMapping("/set-default-sign-image/{signImageNumber}")
    public String setDefaultSignImage(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("signImageNumber") Integer signImageNumber) {
        userService.setDefaultSignImage(authUserEppn, signImageNumber);
        return "redirect:/otp/users/";
    }
}
