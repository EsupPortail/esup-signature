package org.esupportail.esupsignature.dto.ws;

import java.util.ArrayList;
import java.util.List;

public class SignRequestStepsWsDto {

    Integer stepNumber;
    String signType;
    Boolean allSignToComplete;
    List<RecipientsActionsWsDto> recipientsActions = new ArrayList<>();

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

    public List<RecipientsActionsWsDto> getRecipientsActions() {
        return recipientsActions;
    }

    public void setRecipientsActions(List<RecipientsActionsWsDto> recipientsActions) {
        this.recipientsActions = recipientsActions;
    }
}
