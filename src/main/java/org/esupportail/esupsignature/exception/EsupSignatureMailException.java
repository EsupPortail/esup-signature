package org.esupportail.esupsignature.exception;

public class EsupSignatureMailException extends EsupSignatureRuntimeException {

	private static final long serialVersionUID = 1L;

	String message;

	public EsupSignatureMailException(String message) {
		super(message);
		this.message = message;
	}

	public EsupSignatureMailException(String message, Throwable e) {
		super(message, e);
		this.message = message;
	}
}
