package org.esupportail.esupsignature.exception;

public class EsupSignatureException extends Exception {

	private static final long serialVersionUID = 1L;

	String message;
	
	public EsupSignatureException(String message) {
		super(message);
		this.message = message;
	}
	
}
