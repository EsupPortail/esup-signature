package org.esupportail.esupsignature.dto.json;

import java.util.ArrayList;
import java.util.List;

public class SignRequestStepsDto {

    Integer stepNumber;
    String signType;
    Boolean allSignToComplete;
    List<RecipientsActionsDto> recipientsActions = new ArrayList<>();

    public Integer getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(Integer stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getSignType() {
        return signType;
    }

    public void setSignType(String signType) {
        this.signType = signType;
    }

    public Boolean getAllSignToComplete() {
        return allSignToComplete;
    }

    public void setAllSignToComplete(Boolean allSignToComplete) {
        this.allSignToComplete = allSignToComplete;
    }

    public List<RecipientsActionsDto> getRecipientsActions() {
        return recipientsActions;
    }

    public void setRecipientsActions(List<RecipientsActionsDto> recipientsActions) {
        this.recipientsActions = recipientsActions;
    }
}
