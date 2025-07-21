package org.esupportail.esupsignature.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SignType {
    hiddenVisa(0), visa(1), signature(2);

    private final int value;

    SignType(final int newValue) {
        value = newValue;
    }

    public int getValue() { return value; }

    /**
     * Custom deserialization logic for SignType enum.
     * Maps legacy or deprecated string values (e.g., "pdfImageStamp", "certSign", "nexuSign") to the current enum values.
     * Ensures backward compatibility with existing JSON payloads.
     **/
    @JsonCreator
    public static SignType fromString(String key) {
        if ("pdfImageStamp".equalsIgnoreCase(key) || "certSign".equalsIgnoreCase(key) || "nexuSign".equalsIgnoreCase(key)) {
            return signature;
        }
        return SignType.valueOf(key);
    }


}
