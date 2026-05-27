package org.esupportail.esupsignature.service.security.cas;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apereo.cas.client.session.SessionMappingStorage;
import org.apereo.cas.client.session.SingleSignOutHandler;
import org.apereo.cas.client.util.AbstractConfigurationFilter;
import org.apereo.cas.client.util.XmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
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
		String ticket = request.getParameter("ticket");
		String logoutRequest = request.getParameter("logoutRequest");

		if (!handlerInitialized.getAndSet(true)) {
			singleSignOutHandler.init();
		}

		if ("POST".equalsIgnoreCase(request.getMethod()) && "/login/cas".equals(request.getServletPath())) {
			String sessionIndex = extractSessionIndex(logoutRequest);
			logger.info("CAS POST received on [{}], logoutRequest present: {}, ticket present: {}, sessionIndex: {}, mapping known: {}, storage size: {}",
					request.getRequestURI(),
					logoutRequest != null,
					ticket != null,
					sessionIndex,
					hasMapping(sessionIndex),
					getManagedSessionCount());
		}

		boolean continueChain = singleSignOutHandler.process(request, response);
		if (!continueChain) {
			logger.info("CAS single logout processed for [{}]", request.getRequestURI());
			return;
		}

		if (ticket != null && request.getSession(false) != null) {
			logger.info("CAS ticket [{}] recorded for session [{}], mapping known: {}, storage size: {}",
					ticket,
					request.getSession(false).getId(),
					hasMapping(ticket),
					getManagedSessionCount());
		}

		filterChain.doFilter(servletRequest, servletResponse);
	}

	private String extractSessionIndex(String logoutRequest) {
		if (logoutRequest == null) {
			return null;
		}
		String parsedLogoutRequest = logoutRequest;
		if (!logoutRequest.contains("SessionIndex")) {
			return null;
		}
		try {
			return XmlUtils.getTextForElement(parsedLogoutRequest, "SessionIndex");
		} catch (Exception e) {
			logger.warn("Unable to extract SessionIndex from CAS logoutRequest", e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private boolean hasMapping(String mappingId) {
		if (mappingId == null) {
			return false;
		}
		try {
			SessionMappingStorage storage = singleSignOutHandler.getSessionMappingStorage();
			Field managedSessionsField = storage.getClass().getDeclaredField("MANAGED_SESSIONS");
			managedSessionsField.setAccessible(true);
			Map<String, ?> managedSessions = (Map<String, ?>) managedSessionsField.get(storage);
			return managedSessions.containsKey(mappingId);
		} catch (Exception e) {
			logger.warn("Unable to inspect CAS session mapping storage for mapping [{}]", mappingId, e);
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private int getManagedSessionCount() {
		try {
			SessionMappingStorage storage = singleSignOutHandler.getSessionMappingStorage();
			Field managedSessionsField = storage.getClass().getDeclaredField("MANAGED_SESSIONS");
			managedSessionsField.setAccessible(true);
			Map<String, ?> managedSessions = (Map<String, ?>) managedSessionsField.get(storage);
			return managedSessions.size();
		} catch (Exception e) {
			logger.warn("Unable to inspect CAS session mapping storage size", e);
			return -1;
		}
	}
}
