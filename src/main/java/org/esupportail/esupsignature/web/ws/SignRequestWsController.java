package org.esupportail.esupsignature.web.ws;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ws/signrequests")
public class SignRequestWsController {

    private static final Logger logger = LoggerFactory.getLogger(SignRequestWsController.class);

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

    @GetMapping(value = "/get-last-file/{id}")
    public ResponseEntity<Void> getLastFileFromSignRequest(@PathVariable("id") Long id, HttpServletResponse httpServletResponse) {
        try {
            Map<String, Object> fileResponse = signRequestService.getToSignFileResponse(id);
            if (fileResponse != null) {
                httpServletResponse.setContentType(fileResponse.get("contentType").toString());
                httpServletResponse.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(fileResponse.get("fileName").toString(), StandardCharsets.UTF_8.toString()));
                IOUtils.copyLarge((InputStream) fileResponse.get("inputStream"), httpServletResponse.getOutputStream());
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (NoResultException | IOException | EsupSignatureFsException | SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
