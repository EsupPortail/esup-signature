package org.esupportail.esupsignature.exception;

import java.io.Serial;

public class EsupSignatureRuntimeException extends RuntimeException {

	@Serial
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
