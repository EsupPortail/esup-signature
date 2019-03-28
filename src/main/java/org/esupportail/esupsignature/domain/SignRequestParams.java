package org.esupportail.esupsignature.domain;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.addon.tostring.RooToString;

@RooJavaBean
@RooToString
@RooJpaActiveRecord
public class SignRequestParams {
	
	public enum SignType {
		visa, pdfImageStamp, certSign, nexuSign;
	}

	public enum NewPageType {
		none, onBegin, onEnd;
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
    
    public void setSignType(SignType signType) {
        this.signType = signType;
    }
    
    public NewPageType getNewPageType() {
        return this.newPageType;
    }
    
    public void setNewPageType(NewPageType newPageType) {
        this.newPageType = newPageType;
    }
}
