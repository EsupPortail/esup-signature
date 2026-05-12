package org.esupportail.esupsignature.dto.page.admin;

public class AdminUiStatusDto {

    private Integer nbSessions;
    private Boolean dssStatus;

    public Integer getNbSessions() {
        return nbSessions;
    }

    public void setNbSessions(Integer nbSessions) {
        this.nbSessions = nbSessions;
    }

    public Boolean getDssStatus() {
        return dssStatus;
    }

    public void setDssStatus(Boolean dssStatus) {
        this.dssStatus = dssStatus;
    }

    public Integer nbSessions() {
        return nbSessions;
    }

    public Boolean dssStatus() {
        return dssStatus;
    }
}
