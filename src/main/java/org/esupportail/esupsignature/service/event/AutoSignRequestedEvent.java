package org.esupportail.esupsignature.service.event;

public class AutoSignRequestedEvent {

    private final Long signBookId;
    private final String userEppn;
    private final String authUserEppn;

    public AutoSignRequestedEvent(Long signBookId, String userEppn, String authUserEppn) {
        this.signBookId = signBookId;
        this.userEppn = userEppn;
        this.authUserEppn = authUserEppn;
    }

    public Long getSignBookId() {
        return signBookId;
    }

    public String getUserEppn() {
        return userEppn;
    }

    public String getAuthUserEppn() {
        return authUserEppn;
    }
}
