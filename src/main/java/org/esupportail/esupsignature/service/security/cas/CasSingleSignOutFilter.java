package org.esupportail.esupsignature.service.security.cas;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apereo.cas.client.session.SingleSignOutHandler;
import org.apereo.cas.client.util.AbstractConfigurationFilter;
import org.apereo.cas.client.util.XmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

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
	private final JdbcIndexedSessionRepository sessionRepository;
	private final SessionRegistry sessionRegistry;

	public CasSingleSignOutFilter(String logoutCallbackPath, JdbcIndexedSessionRepository sessionRepository, SessionRegistry sessionRegistry) {
		this.sessionRepository = sessionRepository;
		this.sessionRegistry = sessionRegistry;
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
		String logoutRequest = request.getParameter("logoutRequest");
		String sessionIndex = extractSessionIndex(logoutRequest);
		HttpSession managedSession = getManagedSession(sessionIndex);
		String managedSessionId = managedSession != null ? managedSession.getId() : null;

		if (!handlerInitialized.getAndSet(true)) {
			singleSignOutHandler.init();
		}

		if ("POST".equalsIgnoreCase(request.getMethod()) && "/login/cas".equals(request.getServletPath())) {
			logger.info("CAS POST received on [{}], logoutRequest present: {}, ticket present: {}, sessionIndex: {}, managedSessionId: {}",
					request.getRequestURI(),
					logoutRequest != null,
					request.getParameter("ticket") != null,
					sessionIndex,
					managedSessionId);
		}

		boolean continueChain = singleSignOutHandler.process(request, response);
		if (!continueChain) {
			if (managedSessionId != null) {
				SessionInformation sessionInformation = sessionRegistry.getSessionInformation(managedSessionId);
				if (sessionInformation != null) {
					sessionInformation.expireNow();
				}
				sessionRepository.deleteById(managedSessionId);
				logger.info("Deleted Spring Session [{}] after CAS single logout", managedSessionId);
			}
			logger.info("CAS single logout processed for [{}]", request.getRequestURI());
			return;
		}

		filterChain.doFilter(servletRequest, servletResponse);
	}

	private String extractSessionIndex(String logoutRequest) {
		if (logoutRequest == null || !logoutRequest.contains("SessionIndex")) {
			return null;
		}
		try {
			return XmlUtils.getTextForElement(logoutRequest, "SessionIndex");
		} catch (RuntimeException e) {
			logger.warn("Unable to extract SessionIndex from CAS logoutRequest", e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private HttpSession getManagedSession(String mappingId) {
		if (mappingId == null) {
			return null;
		}
		try {
			Field managedSessionsField = singleSignOutHandler.getSessionMappingStorage().getClass().getDeclaredField("MANAGED_SESSIONS");
			managedSessionsField.setAccessible(true);
			Map<String, HttpSession> managedSessions =
					(Map<String, HttpSession>) managedSessionsField.get(singleSignOutHandler.getSessionMappingStorage());
			return managedSessions.get(mappingId);
		} catch (ReflectiveOperationException e) {
			logger.warn("Unable to resolve CAS-managed session for mapping [{}]", mappingId, e);
			return null;
		}
	}
}
