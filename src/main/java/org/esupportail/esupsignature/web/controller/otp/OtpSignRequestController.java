package org.esupportail.esupsignature.web.controller.otp;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.exception.EsupSignatureMailException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
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
    private final SignRequestService signRequestService;
    private final SignBookService signBookService;

    public OtpSignRequestController(UserService userService, PreAuthorizeService preAuthorizeService, SignRequestService signRequestService, SignBookService signBookService) {
        this.userService = userService;
        this.preAuthorizeService = preAuthorizeService;
        this.signRequestService = signRequestService;
        this.signBookService = signBookService;
    }

    @GetMapping(value = "/signbook-redirect/{id}")
    public String redirect(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) throws IOException, EsupSignatureRuntimeException {
        SignBook signBook = signBookService.getById(id);
        if(!preAuthorizeService.signBookView(id, userEppn, authUserEppn)) {
            User user = userService.getByEppn(userEppn);
            redirectAttributes.addFlashAttribute("errorMsg", "Access non autorisé");
            if (signBook.getLiveWorkflow().getCurrentStep().getRecipients().stream().noneMatch(r -> r.getUser().getEmail().equals(user.getEmail()))) {
                redirectAttributes.addFlashAttribute("errorMsg",
                        "<p>L'adresse email liée à votre authentification ne correspond pas à celle indiquée dans la demande initiale.<br>\n" +
                                "Voici l'adresse transmise par votre fournisseur d'identité :"
                                + user.getEmail() +
                                "</p><p>Vous pouvez soit modifier votre adresse de contact auprès de votre fournisseur d'identité, soit contacter le gestionnaire de la demande afin qu’il mette à jour l’email de contact dans la demande de signature.</p>");
            }
            return "redirect:/otp-access/error";
        }
        return "redirect:/otp/signrequests/" + signBook.getSignRequests().get(0).getId();
    }

    @PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
    @PostMapping(value = "/refuse/{id}")
    public String refuse(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @RequestParam(value = "comment") String comment, RedirectAttributes redirectAttributes) throws EsupSignatureMailException, EsupSignatureRuntimeException {
        signBookService.refuse(id, comment, userEppn, authUserEppn);
        redirectAttributes.addFlashAttribute("message", new JsMessage("info", "La demandes a bien été refusée"));
        return "redirect:/otp/signrequests/" + id;
    }

}