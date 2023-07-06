package org.esupportail.esupsignature.web.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.esupportail.esupsignature.entity.AuditTrail;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.web.ws.json.JsonExternalUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ws/signrequests")
public class SignRequestWsController {

    private static final Logger logger = LoggerFactory.getLogger(SignRequestWsController.class);

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private SignBookService signBookService;

    @CrossOrigin
    @PostMapping("/new")
    @Operation(description = "Création d'une demande de signature")
    public String create(@Parameter(description = "Multipart stream du fichier à signer") @RequestParam MultipartFile[] multipartFiles,
                       @Parameter(description = "Liste des participants") @RequestParam(value = "recipientsEmails", required = false) List<String> recipientsEmails,
                       @Parameter(description = "Liste des participants (ancien nom)") @RequestParam(value = "recipientEmails", required = false) List<String> recipientEmails,
                       @Parameter(description = "Liste des personnes en copie") @RequestParam(value = "recipientsCCEmails", required = false) List<String> recipientsCCEmails,
                       @Parameter(description = "Tout les participants doivent-ils signer ?") @RequestParam(name = "allSignToComplete", required = false) Boolean allSignToComplete,
                       @Parameter(description = "Le créateur doit-il signer en premier ?") @RequestParam(name = "userSignFirst", required = false) Boolean userSignFirst,
                       @Parameter(description = "Envoyer la demande automatiquement") @RequestParam(value = "pending", required = false) Boolean pending,
                       @Parameter(description = "Forcer la signature de tous les documents") @RequestParam(value = "forceAllSign", required = false) Boolean forceAllSign,
                       @Parameter(description = "Commentaire") @RequestParam(value = "comment", required = false) String comment,
                       @Parameter(description = "Infos pour les des signataires externes", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = JsonExternalUserInfo.class)))) @RequestParam(value = "externalUsersInfos", required = false) List<JsonExternalUserInfo> externalUsersInfos,
                       @Parameter(description = "Type de signature", schema = @Schema(allowableValues = {"visa", "pdfImageStamp", "certSign", "nexuSign"}), examples = {@ExampleObject(value = "visa"), @ExampleObject(value = "pdfImageStamp"), @ExampleObject(value = "certSign"), @ExampleObject(value = "nexuSign")}) @RequestParam("signType") String signType,
                       @Parameter(description = "EPPN du créateur/propriétaire de la demande (ancien nom)") @RequestParam(required = false) String eppn,
                       @Parameter(description = "EPPN du créateur/propriétaire de la demande") @RequestParam(required = false) String createByEppn,
                       @Parameter(description = "Un titre (facultatif)") @RequestParam(value = "title", required = false) String title,
                       @RequestParam(required = false) @Parameter(description = "Emplacement final", example = "smb://drive.univ-ville.fr/forms-archive/") String targetUrl) {
        if(createByEppn == null && StringUtils.hasText(eppn)) {
            createByEppn = eppn;
        }
        if(createByEppn == null) {
            throw new EsupSignatureRuntimeException("Required request parameter 'createByEppn' for method parameter type String is not present");
        }
        if(recipientsEmails == null && recipientEmails.size() > 0) {
            recipientsEmails = recipientEmails;
        }
        try {
            Map<SignBook, String> signBookStringMap = signBookService.sendSignRequest(title, multipartFiles, SignType.valueOf(signType), allSignToComplete, userSignFirst, pending, comment, recipientsCCEmails, recipientsEmails, externalUsersInfos, createByEppn, createByEppn, true, forceAllSign, targetUrl);
            return signBookStringMap.keySet().stream().map(sb -> sb.getSignRequests().stream().map(signRequest -> signRequest.getId().toString()).collect(Collectors.joining(","))).collect(Collectors.joining(","));
//            return signBookStringMap.keySet().iterator().next().getSignRequests().get(0).getId();
        } catch (EsupSignatureRuntimeException e) {
            logger.error(e.getMessage(), e);
            return "-1";
        }
    }

    @CrossOrigin
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(description = "Récupération d'une demande de signature", responses = @ApiResponse(description = "SignRequest", content = @Content(schema = @Schema(implementation = SignRequest.class))))
    public String get(@Parameter(description = "Identifiant de la demande") @PathVariable Long id) throws JsonProcessingException {
        return signRequestService.getJson(id);
    }

    @CrossOrigin
    @GetMapping(value = "/status/{id}")
    @Operation(description = "Récupération du statut d'une demande de signature")
    @ResponseBody
    public String getStatus(@Parameter(description = "Identifiant de la demande") @PathVariable Long id) {
        return signRequestService.getStatus(id);
    }

    @CrossOrigin
    @GetMapping(value = "/audit-trail/{id}")
    @Operation(description = "Récupération du dossier de preuve de la demande", responses = @ApiResponse(description = "AuditTrail", content = @Content(schema = @Schema(implementation = AuditTrail.class))))
    @ResponseBody
    public String getAuditTail(@Parameter(description = "Dossier de preuve de la demande") @PathVariable Long id) throws JsonProcessingException {
        return signRequestService.getAuditTrailJson(id);
    }

    @CrossOrigin
    @DeleteMapping("/{id}")
    @Operation(description = "Supprimer une demande de signature")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        Long signBookId = signRequestService.getParentIdIfSignRequestUnique(id);
        if(signBookId != null) {
            signBookService.deleteDefinitive(signBookId, "system");
        } else {
            signRequestService.deleteDefinitive(id);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @CrossOrigin
    @DeleteMapping("/{id}/signbook")
    @Operation(description = "Supprimer le parapheur dans lequel se trouve la demande ciblée")
    public ResponseEntity<String> deleteSignBook(@PathVariable Long id) {
        SignRequest signRequest = signRequestService.getById(id);
        signBookService.deleteDefinitive(signRequest.getParentSignBook().getId(), "system");
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(value = "/get-last-file/{id}")
    @ResponseBody
    @Operation(description = "Récupérer le dernier fichier signé d'une demande", responses = @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = byte[].class), mediaType = "application/pdf")))
    public ResponseEntity<Void> getLastFileFromSignRequest(@PathVariable("id") Long id, HttpServletResponse httpServletResponse) {
        try {
            signRequestService.getToSignFileResponse(id, "attachment", httpServletResponse);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping(value = "/print-with-code/{id}")
    @ResponseBody
    @Operation(description = "Récupérer le dernier fichier signé d'une demande avec un datamatrix apposé dessus", responses = @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = byte[].class), mediaType = "application/pdf")))
    public ResponseEntity<Void> printWithCode(@PathVariable("id") Long id, HttpServletResponse httpServletResponse) {
        try {
            signRequestService.getToSignFileResponseWithCode(id, httpServletResponse);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
