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

import org.esupportail.esupsignature.dto.HttpSession;
import org.esupportail.esupsignature.service.security.HttpSessionsListenerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

	private final HttpSessionsListenerService httpSessionsListenerService;

	public CurrentSessionsController(SessionRegistry sessionRegistry, HttpSessionsListenerService httpSessionsListenerService) {
		this.sessionRegistry = sessionRegistry;
        this.httpSessionsListenerService = httpSessionsListenerService;
    }

	@GetMapping
	public String getCurrentSessions(Model model) {
		Map<String, HttpSession> allSessions = httpSessionsListenerService.getSessions();
		List<SessionInformation> sessions = new ArrayList<>();
		for(Object principal : sessionRegistry.getAllPrincipals()) {
			for(SessionInformation sessionInformation: sessionRegistry.getAllSessions(principal, false)) {
				if (allSessions.containsKey(sessionInformation.getSessionId())) {
					HttpSession httpSession = allSessions.get(sessionInformation.getSessionId());
					httpSession.setLastRequest(sessionInformation.getLastRequest());
					httpSession.setUserEppn(((UserDetails) principal).getUsername());
					sessions.addAll(sessionRegistry.getAllSessions(principal, false));
				} else {
					HttpSession httpSession = new HttpSession();
					httpSession.setSessionId(sessionInformation.getSessionId());
					httpSession.setLastRequest(sessionInformation.getLastRequest());
					httpSession.setUserEppn(((UserDetails) principal).getUsername());
					allSessions.put(sessionInformation.getSessionId(), httpSession);
				}
			}
		}
		sessions.sort((s1, s2) -> s2.getLastRequest().compareTo(s1.getLastRequest()));
		model.addAttribute("httpSessions", allSessions.values().stream().toList());
		model.addAttribute("currentSessions", sessions);
		model.addAttribute("sessionSize", 0);
		model.addAttribute("active", "sessions");
		return "admin/currentsessions";
	}

	@DeleteMapping
	public String deleteSessions(@RequestParam String sessionId) {
		httpSessionsListenerService.getSessions().remove(sessionId);
		SessionInformation sessionInformation = sessionRegistry.getSessionInformation(sessionId);
		if(sessionInformation != null) {
			sessionInformation.expireNow();
		}
		return "redirect:/admin/currentsessions";
	}

}
