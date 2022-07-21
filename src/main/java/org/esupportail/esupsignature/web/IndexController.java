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

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.esupportail.esupsignature.service.ldap.PersonLdap;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

@RequestMapping("/")
@Controller
@EnableConfigurationProperties(GlobalProperties.class)
public class IndexController {

	private static final Logger logger = LoggerFactory.getLogger(IndexController.class);

	@Resource
	private PreAuthorizeService preAuthorizeService;

	public IndexController(GlobalProperties globalProperties) {
		this.globalProperties = globalProperties;
	}

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "home";
	}

	@Resource
	private List<SecurityService> securityServices;

	@Resource
	private UserService userService;

	@Resource
	private SignRequestService signRequestService;

	@Autowired(required = false)
	private LdapPersonService ldapPersonService;

	private final GlobalProperties globalProperties;

	@GetMapping
	public String index(Model model, HttpServletRequest httpServletRequest) {
		DefaultSavedRequest defaultSavedRequest = (DefaultSavedRequest) httpServletRequest.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST");
		if(defaultSavedRequest != null && defaultSavedRequest.getServletPath().startsWith("/ws")) {
			return "redirect:/denied/ws";
		}
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User authUser = getAuthUser(auth);
		if(authUser != null && !authUser.getEppn().equals("system")) {
			model.asMap().clear();
			return "redirect:/user/";
		} else {
			if("anonymousUser".equals(auth.getName())) {
				logger.trace("auth user : " + auth.getName());
				model.addAttribute("securityServices", securityServices);
				model.addAttribute("globalProperties", globalProperties);
				if(defaultSavedRequest != null) {
					model.addAttribute("redirect", defaultSavedRequest.getRequestURL() + "?" + defaultSavedRequest.getQueryString());
				}
				return "signin";
			} else {
				logger.info("auth user : " + auth.getName());
				if(defaultSavedRequest != null && !defaultSavedRequest.getRequestURI().equals("/login/casentry")) {
					return "redirect:" + defaultSavedRequest.getServletPath();
				} else {
					return "redirect:/user/";
				}
			}
		}
	}

	@GetMapping("/login/**")
	public String loginRedirection() {
		return "redirect:/";
	}

	@RequestMapping(value = "/denied/ws/**", method = {RequestMethod.GET, RequestMethod.POST})
	@ResponseBody
	public ResponseEntity<String> deniedWs() {
		return new ResponseEntity<> (HttpStatus.UNAUTHORIZED);
	}

	@RequestMapping(value = "/denied/**", method = {RequestMethod.GET, RequestMethod.POST})
	public String denied(HttpSession httpSession, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User authUser = getAuthUser(auth);
		Object forwardObject = httpServletRequest.getAttribute("javax.servlet.forward.request_uri");
		if(forwardObject != null) {
			String forwardUri = httpServletRequest.getAttribute("javax.servlet.forward.request_uri").toString();
			String[] uriParams = forwardUri.split("/");
			if (uriParams.length == 4 && uriParams[1].equals("user") && uriParams[2].equals("signrequests")) {
				try {
					SignRequest signRequest = signRequestService.getById(Long.parseLong(uriParams[3]));
					if (signRequest != null) {
						User suUser = preAuthorizeService.checkShareForSignRequest(signRequest, authUser.getEppn());
						if (suUser != null) {
							httpSession.setAttribute("suEppn", suUser);
							redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Délégation activée : " + suUser.getEppn()));
							return "redirect:" + forwardUri;
						}
					} else {
						redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Demande non trouvée"));
						return "redirect:/user/";
					}
				} catch (Exception e) {
					return "redirect:/user/";
				}
			}
		}
		return "denied";
	}

	public User getAuthUser(Authentication auth) {
		User user = null;
		if (auth != null && !auth.getName().equals("anonymousUser")) {
			if(ldapPersonService != null) {
				List<PersonLdap> personLdaps =  ldapPersonService.getPersonLdap(auth.getName());
				if(personLdaps.size() > 0) {
					String eppn = personLdaps.get(0).getEduPersonPrincipalName();
					if(eppn == null) {
						eppn = userService.buildEppn(auth.getName());
					}
					user = userService.getUserByEppn(eppn);
				}
			} else {
				logger.debug("Try to retrieve "+ auth.getName() + " without ldap");
				user = userService.getUserByEppn(auth.getName());
			}
		}
		return user;
	}

	@GetMapping("/logged-out")
	public String loggedOut() {
		return "logged-out";
	}

	@RequestMapping(value={"/robots.txt", "/robot.txt"}, produces = "text/plain")
	@ResponseBody
	public String getRobotsTxt() {
		return "User-agent: *\n" +
				"Disallow: /\n";
	}

}
