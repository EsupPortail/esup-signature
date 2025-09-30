package org.esupportail.esupsignature.service.utils.sign;

import eu.europa.esig.dss.alert.SilentOnStatusAlert;
import eu.europa.esig.dss.enumerations.Indication;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.TokenExtractionStrategy;
import eu.europa.esig.dss.enumerations.ValidationLevel;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.model.x509.revocation.ocsp.OCSP;
import eu.europa.esig.dss.signature.AbstractSignatureParameters;
import eu.europa.esig.dss.spi.policy.SignaturePolicyProvider;
import eu.europa.esig.dss.spi.validation.CertificateVerifier;
import eu.europa.esig.dss.spi.x509.revocation.RevocationToken;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;
import org.esupportail.esupsignature.dss.DssUtilsService;
import org.esupportail.esupsignature.dss.model.AbstractSignatureForm;
import org.esupportail.esupsignature.dss.model.DssMultipartFile;
import org.esupportail.esupsignature.dss.model.SignatureDocumentForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class ValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

    private final CertificateVerifier certificateVerifier;
    private final DssUtilsService dssUtilsService;
    private final SignaturePolicyProvider signaturePolicyProvider;
    private final org.springframework.core.io.Resource defaultPolicy;

    public ValidationService(CertificateVerifier certificateVerifier, DssUtilsService dssUtilsService, SignaturePolicyProvider signaturePolicyProvider, org.springframework.core.io.Resource defaultPolicy) {
        this.certificateVerifier = certificateVerifier;
        this.dssUtilsService = dssUtilsService;
        this.signaturePolicyProvider = signaturePolicyProvider;
        this.defaultPolicy = defaultPolicy;
    }

    /**
     * Validation DSS d'un document.
     *
     * @param docInputStream Flux de données contenant le document à valider.
     * @param signInputStream Flux de données contenant la signature associée au document. Peut être null si celle-ci est intégrée au document.
     * @return Un objet Reports contenant les résultats de la validation, ou null si la validation échoue ou si les données fournies sont invalides.
     */
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
            documentValidator.setValidationLevel(ValidationLevel.LONG_TERM_DATA);
            documentValidator.setSignaturePolicyProvider(signaturePolicyProvider);
            documentValidator.setIncludeSemantics(true);
            InputStream is = defaultPolicy.getInputStream();
            return documentValidator.validateDocument(is);
        } catch (Exception e) {
            logger.warn("Unable to validate document : " + e.getMessage(), e);
        }
        return null;
    }

    public void checkRevocation(AbstractSignatureForm signatureDocumentForm, CertificateToken certificateToken, AbstractSignatureParameters<?> parameters) {
        RevocationToken<OCSP> revocationToken = null;
        boolean containsBadSignature = false;
        int revocationCheckAttempt = 10;
        while (revocationCheckAttempt > 0) {
            try {
                Reports reports = validate(new ByteArrayInputStream(((SignatureDocumentForm) signatureDocumentForm).getDocumentToSign().getBytes()), null);
                for (String signatureId : reports.getSimpleReport().getSignatureIdList()) {
                    if (!reports.getSimpleReport().getIndication(signatureId).equals(Indication.TOTAL_FAILED)) {
                        containsBadSignature = true;
                        break;
                    }
                }
                revocationToken = certificateVerifier.getOcspSource().getRevocationToken(certificateToken, certificateToken);
                break;
            } catch (Exception e) {
                logger.warn("attempt " + (11 - revocationCheckAttempt) + " : revocation check fail " + e.getMessage());
                if (certificateVerifier.isCheckRevocationForUntrustedChains() && revocationCheckAttempt == 1) {
                    logger.warn("revocation check impossible, abort LTA signature");
                    //                    throw new EsupSignatureRuntimeException("Impossible de signer avec ce certificat. Détails : " + e.getMessage());
                }
            }
            revocationCheckAttempt--;
        }
        if(containsBadSignature
            || revocationToken == null
            || !certificateVerifier.getRevocationDataVerifier().isAcceptable(revocationToken)
            || (!certificateToken.isValidOn(new Date()) && certificateVerifier.getAlertOnExpiredCertificate() instanceof SilentOnStatusAlert)) {
            logger.warn("LT or LTA signature level not supported, switching to T level");
            logger.debug("containsBadSignature : " + containsBadSignature);
            logger.debug("revocationToken null : " + (revocationToken == null));
            logger.debug("revocationToken ok : " + (revocationToken != null && !certificateVerifier.getRevocationDataVerifier().isAcceptable(revocationToken)));
            logger.debug("certificat valid : " + (!certificateToken.isValidOn(new Date()) && certificateVerifier.getAlertOnExpiredCertificate() instanceof SilentOnStatusAlert));
            if(parameters.getSignatureLevel().name().contains("_LT") || parameters.getSignatureLevel().name().contains("_LTA")) {
                String newLevel = parameters.getSignatureLevel().name().replace("_LTA", "_T");
                newLevel = newLevel.replace("_LT", "_T");
                parameters.setSignatureLevel(SignatureLevel.valueOf(newLevel));
            }
        }
    }

}
