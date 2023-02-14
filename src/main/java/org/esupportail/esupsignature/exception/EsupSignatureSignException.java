package org.esupportail.esupsignature.exception;

public class EsupSignatureSignException extends EsupSignatureRuntimeException {

	private static final long serialVersionUID = 1L;

	String message;
	
	public EsupSignatureSignException(String message) {
		super(message);
		this.message = message;
	}
	
	public EsupSignatureSignException(String message, Throwable e) {
		super(message, e);
		this.message = message;
	}
}
