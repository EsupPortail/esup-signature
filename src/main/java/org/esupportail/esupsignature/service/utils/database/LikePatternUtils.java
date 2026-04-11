package org.esupportail.esupsignature.service.utils.database;

public final class LikePatternUtils {

    private LikePatternUtils() {
    }

    public static String escape(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    public static String containsPattern(String value) {
        if (value == null) {
            return null;
        }
        return "%" + escape(value) + "%";
    }

    public static String startsWithPattern(String value) {
        if (value == null) {
            return null;
        }
        return escape(value) + "%";
    }
}

