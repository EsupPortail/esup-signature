package org.esupportail.esupsignature.web.controller.pub;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.service.LogService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

@Controller
@Transactional
@RequestMapping("/public")
public class PublicController {

    @Resource
    LogService logService;

    @Resource
    SignRequestService signRequestService;

    @GetMapping(value = "/control/{token}")
    public String createSignBook(@PathVariable String token, Model model) {

        List<SignRequest> signRequestOptional = signRequestService.getSignRequestsByToken(token);
        if(signRequestOptional.size() > 0) {
            SignRequest signRequest = signRequestOptional.get(0);
            List<Log> logs = logService.getById(signRequest.getId());
            AbstractMap.SimpleEntry<List<User>, List<User>> userResponse = signRequestService.checkUserResponse(signRequest);
            model.addAttribute("usersHasSigned", userResponse.getValue());
            model.addAttribute("usersHasRefused", userResponse.getKey());
            model.addAttribute("signRequest", signRequest);
            model.addAttribute("signedDocument", signRequest.getSignedDocuments().get(signRequest.getSignedDocuments().size() - 1));
            model.addAttribute("logs", logs);
        }
        return "public/control";
    }

}
