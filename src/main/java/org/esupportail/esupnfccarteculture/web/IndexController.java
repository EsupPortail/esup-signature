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
package org.esupportail.esupnfccarteculture.web;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/")
@Controller
public class IndexController {

	@RequestMapping
	public String index(HttpServletRequest request, Model uiModel) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
		if(!eppn.equals("anonymousUser")) {
			if(request.isUserInRole("ROLE_ADMIN")) {
				return "redirect:/manager/taglogs?page=1&amp;size=10";
			} else if(request.isUserInRole("ROLE_MANAGER")) {
				return "redirect:/manager/etudiants?page=1&amp;size=10";
			} else if(request.isUserInRole("ROLE_PARTENAIRE")) {
				return "redirect:/partenaire";
			} else {
				return "redirect:/etudiant";
			}
		} else {
			return "index";
		}

	}
	
	@RequestMapping("/login")
	public String login(HttpServletRequest request, Model uiModel) {
		if(request.isUserInRole("ROLE_ADMIN")) {
			return "redirect:/manager/taglogs?page=1&amp;size=10";
		} else if(request.isUserInRole("ROLE_MANAGER")) {
			return "redirect:/manager/etudiants?page=1&amp;size=10";
		} else if(request.isUserInRole("ROLE_PARTENAIRE")) {
			return "redirect:/partenaire";
		} else {
			return "redirect:/etudiant";
		}
	}
}
