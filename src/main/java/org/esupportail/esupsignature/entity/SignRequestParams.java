package org.esupportail.esupsignature.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Version;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

@Entity
public class SignRequestParams {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

	@Version
    @Column(name = "version")
    private Integer version;
	
	public enum SignType {
		visa, pdfImageStamp, certSign, nexuSign;
	}

	public enum NewPageType {
		none, onBegin;
	}
	
	@Enumerated(EnumType.STRING)
	private SignType signType;
    
	@Enumerated(EnumType.STRING)
	private NewPageType newPageType;

	private int signPageNumber;
	
	private int xPos;

	private int yPos;
	
    public SignType getSignType() {
        return this.signType;
    }

    public String getSignTypeLabel() {
        return this.signType.toString();
    }
    
    public void setSignType(SignType signType) {
        this.signType = signType;
    }
    
    public NewPageType getNewPageType() {
        return this.newPageType;
    }
    
    public void setNewPageType(NewPageType newPageType) {
        this.newPageType = newPageType;
    }

	public int getSignPageNumber() {
        return this.signPageNumber;
    }

	public void setSignPageNumber(int signPageNumber) {
        this.signPageNumber = signPageNumber;
    }

	public int getXPos() {
        return this.xPos;
    }

	public void setXPos(int xPos) {
        this.xPos = xPos;
    }

	public int getYPos() {
        return this.yPos;
    }

	public void setYPos(int yPos) {
        this.yPos = yPos;
    }

	public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
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
