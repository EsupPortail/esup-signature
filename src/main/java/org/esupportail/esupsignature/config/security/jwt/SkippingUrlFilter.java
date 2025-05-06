package org.esupportail.esupsignature.config.security.jwt;

import jakarta.servlet.http.HttpServletRequest;

public class SkippingUrlFilter {

    private static final String[] SKIP_URLS = {
            "/webjars",
            "/css/",
            "/js/",
            "/images/",
            "/favicon.ico", "/svg/", "/fonts/",
    };

    public static boolean shouldSkip(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        for (String swaggerUrl : SKIP_URLS) {
            if (requestURI.contains(swaggerUrl)) {
                return true;
            }
        }
        return false;
    }
}
