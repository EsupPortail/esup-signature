package org.esupportail.esupsignature.entity.enums;

/**
 * Enumération représentant les différents types de signatures possibles
 * ainsi qu'une valeur associée pour chacun des types.
 *
 * Les différents types de signatures incluent :
 *
 * - imageStamp : Utilisation d'une image comme tampon de signature (valeur associée : 2).
 * - userCert : Signature avec un certificat utilisateur (valeur associée : 3).
 * - groupCert : Signature avec un certificat de groupe (valeur associée : 3).
 * - autoCert : Signature avec un certificat automatisé (valeur associée : 3).
 * - openPkiCert : Signature avec un certificat de type open PKI (valeur associée : 3).
 * - sealCert : Signature avec un certificat de scellement (valeur associée : 4).
 * - nexuCert : Signature avec un certificat via Esup-DSS-Client (valeur associée : 4).
 */
public enum SignWith {

    imageStamp(2), userCert(3), groupCert(3), autoCert(3), openPkiCert(3), sealCert(4), nexuCert(4);

    private final int value;

    SignWith(final int newValue) {
        value = newValue;
    }

    public int getValue() { return value; }

}
