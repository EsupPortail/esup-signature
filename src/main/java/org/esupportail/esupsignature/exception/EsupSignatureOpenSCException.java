package org.esupportail.esupsignature.exception;

public class EsupSignatureOpenSCException extends EsupSignatureRuntimeException {

    private static final long serialVersionUID = 1L;

    String message;

    public EsupSignatureOpenSCException(String message) {
        super(message);
        this.message = message;
    }

    public EsupSignatureOpenSCException(String message, Throwable e) {
        super(message, e);
        this.message = message;
    }

}