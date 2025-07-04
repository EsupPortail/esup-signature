package org.esupportail.esupsignature.entity.enums;

public enum SignType {
    hiddenVisa(0), visa(1), signature(2);

    private final int value;

    SignType(final int newValue) {
        value = newValue;
    }

    public int getValue() { return value; }

}
