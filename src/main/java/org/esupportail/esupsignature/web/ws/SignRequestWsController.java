package org.esupportail.esupsignature.web.ws;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import org.esupportail.esupsignature.web.ws.json.JsonExternalUserInfo;
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
    private SignRequestService signRequestService;

    @Resource
    private UserService userService;

    @CrossOrigin
    @PostMapping("/new")
    @Operation(description = "Création d'une demande de signature")
    public Long create(@Parameter(description = "Multipart stream du fichier à signer") @RequestParam MultipartFile[] multipartFiles,
                       @Parameter(description = "Liste des participants") @RequestParam(value = "recipientsEmails", required = false) List<String> recipientsEmails,
                       @Parameter(description = "Liste des personnes en copie") @RequestParam(value = "recipientsCCEmails", required = false) List<String> recipientsCCEmails,
                       @Parameter(description = "Tout les participants doivent-ils signer ?") @RequestParam(name = "allSignToComplete", required = false) Boolean allSignToComplete,
                       @Parameter(description = "Le créateur doit-il signer en premier ?") @RequestParam(name = "userSignFirst", required = false) Boolean userSignFirst,
                       @Parameter(description = "Envoyer la demande automatiquement") @RequestParam(value = "pending", required = false) Boolean pending,
                       @Parameter(description = "Forcer la signature de tous les documents") @RequestParam(value = "forceAllSign", required = false) Boolean forceAllSign,
                       @Parameter(description = "Commentaire") @RequestParam(value = "comment", required = false) String comment,
                       @Parameter(description = "Infos pour les des signataires externes", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = JsonExternalUserInfo.class)))) @RequestParam(value = "externalUsersInfos", required = false) List<JsonExternalUserInfo> externalUsersInfos,
                       @Parameter(description = "Type de signature", schema = @Schema(allowableValues = {"visa", "pdfImageStamp", "certSign", "nexuSign"}), examples = {@ExampleObject(value = "visa"), @ExampleObject(value = "pdfImageStamp"), @ExampleObject(value = "certSign"), @ExampleObject(value = "nexuSign")}) @RequestParam("signType") String signType,
                       @Parameter(description = "EPPN du créateur/propriétaire de la demande") @RequestParam String eppn) {
        User user = userService.getByEppn(eppn);
        try {
            Map<SignBook, String> signBookStringMap = signRequestService.sendSignRequest(multipartFiles, SignType.valueOf(signType), allSignToComplete, userSignFirst, pending, comment, recipientsCCEmails, recipientsEmails, externalUsersInfos, user, user, true, forceAllSign);
            return signBookStringMap.keySet().iterator().next().getSignRequests().get(0).getId();
        } catch (EsupSignatureException | EsupSignatureIOException e) {
            logger.error(e.getMessage());
            return -1L;
        }
    }

    @CrossOrigin
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(description = "Récupération d'une demande de signature")
    public SignRequest get(@Parameter(description = "Identifiant de la demande") @PathVariable Long id) {
        return signRequestService.getById(id);
    }

    @CrossOrigin
    @DeleteMapping("/{id}")
    @Operation(description = "Supprimer une demande de signature")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        signRequestService.deleteDefinitive(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(value = "/get-last-file/{id}")
    @ResponseBody
    @Operation(description = "Récupérer le dernier fichier signé d'une demande", responses = @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = byte[].class), mediaType = "application/pdf")))
    public ResponseEntity<Void> getLastFileFromSignRequest(@PathVariable("id") Long id, HttpServletResponse httpServletResponse) {
        try {
            Map<String, Object> fileResponse = signRequestService.getToSignFileResponse(id);
            if (fileResponse != null) {
                httpServletResponse.setContentType(fileResponse.get("contentType").toString());
                httpServletResponse.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileResponse.get("fileName").toString(), StandardCharsets.UTF_8.toString()));
                IOUtils.copyLarge((InputStream) fileResponse.get("inputStream"), httpServletResponse.getOutputStream());
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (NoResultException | IOException | EsupSignatureFsException | SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
