package org.esupportail.esupsignature.entity.enums;

public enum SignWith {

    imageStamp(2), userCert(3), groupCert(3), autoCert(3), openPkiCert(3), sealCert(3), nexuCert(4);

    private final int value;

    SignWith(final int newValue) {
        value = newValue;
    }

    public int getValue() { return value; }

}
