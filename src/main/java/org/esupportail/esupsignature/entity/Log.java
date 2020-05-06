package org.esupportail.esupsignature.entity;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Configurable
public class Log {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

	@Version
    private Integer version;
	
	@Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy - HH:mm")
    private Date logDate;

    private String eppn;
//
//    @NotNull
//    private String test;

    private String eppnFor;

	private String action;
	
	private String initialStatus;
	
	private String finalStatus;
	
	private String returnCode;
	
	private String ip;

    @Column(columnDefinition = "TEXT")
	private String comment;

    private Integer pageNumber;

    private Integer posX;

    private Integer posY;

    private Integer stepNumber;

	private Long signRequestId;

    public Long getSignRequestId() {
        return this.signRequestId;
    }

    public void setSignRequestId(Long signRequestId) {
        this.signRequestId = signRequestId;
    }

    public Date getLogDate() {
        return this.logDate;
    }

	public void setLogDate(Date logDate) {
        this.logDate = logDate;
    }

	public String getEppn() {
        return this.eppn;
    }

	public void setEppn(String eppn) {
        this.eppn = eppn;
    }

    public String getEppnFor() {
        return eppnFor;
    }

    public void setEppnFor(String eppnFor) {
        this.eppnFor = eppnFor;
    }

    public String getAction() {
        return this.action;
    }

	public void setAction(String action) {
        this.action = action;
    }

	public String getInitialStatus() {
        return this.initialStatus;
    }

	public void setInitialStatus(String initialStatus) {
        this.initialStatus = initialStatus;
    }

	public String getFinalStatus() {
        return this.finalStatus;
    }

	public void setFinalStatus(String finalStatus) {
        this.finalStatus = finalStatus;
    }

	public String getReturnCode() {
        return this.returnCode;
    }

	public void setReturnCode(String returnCode) {
        this.returnCode = returnCode;
    }

	public String getIp() {
        return this.ip;
    }

	public void setIp(String ip) {
        this.ip = ip;
    }

	public String getComment() {
        return this.comment;
    }

	public void setComment(String comment) {
        this.comment = comment;
    }

	public Long getId() {
        return this.id;
    }

	public void setId(Long id) {
        this.id = id;
    }

	public Integer getVersion() {
        return this.version;
    }

	public void setVersion(Integer version) {
        this.version = version;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getPosX() {
        return posX;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }

    public int getPosY() {
        return posY;
    }

    public void setPosY(int posY) {
        this.posY = posY;
    }

    public Integer getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(Integer stepNumber) {
        this.stepNumber = stepNumber;
    }
}
