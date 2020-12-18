package org.esupportail.esupsignature.web;

import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.LogService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import java.util.AbstractMap;
import java.util.List;

@Controller

@RequestMapping("/public")
public class PublicController {

    @Resource
    LogService logService;

    @Resource
    SignRequestService signRequestService;

    @GetMapping(value = "/control/{token}")
    public String control(@PathVariable String token, Model model) {
        List<SignRequest> signRequestOptional = signRequestService.getSignRequestsByToken(token);
        if(signRequestOptional.size() > 0) {
            SignRequest signRequest = signRequestOptional.get(0);
            List<Log> logs = logService.getById(signRequest.getId());
            AbstractMap.SimpleEntry<List<User>, List<User>> userResponse = signRequestService.checkUserResponse(signRequest);
            model.addAttribute("usersHasSigned", userResponse.getValue());
            model.addAttribute("usersHasRefused", userResponse.getKey());
            model.addAttribute("signRequest", signRequest);
            model.addAttribute("signedDocument", signRequestService.getLastSignedFile(signRequest.getId()));
            model.addAttribute("logs", logs);
        }
        return "public/control";
    }

}
