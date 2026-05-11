package org.esupportail.esupsignature.web.controller.otp;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/otp/signrequests")
@EnableConfigurationProperties(GlobalProperties.class)
public class OtpSignRequestController {

    private static final Logger logger = LoggerFactory.getLogger(OtpSignRequestController.class);

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "signrequests";
    }

    private final UserService userService;
    private final PreAuthorizeService preAuthorizeService;
    private final SignBookService signBookService;

    public OtpSignRequestController(UserService userService, PreAuthorizeService preAuthorizeService, SignBookService signBookService) {
        this.userService = userService;
        this.preAuthorizeService = preAuthorizeService;
        this.signBookService = signBookService;
    }

    @GetMapping(value = "/signbook-redirect/{id}")
    public String redirect(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) throws IOException, EsupSignatureRuntimeException {
        if(!preAuthorizeService.signBookView(id, userEppn, authUserEppn)) {
            User user = userService.getByEppn(userEppn);
            redirectAttributes.addFlashAttribute("errorMsg", "Access non autorisé");
            String userEmail = user != null ? user.getEmail() : null;
            if (userEmail != null && !signBookService.isUserEmailInCurrentStepRecipients(id, userEmail)) {
                redirectAttributes.addFlashAttribute("errorMsg",
                        "<p>L'adresse email liée à votre authentification ne correspond pas à celle indiquée dans la demande initiale.<br>\n" +
                                "Voici l'adresse transmise par votre fournisseur d'identité :"
                                + userEmail +
                                "</p><p>Vous pouvez soit modifier votre adresse de contact auprès de votre fournisseur d'identité, soit contacter le gestionnaire de la demande afin qu’il mette à jour l’email de contact dans la demande de signature.</p>");
            }
            return "redirect:/otp-access/error";
        }
        Long signRequestId = signBookService.getRedirectSignRequestId(id);
        if (signRequestId == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Demande de signature introuvable");
            return "redirect:/otp-access/error";
        }
        return "redirect:/otp/signrequests/" + signRequestId;
    }

}