package org.esupportail.esupsignature.web;

import eu.europa.esig.dss.validation.reports.Reports;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.io.FileUtils;
import org.esupportail.esupsignature.dss.service.XSLTService;
import org.esupportail.esupsignature.entity.AuditTrail;
import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.service.AuditTrailService;
import org.esupportail.esupsignature.service.LogService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.esupportail.esupsignature.service.utils.sign.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller

@RequestMapping("/public")
public class PublicController {

    private final BuildProperties buildProperties;
    private final LogService logService;
    private final SignRequestService signRequestService;

    private final AuditTrailService auditTrailService;

    private final FileService fileService;

    private final UserService userService;

    private final XSLTService xsltService;

    private final PreAuthorizeService preAuthorizeService;
    private final ValidationService validationService;
    private final SignService signService;

    public PublicController(@Autowired(required = false) BuildProperties buildProperties, LogService logService, SignRequestService signRequestService, AuditTrailService auditTrailService, FileService fileService, UserService userService, XSLTService xsltService, PreAuthorizeService preAuthorizeService, ValidationService validationService, SignService signService) {
        this.buildProperties = buildProperties;
        this.logService = logService;
        this.signRequestService = signRequestService;
        this.auditTrailService = auditTrailService;
        this.fileService = fileService;
        this.userService = userService;
        this.xsltService = xsltService;
        this.preAuthorizeService = preAuthorizeService;
        this.validationService = validationService;
        this.signService = signService;
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
                model.addAttribute("viewAccess", preAuthorizeService.checkUserViewRights(signRequest.get(), eppn, eppn));
            }
            if(auditTrail.getAuditSteps().stream().anyMatch(as -> as.getSignCertificat() != null && !as.getSignCertificat().isEmpty())) {
                Reports reports = signService.validate(signRequest.get().getId());
                model.addAttribute("simpleReport", xsltService.generateShortReport(reports.getXmlSimpleReport()));
            }
        }
    }
}
