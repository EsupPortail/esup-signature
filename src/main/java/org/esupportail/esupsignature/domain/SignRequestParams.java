package org.esupportail.esupsignature.domain;

import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.addon.tostring.RooToString;

@RooJavaBean
@RooToString
@RooJpaActiveRecord
public class SignRequestParams {
	
	public enum SignType {
		certSign, pdfImageStamp, nexuSign, validate;
	}

	public enum NewPageType {
		none, onBegin, onEnd;
	}

	private SignType signType;
    
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
