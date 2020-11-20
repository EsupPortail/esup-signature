package org.esupportail.esupsignature.web.controller.pub;

import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.repository.LogRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@Transactional
@RequestMapping("/public")
public class PublicController {

    @Resource
    private LogRepository logRepository;

    @Resource
    private SignRequestRepository signRequestRepository;

    @GetMapping(value = "/control/{token}")
    public String createSignBook(@PathVariable String token, Model model) {
        List<SignRequest> signRequestOptional = signRequestRepository.findByToken(token);
        if(signRequestOptional.size() > 0) {
            SignRequest signRequest = signRequestOptional.get(0);
            List<Log> logs = logRepository.findBySignRequestId(signRequest.getId());
            model.addAttribute("signRequest", signRequest);
            model.addAttribute("signedDocument", signRequest.getSignedDocuments().get(signRequest.getSignedDocuments().size() - 1));
            model.addAttribute("logs", logs);
        }
        return "public/control";
    }

}
