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

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RequestMapping("/")
@Controller
public class IndexController {
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return "index";
	}
	
	@Autowired
	private List<SecurityService> securityConfigs;
	
	@Resource
	private UserService userService;
	
	@ModelAttribute("user")
	public User getUser() {
		return userService.getUserFromAuthentication();
	}
	
	@RequestMapping
	public String index(HttpServletRequest request, Model model) {
		User user = userService.getUserFromAuthentication();
		model.addAttribute("user", user);
		if(user != null) {
			return "redirect:/user/signrequests/";
		} else {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if("anonymousUser".equals(auth.getName())) {
				if(securityConfigs.size() > 1) {
					model.addAttribute("securityConfigs", securityConfigs);
					return "index";
				} else {
					return "redirect:" + securityConfigs.get(0).getLoginUrl();
				}
			} else {
				userService.createUser(SecurityContextHolder.getContext().getAuthentication());
				return "index";			
			}
		}

	}
	
	@RequestMapping("/login/**")
	public String loginRedirection(HttpServletRequest request, Model uiModel) {
		return "redirect:/";			
	}

}
