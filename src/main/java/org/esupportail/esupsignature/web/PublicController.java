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

    private final SignService signService;

    private final XSLTService xsltService;

    private final PreAuthorizeService preAuthorizeService;

    public PublicController(@Autowired(required = false) BuildProperties buildProperties, LogService logService, SignRequestService signRequestService, AuditTrailService auditTrailService, FileService fileService, UserService userService, SignService signService, XSLTService xsltService, PreAuthorizeService preAuthorizeService) {
        this.buildProperties = buildProperties;
        this.logService = logService;
        this.signRequestService = signRequestService;
        this.auditTrailService = auditTrailService;
        this.fileService = fileService;
        this.userService = userService;
        this.signService = signService;
        this.xsltService = xsltService;
        this.preAuthorizeService = preAuthorizeService;
    }

    @GetMapping(value = "/control/{token}")
    public String control(@PathVariable String token, Model model) throws EsupSignatureFsException, IOException {
        AuditTrail auditTrail = auditTrailService.getAuditTrailByToken(token);
        if(auditTrail == null) {
            return "error";
        }
        model.addAttribute("auditTrailChecked", false);
        model.addAttribute("size", auditTrail.getDocumentSize());
        model.addAttribute("auditTrail", auditTrail);
        Optional<SignRequest> signRequest = signRequestService.getSignRequestByToken(token);
        if(signRequest.isPresent()) {
            if (auditTrail.getAuditSteps().stream().anyMatch(as -> as.getSignCertificat() != null && !as.getSignCertificat().isEmpty())) {
                Reports reports = signService.validate(signRequest.get().getId());
                if (reports != null) {
                    model.addAttribute("simpleReport", xsltService.generateShortReport(reports.getXmlSimpleReport()));
                } else {
                    model.addAttribute("signRequest", signRequest.get());
                }
            } else{
                model.addAttribute("signRequest", signRequest.get());
            }
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getName().equals("anonymousUser")) {
            String eppn = userService.tryGetEppnFromLdap(auth);
            if(eppn != null && userService.getByEppn(eppn) != null) {
                model.addAttribute("signRequest", signRequest.get());
                setControlValues(model, signRequest.get(), auditTrail, eppn);
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
        String checksum = fileService.getFileChecksum(multipartFile.getInputStream());
        AuditTrail auditTrail = auditTrailService.getAuditTrailFromCheksum(checksum);
        if(auditTrail != null && auditTrail.getToken().equals(token)) {
            model.addAttribute("auditTrailChecked", true);
            List<Log> logs = logService.getFullByToken(token);
            model.addAttribute("logs", logs);
            Optional<SignRequest> signRequest = signRequestService.getSignRequestByToken(token);
            if(signRequest.isPresent()) {
                model.addAttribute("signRequest", signRequest.get());
            }
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
            if(signRequest.isPresent()) {
                setControlValues(model, signRequest.get(), auditTrail, eppn);
                if(auditTrail.getAuditSteps().stream().anyMatch(as -> as.getSignCertificat() != null && !as.getSignCertificat().isEmpty())) {
                    Reports reports = signService.validate(signRequest.get().getId());
                    model.addAttribute("simpleReport", xsltService.generateShortReport(reports.getXmlSimpleReport()));
                }
            }
        } else {
            model.addAttribute("error", true);
            return "public/control";
        }
        return "public/control";
    }

    private void setControlValues(Model model, SignRequest signRequest, AuditTrail auditTrail, String eppn) {
        model.addAttribute("usersHasSigned", auditTrailService.checkUserResponseSigned(signRequest));
        model.addAttribute("usersHasRefused", auditTrailService.checkUserResponseRefused(signRequest));
        model.addAttribute("signRequest", signRequest);
        if(auditTrail.getDocumentSize() != null) {
            model.addAttribute("size", FileUtils.byteCountToDisplaySize(auditTrail.getDocumentSize()));
        }
        if(eppn != null) {
            model.addAttribute("viewAccess", preAuthorizeService.checkUserViewRights(signRequest, eppn, eppn));
        }
    }
}
