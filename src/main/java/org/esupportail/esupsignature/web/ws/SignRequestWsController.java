package org.esupportail.esupsignature.web.ws;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ws/signrequests")
public class SignRequestWsController {

    @Resource
    SignRequestService signRequestService;

    @Resource
    UserService userService;

    @CrossOrigin
    @PostMapping("/new")
    public Long create(@RequestParam MultipartFile[] multipartFiles, @RequestParam(value = "recipientsEmails", required = false) String[] recipientsEmails, @RequestParam(value = "recipientsCCEmails", required = false) String[] recipientsCCEmails,
                         @RequestParam(name = "allSignToComplete", required = false) Boolean allSignToComplete,
                         @RequestParam(name = "userSignFirst", required = false) Boolean userSignFirst,
                         @RequestParam(value = "pending", required = false) Boolean pending,
                         @RequestParam(value = "comment", required = false) String comment,
                         @RequestParam("signType") String signType,
                         @RequestParam String eppn) {
        User user = userService.getByEppn(eppn);
        try {
            Map<SignBook, String> signBookStringMap = signRequestService.sendSignRequest(multipartFiles, recipientsEmails, recipientsCCEmails, allSignToComplete, userSignFirst, pending, comment, SignType.valueOf(signType), user, user);
            return signBookStringMap.keySet().iterator().next().getSignRequests().get(0).getId();
        } catch (EsupSignatureException | EsupSignatureIOException e) {
            return -1L;
        }
    }

    @CrossOrigin
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public SignRequest get(@PathVariable Long id) {
        return signRequestService.getById(id);
    }

    @CrossOrigin
    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SignRequest> getAll() {
        return signRequestService.getAll();
    }

    @CrossOrigin
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        signRequestService.deleteDefinitive(id);
        return new ResponseEntity<>(HttpStatus.OK);

    }

    public String update() {
        return "toto";
    }
}
