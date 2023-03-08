package org.esupportail.esupsignature.exception;

public class EsupSignatureKeystoreException extends EsupSignatureRuntimeException {

	private static final long serialVersionUID = 1L;

	String message;
	
	public EsupSignatureKeystoreException(String message) {
		super(message);
		this.message = message;
	}
	
	public EsupSignatureKeystoreException(String message, Throwable e) {
		super(message, e);
		this.message = message;
	}
}
