/**
 * Licensed to ESUP-Portail under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * ESUP-Portail licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.esupportail.esupsignature.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dto.ui.global.UiGlobalPropertiesDto;
import org.esupportail.esupsignature.dto.ui.global.UiMessageDto;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.ldap.LdapPersonLightService;
import org.esupportail.esupsignature.service.ldap.entry.PersonLightLdap;
import org.esupportail.esupsignature.service.security.OidcOtpSecurityService;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.esupportail.esupsignature.service.security.cas.CasSecurityServiceImpl;
import org.esupportail.esupsignature.service.security.oauth.azuread.AzureAdSecurityServiceImpl;
import org.esupportail.esupsignature.service.security.oauth.franceconnect.FranceConnectSecurityServiceImpl;
import org.esupportail.esupsignature.service.security.oauth.proconnect.ProConnectSecurityServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Objects;

@RequestMapping("/")
@Controller
@EnableConfigurationProperties(GlobalProperties.class)
public class IndexController {

	private static final Logger logger = LoggerFactory.getLogger(IndexController.class);

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "home";
	}

	private final GlobalProperties globalProperties;
	private final PreAuthorizeService preAuthorizeService;
	private final List<SecurityService> securityServices;
	private final UserService userService;
	private final SignRequestService signRequestService;
	private final LdapPersonLightService ldapPersonLightService;

	public IndexController(GlobalProperties globalProperties, PreAuthorizeService preAuthorizeService, List<SecurityService> securityServices, UserService userService, SignRequestService signRequestService, @Autowired(required = false) LdapPersonLightService ldapPersonLightService) {
		this.globalProperties = globalProperties;
        this.preAuthorizeService = preAuthorizeService;
        this.securityServices = securityServices;
        this.userService = userService;
        this.signRequestService = signRequestService;
        this.ldapPersonLightService = ldapPersonLightService;
	}

	/**
	 * Filtre les services de sécurité pour déterminer lesquels doivent être affichés sur la page de connexion.
	 *
	 * Logique de filtrage :
	 * - Exclut les services OidcOtpSecurityService (comme FranceConnect, ProConnect) qui nécessitent une gestion OTP spéciale
	 * - INCLUT Azure AD même s'il implémente OidcOtpSecurityService car il peut être utilisé comme authentification standard
	 * - Inclut tous les autres services de sécurité (CAS, Shibboleth, etc.)
	 *
	 * @param securityServices Liste complète des services de sécurité disponibles
	 * @return Liste filtrée des services à afficher sur la page de connexion
	 */
	private List<SecurityService> getDisplayableSecurityServices(List<SecurityService> securityServices) {
		return securityServices.stream()
			.filter(service -> {
				// Si ce n'est pas un service OIDC/OTP, on l'inclut
				if (!(service instanceof OidcOtpSecurityService)) {
					return true;
				}

				// Si c'est Azure AD, on l'inclut même s'il implémente OidcOtpSecurityService
				if (service instanceof AzureAdSecurityServiceImpl) {
					return true;
				}

				// Pour les autres services OIDC/OTP (FranceConnect, ProConnect), on les exclut
				// car ils nécessitent une gestion spéciale avec OTP
				if (service instanceof FranceConnectSecurityServiceImpl ||
					service instanceof ProConnectSecurityServiceImpl) {
					return false;
				}

				// Par défaut, pour tout autre service OIDC/OTP non spécifiquement géré,
				// on l'exclut pour éviter les problèmes
				return false;
			})
			.toList();
	}

	@GetMapping
	public String index(@ModelAttribute("authUserEppn") String authUserEppn, Model model, HttpServletRequest httpServletRequest) {
		String savedQueryString = null;
		HttpSession httpSession = httpServletRequest.getSession(false);
		if(httpSession != null) {
			DefaultSavedRequest defaultSavedRequest = null;
			try {
				defaultSavedRequest = (DefaultSavedRequest) httpSession.getAttribute("SPRING_SECURITY_SAVED_REQUEST");
			} catch (Exception e) {
				logger.warn(e.getMessage());
			}
			if (defaultSavedRequest != null) {
				if (StringUtils.hasText(defaultSavedRequest.getQueryString())) {
					savedQueryString = defaultSavedRequest.getRequestURL() + "?" + defaultSavedRequest.getQueryString();
				} else {
					savedQueryString = defaultSavedRequest.getRequestURL();
				}
			}
		}
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if(StringUtils.hasText(authUserEppn) && !authUserEppn.equals("system")) {
			model.asMap().clear();
			return "redirect:/user";
		} else {
			if("anonymousUser".equals(auth.getName())) {
				logger.trace("auth user : " + auth.getName());
				// Utilisation de la nouvelle méthode de filtrage pour les services de sécurité
				model.addAttribute("securityServices", getDisplayableSecurityServices(securityServices));
				model.addAttribute("globalProperties", UiGlobalPropertiesDto.fromGlobalProperties(globalProperties));
				model.addAttribute("loginTitle", userService.getSystemUiParam(UiParams.loginTitle));
				model.addAttribute("loginSubtitle", userService.getSystemUiParam(UiParams.loginSubtitle));
				if(StringUtils.hasText(savedQueryString)) {
					model.addAttribute("redirect", savedQueryString);
					if(!savedQueryString.contains("/casentry") && securityServices.size() == 1 && securityServices.get(0) instanceof CasSecurityServiceImpl) {
						return "redirect:/login/casentry?redirect=" + savedQueryString;
					}
				}
				if(httpServletRequest.getSession().getAttribute("errorMsg") != null) {
					model.addAttribute("message", new UiMessageDto("error", httpServletRequest.getSession().getAttribute("errorMsg").toString()));
				}
				return "signin";
			} else {
				logger.info("auth user : " + auth.getName());
				if(StringUtils.hasText(savedQueryString) && !savedQueryString.equals("/login/casentry")) {
					return "redirect:" + savedQueryString;
				} else {
					return "redirect:/user";
				}
			}
		}
	}

	@RequestMapping(value = "/denied/**", method = {RequestMethod.GET, RequestMethod.POST})
	public String denied(HttpSession httpSession, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes) {
		httpServletRequest.getSession().removeAttribute("SPRING_SECURITY_SAVED_REQUEST");
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User authUser = getAuthUser(auth);
		Object forwardObject = httpServletRequest.getAttribute("jakarta.servlet.forward.request_uri");
		if(forwardObject != null) {
			String forwardUri = httpServletRequest.getAttribute("jakarta.servlet.forward.request_uri").toString();
			String[] uriParams = forwardUri.split("/");
			if (uriParams.length == 4 && uriParams[1].equals("user") && uriParams[2].equals("signrequests")) {
				try {
					SignRequest signRequest = signRequestService.getById(Long.parseLong(uriParams[3]));
					if (signRequest != null) {
						User suUser = preAuthorizeService.checkShareForSignRequest(signRequest, authUser.getEppn());
						if (suUser != null) {
							httpSession.setAttribute("suEppn", suUser.getEppn());
							redirectAttributes.addFlashAttribute("message", new UiMessageDto("success", "Délégation activée : " + suUser.getEppn()));
							return "redirect:" + forwardUri;
						}
					} else {
						redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", "Demande non trouvée"));
						return "redirect:/user";
					}
				} catch (Exception e) {
					logger.warn(e.getMessage());
				}
			}
		}
		return "denied";
	}

	@GetMapping("/login/casentry")
	public String loginRedirection() {
		return "redirect:/user";
	}

	@RequestMapping(
			value = {"/login/proconnectentry", "/login/franceconnectentry", "/login/azureadentry"},
			method = {RequestMethod.GET, RequestMethod.POST}
	)
	public String loginOAuthRedirectionPost(
			@RequestParam(value = "redirect", required = false) String redirectUrl,
			HttpServletRequest request,
			Authentication authentication, Model model) {
		String path = request.getServletPath();
		// Azure AD : redirection vers l'URL demandée ou le tableau de bord
		if (path.contains("azureadentry")) {
			if (StringUtils.hasText(redirectUrl) && !"null".equals(redirectUrl)) {
				return "redirect:" + redirectUrl;
			}
			return "redirect:/user";
		}
		// FranceConnect et ProConnect : message d'erreur OTP
		DefaultOidcUser defaultOidcUser = (DefaultOidcUser) authentication.getPrincipal();
		String name = defaultOidcUser.getAttributes().get("given_name").toString() + " ";
		name += defaultOidcUser.getAttributes().containsKey("family_name")
			? defaultOidcUser.getAttributes().get("family_name").toString()
			: defaultOidcUser.getAttributes().get("usual_name").toString();
		model.addAttribute("errorMsg", "Bonjour " + name + ",<br>" +
				"Merci de vous déconnecter et d'utiliser de nouveau le lien d'accès présent dans le mail que vous avez reçu pour signer votre document." +
				"Une nouvelle connexion est nécessaire pour chaque nouvelle demande à signer.");
		return "otp/error";
	}

	public User getAuthUser(Authentication auth) {
		User user = null;
		if (auth != null && !auth.getName().equals("anonymousUser")) {
			if(ldapPersonLightService != null && !(auth instanceof OAuth2AuthenticationToken)) {
				List<PersonLightLdap> personLdaps =  ldapPersonLightService.getPersonLdapLight(auth.getName());
				if(personLdaps.size() == 1) {
					String eppn = personLdaps.get(0).getEduPersonPrincipalName();
					if (!StringUtils.hasText(eppn)) {
						eppn = userService.buildEppn(auth.getName());
					}
					user = userService.getByEppn(eppn);
				} else {
					if (personLdaps.isEmpty()) {
						logger.debug("no result on ldap search for " + auth.getName());
					} else {
						logger.debug("more than one result on ldap search for " + auth.getName());
					}
				}
			} else {
				logger.debug("Try to retrieve "+ auth.getName() + " without ldap");
				user = userService.getByEppn(auth.getName());
			}
		}
		return user;
	}

	@GetMapping("/logged-out")
	public String loggedOut(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
		String returnedState = httpServletRequest.getParameter("state");
		String expectedState = null;
		boolean hasOtpLogoutCookie = false;
		if (httpServletRequest.getCookies() != null) {
			for (Cookie cookie : httpServletRequest.getCookies()) {
				if ("logout_state".equals(cookie.getName())) {
					expectedState = cookie.getValue();
				}
				if ("logout_user_type".equals(cookie.getName()) && "otp".equals(cookie.getValue())) {
					hasOtpLogoutCookie = true;
				}
			}
		}
		if (!Objects.equals(returnedState, expectedState)) {
			throw new IllegalStateException("Échec vérification du state !");
		}

		boolean otpLoggedOut = "true".equals(httpServletRequest.getParameter("otp")) || hasOtpLogoutCookie;
		boolean requiresRedirect = httpServletRequest.getParameter("otp") == null && (returnedState != null || hasOtpLogoutCookie);

		expireCookie(httpServletResponse, "logout_state");
		expireCookie(httpServletResponse, "logout_user_type");
		HttpSession httpSession = httpServletRequest.getSession(false);
		if (httpSession != null) {
			httpSession.invalidate();
		}
		if (requiresRedirect) {
			return otpLoggedOut ? "redirect:/logged-out?otp=true" : "redirect:/logged-out";
		}
		model.addAttribute("otpLoggedOut", otpLoggedOut);
		return "logged-out";
	}

    private void expireCookie(HttpServletResponse httpServletResponse, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        httpServletResponse.addCookie(cookie);
    }

	@RequestMapping(value={"/robots.txt", "/robot.txt"}, produces = "text/plain")
	@ResponseBody
	public String getRobotsTxt() {
		return "User-agent: *\n" +
				"Disallow: /\n";
	}

}
