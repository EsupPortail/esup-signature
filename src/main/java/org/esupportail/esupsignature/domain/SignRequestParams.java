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
	
	private float xPos;

	private float yPos;
	
}
