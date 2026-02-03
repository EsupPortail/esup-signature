package org.esupportail.esupsignature.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enumération représentant les différents types de signatures.
 * Chaque type de signature est associé à une valeur numérique qui peut être utilisée pour des
 * objectifs spécifiques, tels que la compatibilité avec des systèmes existants.
 *
 * Les types de signatures disponibles sont :
 *
 * - hiddenVisa : Validation simple sans altération du document.
 * - visa : Représente une signature de type "Visa" (paramètres de signature verrouillés).
 * - signature : Représente une signature standard.
 *
 */
public enum SignType {
    hiddenVisa(0), visa(1), signature(2);

    private final int value;

    SignType(final int newValue) {
        value = newValue;
    }

    public int getValue() { return value; }

    /**
     * Cette méthode permet d’assurer la compatibilté avec les anciennes valeurs encore présentes en base.
     */
    @JsonCreator
    public static SignType fromString(String key) {
        if ("pdfImageStamp".equalsIgnoreCase(key) || "certSign".equalsIgnoreCase(key) || "nexuSign".equalsIgnoreCase(key)) {
            return signature;
        }
        return SignType.valueOf(key);
    }


}
