package org.esupportail.esupsignature.entity.enums;

public enum SignType {
    hiddenVisa(0), visa(1), pdfImageStamp(2), certSign(3), nexuSign(4);

    private final int value;

    SignType(final int newValue) {
        value = newValue;
    }

    public int getValue() { return value; }

}
