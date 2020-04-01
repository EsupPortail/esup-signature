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
package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

@RequestMapping("/admin/currentsessions")
@Controller
public class CurrentSessionsController {
	
	@ModelAttribute("adminMenu")
	String getCurrentMenu() {
		return "active";
	}

	@Resource
	@Qualifier("sessionRegistry")
	private SessionRegistry sessionRegistry;

	@Resource
	private UserService userService;

	@ModelAttribute("user")
	public User getUser() {
		return userService.getUserFromAuthentication();
	}

	@GetMapping
	public String getCurrentSessions(Model uiModel) {
		List<String> sessions = new Vector<>();
		List<Object> principals = sessionRegistry.getAllPrincipals();
		for(Object p: principals) {
			sessions.add(((UserDetails) p).getUsername());
		}
		uiModel.addAttribute("currentSessions", sessions);
		uiModel.addAttribute("active", "sessions");
		return "admin/currentsessions";
	}

}
