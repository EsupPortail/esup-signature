package org.esupportail.esupsignature.web;

import eu.europa.esig.dss.validation.reports.Reports;
import org.apache.commons.io.FileUtils;
import org.esupportail.esupsignature.dss.service.XSLTService;
import org.esupportail.esupsignature.entity.AuditTrail;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.service.AuditTrailService;
import org.esupportail.esupsignature.service.LogService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@Controller

@RequestMapping("/public")
public class PublicController {

    private final BuildProperties buildProperties;

    @Resource
    private LogService logService;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private AuditTrailService auditTrailService;

    @Resource
    private FileService fileService;

    @Resource
    private UserService userService;

    @Resource
    private XSLTService xsltService;

    @Resource
    private PreAuthorizeService preAuthorizeService;

    public PublicController(@Autowired(required = false) BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @GetMapping(value = "/control/{token}")
    public String control(@PathVariable String token, Model model) throws EsupSignatureFsException, IOException {
        SignRequest signRequest = signRequestService.getSignRequestByToken(token);
        if(signRequest == null) {
            return "error";
        }
        AuditTrail auditTrail = auditTrailService.getAuditTrailByToken(signRequest.getToken());
        Document signedDocument = signRequestService.getLastSignedFile(signRequest.getId());
        model.addAttribute("size", FileUtils.byteCountToDisplaySize(signedDocument.getSize()));
        model.addAttribute("auditTrail", auditTrail);
        if(auditTrail != null && auditTrail.getAuditSteps().stream().anyMatch(as -> as.getSignCertificat() != null && !as.getSignCertificat().isEmpty())) {
            Reports reports = signRequestService.validate(signRequest.getId());
            model.addAttribute("simpleReport", xsltService.generateShortReport(reports.getXmlSimpleReport()));
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getName().equals("anonymousUser")) {
            String eppn = userService.tryGetEppnFromLdap(auth);
            if(eppn != null && userService.getUserByEppn(eppn) != null && auditTrail != null) {
                model.addAttribute("signRequest", signRequest);
                setControlValues(model, signRequest, auditTrail, eppn);
            }
        }
        if (buildProperties != null) {
            model.addAttribute("version", buildProperties.getVersion());
        }
        model.addAttribute("token", token);
        return "public/control";
    }

    @PostMapping(value = "/control/{token}")
    public String checkFile(@PathVariable String token, @RequestParam(value = "multipartFile") MultipartFile multipartFile,
                            Model model, HttpSession httpSession) throws IOException {
        String checksum = fileService.getFileChecksum(multipartFile.getInputStream());
        AuditTrail auditTrail = auditTrailService.getAuditTrailFromCheksum(checksum);
        if(auditTrail != null && auditTrail.getToken().equals(token)) {
            SignRequest signRequest = signRequestService.getSignRequestByToken(token);
            if(signRequest != null) {
                model.addAttribute("signRequest", signRequest);
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
            if(signRequest != null) {
                setControlValues(model, signRequest, auditTrail, eppn);
            }
            if(auditTrail.getAuditSteps().stream().anyMatch(as -> as.getSignCertificat() != null && !as.getSignCertificat().isEmpty())) {
                Reports reports = signRequestService.validate(signRequest.getId());
                model.addAttribute("simpleReport", xsltService.generateShortReport(reports.getXmlSimpleReport()));
            }
        } else {
            model.addAttribute("error", true);
            return "public/control";
        }
        return "public/control";
    }

    private void setControlValues(Model model, SignRequest signRequest, AuditTrail auditTrail, String eppn) {
        List<Log> logs = logService.getFullBySignRequest(signRequest.getId());
        model.addAttribute("logs", logs);
        model.addAttribute("usersHasSigned", signRequestService.checkUserResponseSigned(signRequest));
        model.addAttribute("usersHasRefused", signRequestService.checkUserResponseRefused(signRequest));
        model.addAttribute("signRequest", signRequest);
        if(auditTrail.getDocumentSize() != null) {
            model.addAttribute("size", FileUtils.byteCountToDisplaySize(auditTrail.getDocumentSize()));
        }
        if(eppn != null) {
            model.addAttribute("viewAccess", preAuthorizeService.checkUserViewRights(signRequest, eppn, eppn));
        }
    }
}
