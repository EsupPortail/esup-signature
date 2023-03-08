package org.esupportail.esupsignature.exception;

public class EsupSignatureNexuException extends EsupSignatureRuntimeException {

	private static final long serialVersionUID = 1L;

	String message;
	
	public EsupSignatureNexuException(String message) {
		super(message);
		this.message = message;
	}
	
	public EsupSignatureNexuException(String message, Throwable e) {
		super(message, e);
		this.message = message;
	}
}
