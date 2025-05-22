package org.esupportail.esupsignature.config.security.jwt;

import jakarta.annotation.Nullable;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class MissingBearerTokenFilter extends OncePerRequestFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(MissingBearerTokenFilter.class);

	@Override
	protected void doFilterInternal(
		@Nullable HttpServletRequest request,
		@Nullable HttpServletResponse response,
		@Nullable FilterChain filterChain
	) throws ServletException, IOException {
		if (request != null && response != null) {
			String authorizationHeader = request.getHeader("Authorization");
			LOGGER.info("Authorization header: {}", authorizationHeader);
			if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header plop");
				return;
			}
		}
		if (filterChain != null) {
			filterChain.doFilter(request, response);
		}
	}
}