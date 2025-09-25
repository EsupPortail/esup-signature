package org.esupportail.esupsignature.entity.enums;

import java.util.Arrays;

public enum SignRequestStatus {
    uploading, draft, pending, checked, signed, refused, deleted, completed, exported, @Deprecated archived, @Deprecated cleaned;

    public static SignRequestStatus[] activeValues() {
        return Arrays.stream(values())
                .filter(s -> {
                    try {
                        return SignRequestStatus.class
                                .getField(s.name())
                                .getAnnotation(Deprecated.class) == null;
                    } catch (NoSuchFieldException e) {
                        return false;
                    }
                })
                .toArray(SignRequestStatus[]::new);
    }
}
