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
import jakarta.servlet.http.HttpSession;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.ldap.LdapPersonLightService;
import org.esupportail.esupsignature.service.ldap.entry.PersonLightLdap;
import org.esupportail.esupsignature.service.security.OidcOtpSecurityService;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.esupportail.esupsignature.service.security.cas.CasSecurityServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
				model.addAttribute("securityServices", securityServices.stream().filter(s -> !(s instanceof OidcOtpSecurityService)).toList());
				model.addAttribute("globalProperties", globalProperties);
				if(StringUtils.hasText(savedQueryString)) {
					model.addAttribute("redirect", savedQueryString);
					if(!savedQueryString.contains("/casentry") && securityServices.size() == 1 && securityServices.get(0) instanceof CasSecurityServiceImpl) {
						return "redirect:/login/casentry?redirect=" + savedQueryString;
					}
				}
				if(httpServletRequest.getSession().getAttribute("errorMsg") != null) {
					model.addAttribute("message", new JsMessage("error", httpServletRequest.getSession().getAttribute("errorMsg").toString()));
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
							redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Délégation activée : " + suUser.getEppn()));
							return "redirect:" + forwardUri;
						}
					} else {
						redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Demande non trouvée"));
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
			value = {"/login/proconnectentry", "/login/franceconnectentry"},
			method = {RequestMethod.GET, RequestMethod.POST}
	)	public String loginFranceConnectRedirectionPost(Authentication authentication, Model model) {
		DefaultOidcUser defaultOidcUser = (DefaultOidcUser) authentication.getPrincipal();
		String name = defaultOidcUser.getAttributes().get("given_name").toString() + " ";
		name += defaultOidcUser.getAttributes().containsKey("family_name")
			? defaultOidcUser.getAttributes().get("family_name").toString()
			: defaultOidcUser.getAttributes().get("usual_name").toString();
		model.addAttribute("errorMsg", "Bonjour " + name + ",<br>" +
				"Merci de vous déconnecter et d'utiliser de nouveau le lien d'accès présent dans le mail que vous avez reçu pour signer votre document." +
				"Une nouvelle connexion est necessaire pour chaque nouvelle demande à signer");
		return "otp/error";
	}

	public User getAuthUser(Authentication auth) {
		User user = null;
		if (auth != null && !auth.getName().equals("anonymousUser")) {
			if(ldapPersonLightService != null) {
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
	public String loggedOut(HttpServletRequest httpServletRequest) {
		String returnedState = httpServletRequest.getParameter("state");
		String expectedState = null;
		if (httpServletRequest.getCookies() != null) {
			for (Cookie cookie : httpServletRequest.getCookies()) {
				if ("logout_state".equals(cookie.getName())) {
					expectedState = cookie.getValue();
				}
			}
		}
		if (!Objects.equals(returnedState, expectedState)) {
			throw new IllegalStateException("Échec vérification du state !");
		}
		httpServletRequest.getSession().invalidate();
		return "logged-out";
	}

	@RequestMapping(value={"/robots.txt", "/robot.txt"}, produces = "text/plain")
	@ResponseBody
	public String getRobotsTxt() {
		return "User-agent: *\n" +
				"Disallow: /\n";
	}

}
