package org.esupportail.esupsignature.service.utils.sign;

import eu.europa.esig.dss.AbstractSignatureParameters;
import eu.europa.esig.dss.enumerations.Indication;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.TokenExtractionStrategy;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.model.x509.revocation.ocsp.OCSP;
import eu.europa.esig.dss.spi.x509.revocation.RevocationToken;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.SignaturePolicyProvider;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.executor.ValidationLevel;
import eu.europa.esig.dss.validation.reports.Reports;
import jakarta.annotation.Resource;
import org.esupportail.esupsignature.dss.DssUtilsService;
import org.esupportail.esupsignature.dss.model.AbstractSignatureForm;
import org.esupportail.esupsignature.dss.model.DssMultipartFile;
import org.esupportail.esupsignature.dss.model.SignatureDocumentForm;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

@Service
public class ValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

    private final CertificateVerifier certificateVerifier;

    @Resource
    protected FileService fileService;

    @Resource
    protected DssUtilsService dssUtilsService;

    @Resource
    protected SignaturePolicyProvider signaturePolicyProvider;

    @Resource
    private org.springframework.core.io.Resource defaultPolicy;

    public ValidationService(CertificateVerifier certificateVerifier) {
        this.certificateVerifier = certificateVerifier;
    }

    public Reports validate(InputStream docInputStream, InputStream signInputStream) {
        try {
            List<DSSDocument> detachedContents = new ArrayList<>();
            SignedDocumentValidator documentValidator;
            if(signInputStream != null && signInputStream.available() > 0) {
                detachedContents.add(dssUtilsService.toDSSDocument(new DssMultipartFile("doc", "doc", null, docInputStream)));
                documentValidator = SignedDocumentValidator.fromDocument(Objects.requireNonNull(dssUtilsService.toDSSDocument(new DssMultipartFile("sign", "sign", null, signInputStream))));
                documentValidator.setValidationLevel(ValidationLevel.LONG_TERM_DATA);
                documentValidator.setDetachedContents(detachedContents);
            } else {
                DSSDocument dssDocument = dssUtilsService.toDSSDocument(new DssMultipartFile("doc", "doc", null, docInputStream));
                if(dssDocument != null) {
                    documentValidator = SignedDocumentValidator.fromDocument(dssDocument);
                } else {
                    return null;
                }
            }
            logger.debug("validate with : " + documentValidator.getClass());
            if(certificateVerifier != null) {
                documentValidator.setCertificateVerifier(certificateVerifier);
            }
            documentValidator.setSignaturePolicyProvider(new SignaturePolicyProvider());
            documentValidator.setTokenExtractionStrategy(TokenExtractionStrategy.NONE);
            documentValidator.setLocale(Locale.FRENCH);
            documentValidator.setValidationLevel(ValidationLevel.LONG_TERM_DATA);
            documentValidator.setSignaturePolicyProvider(signaturePolicyProvider);
            documentValidator.setIncludeSemantics(true);
            InputStream is = defaultPolicy.getInputStream();
            return documentValidator.validateDocument(is);
        } catch (Exception e) {
            logger.warn("Unable to validate document : " + e.getMessage());
        }
        return null;
    }

    public void checkRevocation(AbstractSignatureForm signatureDocumentForm, CertificateToken certificateToken, AbstractSignatureParameters<?> parameters) {
        RevocationToken<OCSP> revocationToken = null;
        boolean containsBadSignature = false;
        try {
            Reports reports = validate(new ByteArrayInputStream(((SignatureDocumentForm) signatureDocumentForm).getDocumentToSign().getBytes()), null);
            for(String signatureId : reports.getSimpleReport().getSignatureIdList()) {
                if(!reports.getSimpleReport().getIndication(signatureId).equals(Indication.TOTAL_FAILED)) {
                    containsBadSignature = true;
                    break;
                }
            }
            revocationToken = certificateVerifier.getOcspSource().getRevocationToken(certificateToken, certificateToken);
        } catch (Exception e) {
            logger.warn("revocation check fail " + e.getMessage());
            if(certificateVerifier.isCheckRevocationForUntrustedChains()) {
                throw new EsupSignatureRuntimeException("Impossible de signer avec ce certificat. Détails : " + e.getMessage());
            }
        }
        if(containsBadSignature || revocationToken != null && !certificateVerifier.getRevocationDataVerifier().isAcceptable(revocationToken)
            || (!certificateToken.isValidOn(new Date()) && parameters.isSignWithExpiredCertificate())) {
            logger.warn("LT or LTA signature level not supported, switching to T level");
            if(parameters.getSignatureLevel().name().contains("_LT") || parameters.getSignatureLevel().name().contains("_LTA")) {
                String newLevel = parameters.getSignatureLevel().name().replace("_LTA", "_T");
                newLevel = newLevel.replace("_LT", "_T");
                parameters.setSignatureLevel(SignatureLevel.valueOf(newLevel));
            }
        }
    }

}
