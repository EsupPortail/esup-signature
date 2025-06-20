package org.esupportail.esupsignature.web.wsjwt;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import org.esupportail.esupsignature.dto.json.WorkflowStepDto;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.SignWith;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.RecipientService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ws-jwt/signrequests")
public class SignRequestJwtController {

    private final UserService userService;
    private final RecipientService recipientService;
    private final SignRequestService signRequestService;
    private final SignBookService signBookService;

    public SignRequestJwtController(UserService userService, RecipientService recipientService, SignRequestService signRequestService, SignBookService signBookService) {
        this.userService = userService;
        this.recipientService = recipientService;
        this.signRequestService = signRequestService;
        this.signBookService = signBookService;
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping
    public ResponseEntity<?> testJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.getByEppn(userService.buildEppn(authentication.getName()));
        return ResponseEntity.ok(user.getEmail());
    }

    @CrossOrigin
    @PostMapping(value ="/sign", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(security = @SecurityRequirement(name = "bearer token"), description = "Création d'une demande de signature et signature du document")
    public ResponseEntity<?> sign(@Parameter(description = "Multipart stream du fichier à signer") @RequestParam MultipartFile[] multipartFiles,
                                    @RequestParam(required = false) @Parameter(description = "Multipart stream des pièces jointes") MultipartFile[] attachementMultipartFiles,
                                    @RequestParam(required = false) @Parameter(description = "Paramètres des étapes (objet json)", array = @ArraySchema(schema = @Schema( implementation = WorkflowStepDto.class)), example =
                                            """
                                                  [{
                                                  "title": "string",
                                                  "workflowId": 0,
                                                  "stepNumber": 1,
                                                  "description": "string",
                                                  "recipientsCCEmails": [
                                                    "string"
                                                  ],
                                                  "recipients": [
                                                    {
                                                      "step": 0,
                                                      "email": "string",
                                                      "phone": "string",
                                                      "name": "string",
                                                      "firstName": "string",
                                                      "forceSms": true
                                                    }
                                                  ],
                                                  "signRequestParams": [
                                                    {
                                                      "signPageNumber": 1,
                                                      "signDocumentNumber": 0,
                                                      "signWidth": 150,
                                                      "signHeight": 75,
                                                      "xPos": 0,
                                                      "yPos": 0
                                                    }
                                                  ],
                                                  "changeable": false,
                                                  "signLevel": 0,
                                                  "signType": "visa",
                                                  "repeatable": false,
                                                  "repeatableSignType": "visa",
                                                  "allSignToComplete": false,
                                                  "userSignFirst": false,
                                                  "multiSign": true,
                                                  "singleSignWithAnnotation": false,
                                                  "autoSign": false,
                                                  "forceAllSign": false,
                                                  "comment": "string",
                                                  "attachmentRequire": false,
                                                  "attachmentAlert": false,
                                                  "maxRecipients": 99,
                                                  "targetEmails": [
                                                    "string"
                                                  ]
                                                  }]
                                                  """) String stepsJsonString,
                                    @RequestParam(required = false) @Parameter(description = "Titre (facultatif)") String title,
                                    @RequestParam(required = false) @Parameter(description = "Liste des personnes en copie (emails). Ne prend pas en charge les groupes") List<String> recipientsCCEmails,
                                    @RequestParam(required = false) @Parameter(description = "Commentaire") String comment,
                                    @RequestParam(required = false) @Parameter(description = "Envoyer la demande automatiquement") Boolean pending,
                                    @RequestParam(required = false) @Parameter(description = "Emplacement final", example = "smb://drive.univ-ville.fr/forms-archive/") String targetUrl,
                                    @RequestParam(required = false) @Parameter(description = "Retour au format json (facultatif, false par défaut)") Boolean json,
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "Liste des participants") List<String> recipientsEmails,
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "Liste des participants (ancien nom)") List<String> recipientEmails,
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "Tout les participants doivent-ils signer ?") Boolean allSignToComplete,
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "Le créateur doit-il signer en premier ?") Boolean userSignFirst,
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "Forcer la signature de tous les documents") Boolean forceAllSign,
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "Type de signature", schema = @Schema(allowableValues = {"visa", "pdfImageStamp", "certSign", "nexuSign"}), examples = {@ExampleObject(value = "visa"), @ExampleObject(value = "pdfImageStamp"), @ExampleObject(value = "certSign"), @ExampleObject(value = "nexuSign")}) String signType,
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "EPPN du créateur/propriétaire de la demande (ancien nom)") String eppn
    ) throws EsupSignatureException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.getByEppn(userService.buildEppn(authentication.getName()));
        if(json == null) {
            json = false;
        }
        if(recipientEmails == null && recipientsEmails != null && !recipientsEmails.isEmpty()) {
            recipientEmails = recipientsEmails;
        }
        List<WorkflowStepDto> workflowStepDtos;
        if(stepsJsonString == null && recipientEmails != null) {
            workflowStepDtos = recipientService.convertRecipientEmailsToStep(recipientEmails);
            workflowStepDtos.forEach(workflowStepDto -> {
                workflowStepDto.setSignType(SignType.valueOf(signType));
                workflowStepDto.setAllSignToComplete(allSignToComplete);
                workflowStepDto.setUserSignFirst(userSignFirst);
                workflowStepDto.setComment(comment);
                workflowStepDto.setRecipientsCCEmails(recipientsCCEmails);
            });
        } else {
            workflowStepDtos = recipientService.convertRecipientJsonStringToWorkflowStepDtos(stepsJsonString);
        }
        if(workflowStepDtos != null) {
            Map<SignBook, String> signBookStringMap = signBookService.createAndSendSignBook(title, multipartFiles, pending, workflowStepDtos, user.getEppn(), false, forceAllSign, targetUrl);
            List<String> signRequestIds = signBookStringMap.keySet().stream().flatMap(sb -> sb.getSignRequests().stream().map(signRequest -> signRequest.getId().toString())).toList();
            if(signRequestIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("-1");
            }
            signRequestService.addAttachement(attachementMultipartFiles, null, Long.valueOf(signRequestIds.get(0)), user.getEppn());
            for(String signRequestId : signRequestIds) {
                signBookService.initSign(Long.valueOf(signRequestId), null, null, null, "", SignWith.imageStamp.name(), null, user.getEppn(), user.getEppn());
            }
            if(json) {
                return ResponseEntity.ok(signRequestIds);
            } else {
                return ResponseEntity.ok(org.apache.commons.lang.StringUtils.join(signRequestIds, ","));
            }
        }
        return ResponseEntity.internalServerError().body("-1");
    }

    @GetMapping(value = "/get-last-file/{id}")
    @Operation(security = @SecurityRequirement(name = "bearer token"), description = "Récupérer le dernier fichier signé d'une demande", responses = @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = byte[].class), mediaType = MediaType.APPLICATION_PDF_VALUE)))
    public ResponseEntity<Void> getLastFileFromSignRequest(@PathVariable("id") Long id, HttpServletResponse httpServletResponse) throws IOException, EsupSignatureException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.getByEppn(userService.buildEppn(authentication.getName()));
        SignRequest signRequest = signRequestService.getById(id);
        if(signRequest.getCreateBy().getEppn().equals(user.getEppn())) {
            signRequestService.getToSignFileResponse(id, "form-data", httpServletResponse, false);
        }
        return ResponseEntity.ok().build();
    }

}
