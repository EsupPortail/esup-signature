package org.esupportail.esupsignature.exception;

import java.io.Serial;

public class EsupSignatureException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	String message;
	
	public EsupSignatureException(String message) {
		super(message);
		this.message = message;
	}
	
	public EsupSignatureException(String message, Throwable e) {
		super(message, e);
		this.message = message;
	}
}
