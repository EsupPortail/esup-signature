package org.esupportail.esupsignature.web;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import eu.europa.esig.dss.validation.reports.Reports;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.io.FileUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.service.XSLTService;
import org.esupportail.esupsignature.entity.AuditTrail;
import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.service.AuditTrailService;
import org.esupportail.esupsignature.service.LogService;
import org.esupportail.esupsignature.service.MobileSignTokenService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.esupportail.esupsignature.service.utils.sign.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller

@RequestMapping("/public")
public class PublicController {

    private final BuildProperties buildProperties;
    private final GlobalProperties globalProperties;
    private final LogService logService;
    private final SignRequestService signRequestService;
    private final AuditTrailService auditTrailService;
    private final FileService fileService;
    private final UserService userService;
    private final XSLTService xsltService;
    private final PreAuthorizeService preAuthorizeService;
    private final ValidationService validationService;
    private final SignService signService;
    private final MobileSignTokenService mobileSignTokenService;

    public PublicController(@Autowired(required = false) BuildProperties buildProperties, GlobalProperties globalProperties, LogService logService, SignRequestService signRequestService, AuditTrailService auditTrailService, FileService fileService, UserService userService, XSLTService xsltService, PreAuthorizeService preAuthorizeService, ValidationService validationService, SignService signService, MobileSignTokenService mobileSignTokenService) {
        this.buildProperties = buildProperties;
        this.globalProperties = globalProperties;
        this.logService = logService;
        this.signRequestService = signRequestService;
        this.auditTrailService = auditTrailService;
        this.fileService = fileService;
        this.userService = userService;
        this.xsltService = xsltService;
        this.preAuthorizeService = preAuthorizeService;
        this.validationService = validationService;
        this.signService = signService;
        this.mobileSignTokenService = mobileSignTokenService;
    }

    @GetMapping(value = "/control")
    public String control() {
        return "public/control";
    }

    @GetMapping(value = "/control/{token}")
    public String controlToken(@PathVariable String token, Model model) throws EsupSignatureFsException, IOException {
        AuditTrail auditTrail = auditTrailService.getAuditTrailByToken(token);
        if(auditTrail == null) {
            return "error";
        }
        if(auditTrail.getDocumentSize() != null) {
            model.addAttribute("size", FileUtils.byteCountToDisplaySize(auditTrail.getDocumentSize()));
        }
        model.addAttribute("auditTrail", auditTrail);
        Optional<SignRequest> signRequest = signRequestService.getSignRequestByToken(token);
        if(signRequest.isPresent()) {
            if (auditTrail.getAuditSteps().stream().anyMatch(as -> as.getSignCertificat() != null && !as.getSignCertificat().isEmpty())) {
                Reports reports = signService.validate(signRequest.get().getId());
                if (reports != null) {
                    model.addAttribute("simpleReport", xsltService.generateShortReport(reports.getXmlSimpleReport()));
                }
            }
        } else {
            model.addAttribute("auditTrailChecked", false);
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getName().equals("anonymousUser")) {
            model.addAttribute("auditTrailChecked", true);
            String eppn = userService.tryGetEppnFromLdap(auth);
            if(eppn != null && userService.getByEppn(eppn) != null) {
                setControlValues(model, signRequest, auditTrail, eppn);
            }
        }
        if (buildProperties != null) {
            model.addAttribute("version", buildProperties.getVersion());
        }
        model.addAttribute("token", token);
        List<Log> logs = logService.getFullByToken(token);
        model.addAttribute("logs", logs);
        return "public/control";
    }

    @PostMapping(value = "/control/{token}")
    public String checkFile(@PathVariable String token, @RequestParam(value = "multipartFile") MultipartFile multipartFile,
                            Model model, HttpSession httpSession) throws IOException {
        model.addAttribute("token", token);
        String checksum = fileService.getFileChecksum(multipartFile.getInputStream());
        AuditTrail auditTrail = auditTrailService.getAuditTrailFromCheksum(checksum);
        if(auditTrail != null) {
            if(auditTrail.getDocumentSize() != null) {
                model.addAttribute("size", FileUtils.byteCountToDisplaySize(auditTrail.getDocumentSize()));
            }
            if("null".equals(token)) {
                token = auditTrail.getToken();
            }
            model.addAttribute("auditTrailChecked", true);
            List<Log> logs = logService.getFullByToken(token);
            model.addAttribute("logs", logs);
            Optional<SignRequest> signRequest = signRequestService.getSignRequestByToken(token);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String eppn = null;
            if (auth != null && !auth.getName().equals("anonymousUser")) {
                eppn = userService.tryGetEppnFromLdap(auth);
                if(httpSession.getAttribute("suEppn") != null) {
                    eppn = httpSession.getAttribute("suEppn").toString();
                }
            }
            if (buildProperties != null) {
                model.addAttribute("version", buildProperties.getVersion());
            }
            model.addAttribute("auditTrail", auditTrail);
            setControlValues(model, signRequest, auditTrail, eppn);
        } else if (token == null || token.equals("null")) {
            Reports reports = validationService.validate(multipartFile.getInputStream(), null);
            if(reports != null) {
                model.addAttribute("simpleReport", xsltService.generateShortReport(reports.getXmlSimpleReport()));
            }
            model.addAttribute("error", true);
        } else {
            model.addAttribute("error", true);
        }
        return "public/control";
    }

    private void setControlValues(Model model, Optional<SignRequest> signRequest, AuditTrail auditTrail, String eppn) throws IOException {
        if(signRequest.isPresent()) {
            model.addAttribute("signRequest", signRequest.get());
            model.addAttribute("usersHasSigned", auditTrailService.checkUserResponseSigned(signRequest.get()));
            model.addAttribute("usersHasRefused", auditTrailService.checkUserResponseRefused(signRequest.get()));
            if (eppn != null) {
                model.addAttribute("viewAccess", preAuthorizeService.checkUserViewRights(signRequest.get().getId(), eppn, eppn));
            }
            if(auditTrail.getAuditSteps().stream().anyMatch(as -> as.getSignCertificat() != null && !as.getSignCertificat().isEmpty())) {
                Reports reports = signService.validate(signRequest.get().getId());
                model.addAttribute("simpleReport", xsltService.generateShortReport(reports.getXmlSimpleReport()));
            }
        }
    }

    // Mobile sign endpoints
    
    @GetMapping("/mobile-sign/{token}")
    public String showMobileSignPage(@PathVariable String token, Model model) {
        boolean valid = mobileSignTokenService.validateToken(token);
        boolean used = mobileSignTokenService.isTokenUsed(token);
        boolean expired = !used && !valid;
        Date expirationDate = mobileSignTokenService.getTokenExpirationDate(token);

        model.addAttribute("token", token);
        model.addAttribute("expired", expired);
        model.addAttribute("used", used);
        model.addAttribute("expiresAtEpochMillis", expirationDate != null ? expirationDate.getTime() : null);
        return "public/mobile-sign";
    }

    @GetMapping(value = "/mobile-sign/{token}/qrcode.png", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> showMobileSignQrCode(@PathVariable String token) throws Exception {
        if (!mobileSignTokenService.tokenExists(token)) {
            return ResponseEntity.notFound().build();
        }

        String mobileSignUrl = buildMobileSignUrl(token);
        BitMatrix qrCode = new MultiFormatWriter().encode(mobileSignUrl, BarcodeFormat.QR_CODE, 200, 200);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(qrCode, "PNG", outputStream);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(outputStream.toByteArray());
    }

    @PostMapping("/mobile-sign/{token}/preview")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveSignaturePreview(
            @PathVariable String token,
            @RequestParam(value = "signImageBase64", required = false) String signImageBase64) {

        Map<String, Object> response = new HashMap<>();

        if (mobileSignTokenService.validateToken(token)) {
            boolean success = mobileSignTokenService.saveSignaturePreview(token, signImageBase64);
            if (success) {
                response.put("success", true);
                response.put("message", "Signature transmise. Revenez sur votre ordinateur pour la vérifier, puis utilisez le bouton d'enregistrement si elle vous convient.");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Erreur lors de la transmission de la signature");
                return ResponseEntity.badRequest().body(response);
            }
        } else {
            response.put("success", false);
            response.put("message", "Token invalide ou expiré. Veuillez générer un nouveau QR code.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/mobile-sign/{token}/finish")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> finishSignaturePreview(@PathVariable String token) {
        Map<String, Object> response = new HashMap<>();

        if (mobileSignTokenService.markFinished(token)) {
            response.put("success", true);
            response.put("message", "Signature confirmée.");
            return ResponseEntity.ok(response);
        }

        response.put("success", false);
        response.put("message", "Token invalide ou expiré.");
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/mobile-sign/{token}/preview")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSignaturePreview(@PathVariable String token) {
        Map<String, Object> response = new HashMap<>();
        String signImageBase64 = mobileSignTokenService.getPendingSignaturePreview(token);

        if (signImageBase64 == null) {
            response.put("success", false);
            response.put("message", "Aucune signature temporaire n'est disponible pour ce lien.");
            return ResponseEntity.badRequest().body(response);
        }

        response.put("success", true);
        response.put("signImageBase64", signImageBase64);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mobile-sign/{token}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkTokenStatus(@PathVariable String token) {
        Map<String, Object> response = new HashMap<>();
        boolean valid = mobileSignTokenService.validateToken(token);
        boolean used = mobileSignTokenService.isTokenUsed(token);
        boolean expired = mobileSignTokenService.isTokenExpired(token);
        Date expirationDate = mobileSignTokenService.getTokenExpirationDate(token);

        response.put("exists", mobileSignTokenService.tokenExists(token));
        response.put("valid", valid);
        response.put("used", used);
        response.put("expired", expired);
        response.put("previewAvailable", mobileSignTokenService.hasPendingSignaturePreview(token));
        response.put("previewTimestamp", mobileSignTokenService.getPendingSignaturePreviewTimestamp(token));
        response.put("finished", mobileSignTokenService.isTokenFinished(token));
        if (expirationDate != null) {
            response.put("expiresAtEpochMillis", expirationDate.getTime());
        }

        if (valid) {
            response.put("message", "Token valide");
        } else if (used) {
            response.put("message", "Signature enregistrée");
        } else if (expired) {
            response.put("message", "Token expiré");
        } else {
            response.put("message", "Token invalide");
        }

        return ResponseEntity.ok(response);
    }

    private String buildMobileSignUrl(String token) {
        String baseUrl = globalProperties.getRootUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "http://" + globalProperties.getDomain();
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/public/mobile-sign/" + token;
    }
}
