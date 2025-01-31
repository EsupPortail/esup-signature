package org.esupportail.esupsignature.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.XRLog;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.validation.reports.Reports;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.esupportail.esupsignature.repository.AuditStepRepository;
import org.esupportail.esupsignature.repository.AuditTrailRepository;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.sign.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.AbstractTemplateView;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.context.webmvc.SpringWebMvcThymeleafRequestContext;
import org.thymeleaf.spring6.naming.SpringContextVariableNames;
import org.thymeleaf.web.servlet.IServletWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class AuditTrailService {

    private static final Logger logger = LoggerFactory.getLogger(AuditTrailService.class);

    private final AuditTrailRepository auditTrailRepository;

    private final AuditStepRepository auditStepRepository;

    private final UserService userService;

    private final FileService fileService;

    private final LogService logService;

    private final TemplateEngine templateEngine;

    private final ValidationService validationService;

    public AuditTrailService(AuditTrailRepository auditTrailRepository, AuditStepRepository auditStepRepository, UserService userService, FileService fileService, LogService logService, TemplateEngine templateEngine, ValidationService validationService) {
        this.auditTrailRepository = auditTrailRepository;
        this.auditStepRepository = auditStepRepository;
        this.userService = userService;
        this.fileService = fileService;
        this.logService = logService;
        this.templateEngine = templateEngine;
        this.validationService = validationService;
    }

    public AuditTrail create(String token) {
        AuditTrail auditTrail = new AuditTrail();
        auditTrail.setToken(token);
        auditTrailRepository.save(auditTrail);
        return auditTrail;
    }

    public void  closeAuditTrail(String token, Document document, InputStream inputStream) throws IOException {
        AuditTrail auditTrail = auditTrailRepository.findByToken(token);
        auditTrail.setDocumentCheckSum(fileService.getFileChecksum(inputStream));
        auditTrail.setDocumentName(document.getFileName());
        auditTrail.setDocumentType(document.getContentType());
        auditTrail.setDocumentSize(document.getSize());
        auditTrail.setDocumentId(document.getId().toString());
    }

    public void addAuditStep(String token, String userEppn, String signId, String timeStampId, String certificat, String timeStampCertificat, Date timeStampDate, Boolean allScrolled, Integer page, Integer posX, Integer posY) {
        AuditTrail auditTrail = auditTrailRepository.findByToken(token);
        AuditStep auditStep = createAuditStep(userEppn, signId, timeStampId, certificat, timeStampCertificat, timeStampDate, allScrolled, page, posX, posY);
        auditTrail.getAuditSteps().add(auditStep);
    }

    public AuditStep createAuditStep(String userEppn, String signId, String timeStampId, String certificat, String timeStampCertificat, Date timeStampDate, Boolean allScrolled, Integer page, Integer posX, Integer posY) {
        User user = userService.getByEppn(userEppn);
        AuditStep auditStep = new AuditStep();
        auditStep.setName(user.getName());
        auditStep.setFirstname(user.getFirstname());
        auditStep.setEmail(user.getEmail());
        auditStep.setPage(page);
        auditStep.setPosX(posX);
        auditStep.setPosY(posY);
        auditStep.setLogin(user.getEppn());
        auditStep.setSignId(signId);
        auditStep.setTimeStampId(timeStampId);
        auditStep.setSignCertificat(certificat);
        auditStep.setTimeStampCertificat(timeStampCertificat);
        auditStep.setTimeStampDate(timeStampDate);
        auditStep.setAllScrolled(allScrolled);
        Map<String, String> authenticationDetails = new HashMap<>();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication != null) {
            authenticationDetails.put("name", authentication.getName());
            if(authentication.getCredentials() != null){
                authenticationDetails.put("credential", authentication.getCredentials().toString());
            }
            authenticationDetails.put("type", authentication.getClass().getTypeName());
        }
        auditStep.setAuthenticationDetails(authenticationDetails);
        auditStepRepository.save(auditStep);
        return auditStep;
    }

    @Transactional
    public AuditTrail getAuditTrailFromCheksum(String checkSum) {
        return auditTrailRepository.findByDocumentCheckSum(checkSum);
    }

    @Transactional
    public AuditTrail getAuditTrailByToken(String token) {
        return auditTrailRepository.findByToken(token);
    }

    @Transactional
    public ByteArrayOutputStream generateAuditTrailPdf(SignRequest signRequest, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        AuditTrail auditTrail = getAuditTrailByToken(signRequest.getToken());
        RequestContext requestContext = new RequestContext(httpServletRequest, httpServletResponse);
        Map<String, Object> vars = new HashMap<>();
        vars.put("auditTrail", auditTrail);
        if(auditTrail != null && auditTrail.getDocumentSize() != null) {
            vars.put("size", FileUtils.byteCountToDisplaySize(auditTrail.getDocumentSize()));
        } else {
            vars.put("size", FileUtils.byteCountToDisplaySize(0));
        }
        List<Log> logs = logService.getFullBySignRequest(signRequest.getId());
        vars.put("logs", logs);
        vars.put("usersHasSigned", checkUserResponseSigned(signRequest));
        vars.put("usersHasRefused", checkUserResponseRefused(signRequest));
        vars.put("signRequest", signRequest);
        vars.put("print", true);

        vars.put(AbstractTemplateView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE, requestContext);
        vars.put(SpringContextVariableNames.SPRING_REQUEST_CONTEXT, requestContext);
        vars.put(SpringContextVariableNames.THYMELEAF_REQUEST_CONTEXT, new SpringWebMvcThymeleafRequestContext(requestContext, httpServletRequest));
        JakartaServletWebApplication jakartaServletWebApplication = JakartaServletWebApplication.buildApplication(httpServletRequest.getServletContext());
        IServletWebExchange iServletWebExchange = jakartaServletWebApplication.buildExchange(httpServletRequest, httpServletResponse);
        final WebContext webContext = new WebContext(iServletWebExchange, Locale.FRENCH);
        for (final Map.Entry<String, Object> entry : vars.entrySet()) {
            webContext.setVariable(entry.getKey(), entry.getValue());
        }
        String html = templateEngine.process("public/control", webContext);
        XRLog.listRegisteredLoggers().forEach(logger -> XRLog.setLevel(logger, java.util.logging.Level.OFF));
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(html, findBaseUrl(httpServletRequest));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        builder.toStream(outputStream);
        builder.run();
        byte[] bytes = outputStream.toByteArray();
        IOUtils.copy(new ByteArrayInputStream(bytes), outputStream);
        return outputStream;
    }

    String findBaseUrl(final HttpServletRequest request) {
        final String baseUrl;
        if (request == null) {
            baseUrl = "";
        } else {
            StringBuffer url = request.getRequestURL();
            String uri = request.getRequestURI();
            String ctx = request.getContextPath();
            baseUrl = url.substring(0,
                    url.length() - uri.length() + ctx.length())
                    + "/";
        }
        return baseUrl;
    }


    public List<User> checkUserResponseSigned(SignRequest signRequest) {
        List<User> usersHasSigned = new ArrayList<>();
        for(Map.Entry<Recipient, Action> recipientActionEntry : signRequest.getRecipientHasSigned().entrySet()) {
            if (recipientActionEntry.getValue().getActionType().equals(ActionType.signed)) {
                usersHasSigned.add(recipientActionEntry.getKey().getUser());
            }
        }
        return usersHasSigned;
    }

    public List<User> checkUserResponseRefused(SignRequest signRequest) {
        List<User> usersHasRefused = new ArrayList<>();
        for(Map.Entry<Recipient, Action> recipientActionEntry : signRequest.getRecipientHasSigned().entrySet()) {
            if (recipientActionEntry.getValue().getActionType().equals(ActionType.refused)) {
                usersHasRefused.add(recipientActionEntry.getKey().getUser());
            }
        }
        return usersHasRefused;
    }

    public void createSignAuditStep(SignRequest signRequest, String userEppn, Document signedDocument, boolean isViewed) {
        DiagnosticData diagnosticData;
        Reports reports;
        reports = validationService.validate(signedDocument.getInputStream(), null);
        diagnosticData = reports.getDiagnosticData();
        String signId = new ArrayList<>(diagnosticData.getAllSignatures()).get(diagnosticData.getAllSignatures().size() - 1).getId();
        String timestampId = "no timestamp found";
        String certificat = new ArrayList<>(diagnosticData.getAllSignatures()).get(diagnosticData.getAllSignatures().size() - 1).getSigningCertificate().getId();
        String timestamp = "no timestamp found";
        if(!diagnosticData.getTimestampList().isEmpty()) {
            timestampId = diagnosticData.getTimestampList().get(0).getId();
            timestamp = diagnosticData.getTimestampList().get(0).getSigningCertificate().getId();
        }
        if (!signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().isEmpty()) {
            SignRequestParams signRequestParams = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().get(0);
            addAuditStep(signRequest.getToken(), userEppn, signId, timestampId, certificat, timestamp, reports.getSimpleReport().getBestSignatureTime(reports.getSimpleReport().getFirstSignatureId()), isViewed, signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos());
        } else {
            addAuditStep(signRequest.getToken(), userEppn, signId, timestampId, certificat, timestamp, reports.getSimpleReport().getBestSignatureTime(reports.getSimpleReport().getFirstSignatureId()), isViewed, 0, 0, 0);
        }
    }
}
