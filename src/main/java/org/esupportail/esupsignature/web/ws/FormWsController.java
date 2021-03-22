package org.esupportail.esupsignature.web.ws;

import io.swagger.v3.oas.annotations.Parameter;
import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.service.DataService;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/ws/forms")
public class FormWsController {

    @Resource
    DataService dataService;

    @Resource
    UserService userService;

    @CrossOrigin
    @PostMapping(value = "/{id}")
    public Long start(@PathVariable Long id, @RequestParam String eppn, @RequestParam(required = false) @Parameter(description = "pattern : stepNumber*email") List<String> recipientEmails, @RequestParam(required = false) List<String> targetEmails) {
        User user = userService.getByEppn(eppn);
        Data data = dataService.addData(id, user, user);
        try {
            SignBook signBook = dataService.sendForSign(data, recipientEmails, targetEmails, user, user);
            return signBook.getSignRequests().get(0).getId();
        } catch (EsupSignatureException | EsupSignatureIOException e) {
            return -1L;
        }
    }
}
