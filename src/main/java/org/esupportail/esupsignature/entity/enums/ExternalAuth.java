package org.esupportail.esupsignature.entity.enums;

public enum ExternalAuth {
    open, sms, proconnect, franceconnect;

    public static ExternalAuth[] enabledValues() {
        return values();
    }

}
