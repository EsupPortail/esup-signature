package org.esupportail.esupsignature.exception;

public class EsupSignatureRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	String message;

	public EsupSignatureRuntimeException(String message) {
		super(message);
		this.message = message;
	}

	public EsupSignatureRuntimeException(String message, Throwable e) {
		super(message, e);
		this.message = message;
	}
}
