package org.esupportail.esupsignature.service.security.su;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SuAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
	private final UserService userService;

	public SuAuthenticationSuccessHandler(UserService userService) {
		this.userService = userService;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
		HttpSession httpSession = httpServletRequest.getSession(true);
		String targetEppn = userService.buildEppn(authentication.getName());
		User targetUser = userService.getByEppn(targetEppn);
		if (targetUser == null) {
			restoreOriginalAuthentication(authentication);
			httpSession.removeAttribute("userEppn");
			httpSession.removeAttribute("authUserEppn");
			httpSession.removeAttribute("suEppn");
			httpSession.setAttribute("suErrorMsg", SwitchUserDetailsService.USER_NEVER_LOGGED_IN_MESSAGE);
			this.redirectStrategy.sendRedirect(httpServletRequest, httpServletResponse, "/admin/su");
			return;
		}
		httpSession.removeAttribute("userEppn");
		httpSession.removeAttribute("authUserEppn");
		this.redirectStrategy.sendRedirect(httpServletRequest, httpServletResponse, "/");
	}

	private void restoreOriginalAuthentication(Authentication switchedAuthentication) {
		switchedAuthentication.getAuthorities().stream()
				.filter(SwitchUserGrantedAuthority.class::isInstance)
				.map(SwitchUserGrantedAuthority.class::cast)
				.map(SwitchUserGrantedAuthority::getSource)
				.findFirst()
				.ifPresent(authentication -> SecurityContextHolder.getContext().setAuthentication(authentication));
	}
	
}
