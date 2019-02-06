package org.esupportail.esupsignature.exception;

public class EsupSignatureIOException extends Exception {

	private static final long serialVersionUID = 1L;

	String message;
	
	public EsupSignatureIOException(String message) {
		super(message);
		this.message = message;
	}
	
	public EsupSignatureIOException(String message, Throwable e) {
		super(message, e);
		this.message = message;
	}
}
