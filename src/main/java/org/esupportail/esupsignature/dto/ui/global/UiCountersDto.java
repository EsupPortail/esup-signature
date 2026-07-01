package org.esupportail.esupsignature.dto.ui.global;

public class UiCountersDto implements java.io.Serializable {

    private Long nbSignRequests;
    private Long nbToSign;
    private Long nbDeleted;
    private Integer reportNumber;
    private Integer managedWorkflowsSize;
    private Boolean isRoleManager;
    private Boolean isOneSignShare;
    private Boolean isOneReadShare;
    private Boolean certificatProblem;

    public UiCountersDto() {
    }

    public UiCountersDto(Long nbSignRequests, Long nbToSign, Long nbDeleted, Integer reportNumber,
                         Integer managedWorkflowsSize, Boolean isRoleManager, Boolean isOneSignShare,
                         Boolean isOneReadShare, Boolean certificatProblem) {
        this.nbSignRequests = nbSignRequests;
        this.nbToSign = nbToSign;
        this.nbDeleted = nbDeleted;
        this.reportNumber = reportNumber;
        this.managedWorkflowsSize = managedWorkflowsSize;
        this.isRoleManager = isRoleManager;
        this.isOneSignShare = isOneSignShare;
        this.isOneReadShare = isOneReadShare;
        this.certificatProblem = certificatProblem;
    }

    public Long getNbSignRequests() { return nbSignRequests; }
    public void setNbSignRequests(Long nbSignRequests) { this.nbSignRequests = nbSignRequests; }
    public Long getNbToSign() { return nbToSign; }
    public void setNbToSign(Long nbToSign) { this.nbToSign = nbToSign; }
    public Long getNbDeleted() { return nbDeleted; }
    public void setNbDeleted(Long nbDeleted) { this.nbDeleted = nbDeleted; }
    public Integer getReportNumber() { return reportNumber; }
    public void setReportNumber(Integer reportNumber) { this.reportNumber = reportNumber; }
    public Integer getManagedWorkflowsSize() { return managedWorkflowsSize; }
    public void setManagedWorkflowsSize(Integer managedWorkflowsSize) { this.managedWorkflowsSize = managedWorkflowsSize; }
    public Boolean getIsRoleManager() { return isRoleManager; }
    public void setIsRoleManager(Boolean roleManager) { isRoleManager = roleManager; }
    public Boolean getIsOneSignShare() { return isOneSignShare; }
    public void setIsOneSignShare(Boolean oneSignShare) { isOneSignShare = oneSignShare; }
    public Boolean getIsOneReadShare() { return isOneReadShare; }
    public void setIsOneReadShare(Boolean oneReadShare) { isOneReadShare = oneReadShare; }
    public Boolean getCertificatProblem() { return certificatProblem; }
    public void setCertificatProblem(Boolean certificatProblem) { this.certificatProblem = certificatProblem; }

    public Long nbSignRequests() { return nbSignRequests; }
    public Long nbToSign() { return nbToSign; }
    public Long nbDeleted() { return nbDeleted; }
    public Integer reportNumber() { return reportNumber; }
    public Integer managedWorkflowsSize() { return managedWorkflowsSize; }
    public Boolean isRoleManager() { return isRoleManager; }
    public Boolean isOneSignShare() { return isOneSignShare; }
    public Boolean isOneReadShare() { return isOneReadShare; }
    public Boolean certificatProblem() { return certificatProblem; }
}
