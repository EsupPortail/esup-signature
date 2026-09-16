package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.SignWith;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
public class SignWithService {

    private final UserService userService;
    private final CertificatService certificatService;
    private final GlobalProperties globalProperties;

    public SignWithService(UserService userService, CertificatService certificatService, GlobalProperties globalProperties) {
        this.userService = userService;
        this.certificatService = certificatService;
        this.globalProperties = globalProperties;
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
            if(workflow != null) {
                List<WorkflowStep> workflowSteps = workflow.getWorkflowSteps().stream().filter(ws -> workflow.getWorkflowSteps().indexOf(ws) > stepNumber - 1).toList();
                if (!workflowSteps.isEmpty() && form.getFields().stream().anyMatch(f -> new HashSet<>(f.getWorkflowSteps()).containsAll(workflowSteps))) {
                    signWiths.removeIf(signWith -> signWith.getValue() > 2);
                }
            }
        }
        if(signRequest.getOriginalDocuments().size() > 1 || (!signRequest.getOriginalDocuments().isEmpty() && !signRequest.getOriginalDocuments().get(0).isPdf())) {
            signWiths.remove(SignWith.imageStamp);
        }
        if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null) {
            LiveWorkflowStep currentLiveWorkflowStep = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep();
            signWiths.removeIf(signWith -> signWith.getValue() > currentLiveWorkflowStep.getMaxSignLevel().getValue() || signWith.getValue() < currentLiveWorkflowStep.getMinSignLevel().getValue());
            if(currentLiveWorkflowStep.getMinSignLevel().equals(SignLevel.qualified)
                    && certificatService.getCheckedSealCertificates().stream().noneMatch(v -> v.eIDasValidity)) {
                signWiths.remove(SignWith.sealCert);
            }
            if(currentLiveWorkflowStep.getMaxSignLevel().equals(SignLevel.advanced)
                    && certificatService.getCheckedSealCertificates().stream().anyMatch(v -> !v.eIDasValidity)) {
                signWiths.add(SignWith.sealCert);
            }
        }
        if(certificatService.getAuthorizedSealCertificatProperties(userEppn, isAlreadyCertSign).isEmpty()) {
            signWiths.remove(SignWith.sealCert);
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
        if(checkSealCertificat(userEppn, false, isAlreadyCertSign)) {
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
        return checkSealCertificat(userEppn, force, false);
    }

    public boolean checkSealCertificat(String userEppn, boolean force, boolean isAlreadyCertSign) {
        if (!certificatService.getAuthorizedSealCertificatProperties(userEppn, isAlreadyCertSign).isEmpty()) {
            return true;
        }
        return false;
    }
}
