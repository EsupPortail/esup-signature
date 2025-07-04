package org.esupportail.esupsignature.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SignLevel {
    hiddenVisa(0), visa(1), signature(2);

    private final int value;

    SignLevel(final int newValue) {
        value = newValue;
    }

    public int getValue() { return value; }

    /**
     * Custom deserialization logic for SignType enum.
     * Maps legacy or deprecated string values (e.g., "pdfImageStamp", "certSign", "nexuSign") to the current enum values.
     * Ensures backward compatibility with existing JSON payloads.
     **/
    @JsonCreator
    public static SignLevel fromString(String key) {
        if ("pdfImageStamp".equalsIgnoreCase(key) || "certSign".equalsIgnoreCase(key) || "nexuSign".equalsIgnoreCase(key)) {
            return signature;
        }
        return SignLevel.valueOf(key);
    }


}
