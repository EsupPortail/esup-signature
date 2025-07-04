package org.esupportail.esupsignature.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.sign.SignProperties;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignWith;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@EnableConfigurationProperties({GlobalProperties.class, SignProperties.class})
public class SignWithService {

    private final UserService userService;
    private final CertificatService certificatService;
    private final GlobalProperties globalProperties;

    private static LoadingCache<String, Boolean> sealCertOKCache;

    public SignWithService(UserService userService, CertificatService certificatService, GlobalProperties globalProperties) {
        this.userService = userService;
        this.certificatService = certificatService;
        this.globalProperties = globalProperties;
        sealCertOKCache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build(new CacheLoader<>() {
            @Override
            public @NotNull Boolean load(@NotNull String s) {
                return false;
            }
        });
    }

    @Transactional
    public List<SignWith> getAuthorizedSignWiths(String userEppn, SignRequest signRequest, boolean isAlreadyCertSign) {
        List<SignWith> signWiths = getAuthorizedSignWiths(userEppn, isAlreadyCertSign);
        if(signRequest.getCurrentSignType() != null) {
            signWiths.removeIf(signWith -> signWith.getValue() < signRequest.getCurrentSignType().getValue());
        }
        if(signRequest.getData() != null && signRequest.getData().getForm() != null) {
            int stepNumber = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber();
            Form form = signRequest.getData().getForm();
            Workflow workflow = signRequest.getParentSignBook().getLiveWorkflow().getWorkflow();
            List<WorkflowStep> workflowSteps = workflow.getWorkflowSteps().stream().filter(ws -> workflow.getWorkflowSteps().indexOf(ws) > stepNumber - 1).toList();
            if(!workflowSteps.isEmpty() && form.getFields().stream().anyMatch(f -> new HashSet<>(f.getWorkflowSteps()).containsAll(workflowSteps))) {
                signWiths.removeIf(signWith -> signWith.getValue() > 2);
            }
        }
        if(signRequest.getOriginalDocuments().size() > 1 || (!signRequest.getOriginalDocuments().isEmpty() && !signRequest.getOriginalDocuments().get(0).getContentType().equals("application/pdf"))) {
            signWiths.remove(SignWith.imageStamp);
        }
        if(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep() != null) {
            signWiths.removeIf(signWith -> signWith.getValue() > signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getMaxSignLevel().getValue() || signWith.getValue() < signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getMinSignLevel().getValue());
        }
        return signWiths;
    }

    public List<SignWith> getAuthorizedSignWiths(String userEppn, boolean isAlreadyCertSign) {
        User user = userService.getByEppn(userEppn);
        List<SignWith> signWiths = new ArrayList<>(List.of(SignWith.values()));
        if(isAlreadyCertSign) signWiths.remove(SignWith.imageStamp);
        if(globalProperties.getDisableCertStorage() || user.getKeystore() == null) {
            signWiths.remove(SignWith.userCert);
        }
        signWiths.remove(SignWith.sealCert);
        if(checkSealCertificat(userEppn, false) && (isAlreadyCertSign || user.getRoles().contains("ROLE_SEAL"))) {
            signWiths.add(SignWith.sealCert);
        }
        if(certificatService.getCertificatByUser(user.getEppn()).isEmpty()) {
            signWiths.remove(SignWith.groupCert);
        }
        if(globalProperties.getOpenXPKIServerUrl() == null) {
            signWiths.remove(SignWith.openPkiCert);
        }
        List<SignWith> toRemoveSignWiths = new ArrayList<>();
        for (SignWith signWith : signWiths) {
            if(!globalProperties.getAuthorizedSignTypes().contains(signWith)) {
                toRemoveSignWiths.add(signWith);
            }
        }
        signWiths.removeAll(toRemoveSignWiths);
        signWiths.remove(SignWith.autoCert);
        return signWiths;
    }

    public boolean checkSealCertificat(String userEppn, boolean force) {
        User user = userService.getByEppn(userEppn);
        if(
                (user.getRoles().contains("ROLE_SEAL")
                    || (!user.getUserType().equals(UserType.external) && globalProperties.getSealAuthorizedForSignedFiles())
                    || (globalProperties.getSealForExternals() && user.getUserType().equals(UserType.external))
                )
                && StringUtils.hasText(globalProperties.getSealCertificatPin())
                && (
                    (globalProperties.getSealCertificatType() != null && globalProperties.getSealCertificatType().equals(GlobalProperties.TokenType.PKCS11) && StringUtils.hasText(globalProperties.getSealCertificatDriver()))
                    ||
                    (globalProperties.getSealCertificatType() != null && globalProperties.getSealCertificatType().equals(GlobalProperties.TokenType.OPENSC))
                    ||
                    (globalProperties.getSealCertificatType() != null && globalProperties.getSealCertificatType().equals(GlobalProperties.TokenType.PKCS12) && StringUtils.hasText(globalProperties.getSealCertificatFile()))
                )
        ) {
            if(Boolean.TRUE.equals(sealCertOKCache.getIfPresent("sealOK"))) {
                return true;
            } else {
                if (!certificatService.getSealCertificats().isEmpty()) {
                    sealCertOKCache.put("sealOK", true);
                    return true;
                } else {
                    return false;
                }
            }
        }
        return force;
    }
}
