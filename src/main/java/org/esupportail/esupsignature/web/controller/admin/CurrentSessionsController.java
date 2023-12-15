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

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RequestMapping("/admin/currentsessions")
@Controller
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class CurrentSessionsController {
	
	@ModelAttribute("adminMenu")
	String getCurrentMenu() {
		return "active";
	}

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "currentSessions";
	}

	private final SessionRegistry sessionRegistry;

	public CurrentSessionsController(SessionRegistry sessionRegistry) {
		this.sessionRegistry = sessionRegistry;
	}

	@GetMapping
	public String getCurrentSessions(Model model) {
		List<SessionInformation> sessions = new ArrayList<>();
		for(Object principal : sessionRegistry.getAllPrincipals()) {
			sessions.addAll(sessionRegistry.getAllSessions(principal, false));
		}
		model.addAttribute("currentSessions", sessions);
		model.addAttribute("sessionSize", 0);
		model.addAttribute("active", "sessions");
		return "admin/currentsessions";
	}

	@DeleteMapping
	public String deleteSessions(@RequestParam String sessionId) {
		sessionRegistry.getSessionInformation(sessionId).expireNow();
		return "redirect:/admin/currentsessions";
	}

}
