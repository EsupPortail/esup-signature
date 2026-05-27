package org.esupportail.esupsignature.service.security.cas;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apereo.cas.client.session.SingleSignOutHandler;
import org.apereo.cas.client.util.AbstractConfigurationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wraps the CAS single logout handler so we can explicitly target /login/cas
 * and log what happens when CAS posts a back-channel logout request.
 */
public class CasSingleSignOutFilter extends AbstractConfigurationFilter {

	private static final Logger logger = LoggerFactory.getLogger(CasSingleSignOutFilter.class);

	private final AtomicBoolean handlerInitialized = new AtomicBoolean(false);
	private final SingleSignOutHandler singleSignOutHandler = new SingleSignOutHandler();

	public CasSingleSignOutFilter(String logoutCallbackPath) {
		singleSignOutHandler.setLogoutCallbackPath(logoutCallbackPath);
	}

	@Override
	public void init(jakarta.servlet.FilterConfig filterConfig) throws ServletException {
		super.init(filterConfig);
		singleSignOutHandler.init();
		handlerInitialized.set(true);
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;

		if (!handlerInitialized.getAndSet(true)) {
			singleSignOutHandler.init();
		}

		if ("POST".equalsIgnoreCase(request.getMethod()) && "/login/cas".equals(request.getServletPath())) {
			logger.info("CAS POST received on [{}], logoutRequest present: {}, ticket present: {}",
					request.getRequestURI(),
					request.getParameter("logoutRequest") != null,
					request.getParameter("ticket") != null);
		}

		boolean continueChain = singleSignOutHandler.process(request, response);
		if (!continueChain) {
			logger.info("CAS single logout processed for [{}]", request.getRequestURI());
			return;
		}

		filterChain.doFilter(servletRequest, servletResponse);
	}
}
