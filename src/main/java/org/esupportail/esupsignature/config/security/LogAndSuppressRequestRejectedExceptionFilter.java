package org.esupportail.esupsignature.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LogAndSuppressRequestRejectedExceptionFilter extends GenericFilterBean {

    private static final Logger logger = LoggerFactory.getLogger(LogAndSuppressRequestRejectedExceptionFilter.class);

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        try {
            chain.doFilter(req, res);
        } catch (RequestRejectedException e) {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;

            logger
                    .error(
                            "request_rejected: remote={}, user_agent={}, request_url={}",
                            request.getRemoteHost(),
                            request.getHeader(HttpHeaders.USER_AGENT),
                            request.getRequestURL(),
                            e
                    );

            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}