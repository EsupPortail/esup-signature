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
package org.esupportail.esupsignature.web.controller;

import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RequestMapping("/")
@Controller
public class IndexController {

	@ModelAttribute("userMenu")
	public String getActiveRole() {
		return "active";
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

	@Resource
	private SignRequestRepository signRequestRepository;

	@ModelAttribute(value = "user", binding = false)
	public User getUser() {
		return userService.getCurrentUser();
	}
	
	@GetMapping
	public String index(@ModelAttribute User user, Model model) {
		model.addAttribute("user", user);
		if(user != null && !user.getEppn().equals("System")) {
			return "redirect:/user/";
		} else {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if("anonymousUser".equals(auth.getName())) {
				model.addAttribute("securityServices", securityServices);
				return "signin";
			} else {
				userService.createUser(SecurityContextHolder.getContext().getAuthentication());
				return "signin";
			}
		}

	}

	@GetMapping("/login/**")
	public String loginRedirection() {
		return "redirect:/";
	}

	@GetMapping("/denied/**")
	public String denied(HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes) {
		String forwardUri = (String) httpServletRequest.getAttribute("javax.servlet.forward.request_uri");
		String[] uriParams = forwardUri.split("/");
		if(uriParams.length == 4 && uriParams[1].equals("user") && uriParams[2].equals("signrequests")) {
			SignRequest signRequest = signRequestRepository.findById(Long.valueOf(uriParams[3])).get();
			User suUser = signRequestService.checkShare(signRequest);
			if(suUser != null) {
				if(userService.switchUser(suUser.getEppn())) {
					redirectAttributes.addFlashAttribute("messageWarning", "Délégation activée vers : " + suUser.getFirstname() + " " + suUser.getName());
				}
				return "redirect:"+ forwardUri;
			}
		}
		//TODO check shares
		return "denied";
	}

}
