package org.esupportail.esupsignature.entity.enums;

public enum SignLevel {
    simple(2), advanced(3), qualified(4);

    private final int value;

    SignLevel(final int newValue) {
        value = newValue;
    }

    public int getValue() { return value; }

}
