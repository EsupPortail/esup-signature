package org.esupportail.esupsignature.config.security;

import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter;

import jakarta.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;

public class APIKeyFilter extends X509AuthenticationFilter {

    @Override
    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request)
    {
        X509Certificate[] certs = (X509Certificate[]) request
                .getAttribute("jakarta.servlet.request.X509Certificate");

        if(certs != null && certs.length > 0)
        {
            return certs[0].getSubjectX500Principal();
        } else {
            return "";
        }
    }

    @Override
    protected Object getPreAuthenticatedCredentials(HttpServletRequest request)
    {
        X509Certificate[] certs = (X509Certificate[]) request
                .getAttribute("jakarta.servlet.request.X509Certificate");

        if(certs != null && certs.length > 0)
        {
            return certs[0].getSubjectX500Principal();
        }

        return "";
    }
}
