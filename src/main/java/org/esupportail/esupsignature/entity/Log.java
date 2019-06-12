package org.esupportail.esupsignature.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Configurable
public class Log {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

	@Version
    @Column(name = "version")
    private Integer version;
	
	@Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy - HH:mm")
    private Date logDate;
	
	private String eppn;

	private String action;
	
	private String initialStatus;
	
	private String finalStatus;
	
	private String returnCode;
	
	private String ip;
	
	private String comment;
	
	private Long signRequestId;


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

	public long getSignRequestId() {
        return this.signRequestId;
    }

	public void setSignRequestId(long signRequestId) {
        this.signRequestId = signRequestId;
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

}
