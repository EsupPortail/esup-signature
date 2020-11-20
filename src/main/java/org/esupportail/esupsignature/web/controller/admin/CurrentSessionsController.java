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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("/admin/currentsessions")
@Controller
public class CurrentSessionsController {
	
	@ModelAttribute("adminMenu")
	String getCurrentMenu() {
		return "active";
	}

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "currentSessions";
	}

	@Resource
	@Qualifier("sessionRegistry")
	private SessionRegistry sessionRegistry;

	@Resource
	private SessionRepository<Session> sessionRepository;

	@GetMapping
	public String getCurrentSessions(Model model) throws NoSuchMethodException {
		Map<String, List<Session>> allSessions = new HashMap<>();
		List<Object> principals = sessionRegistry.getAllPrincipals();
		for(Object principal: principals) {
			List<Session> sessions = new ArrayList<>();
			List<SessionInformation> sessionInformations =  sessionRegistry.getAllSessions(principal, true);
			for(SessionInformation sessionInformation : sessionInformations) {
				Session session = sessionRepository.findById(sessionInformation.getSessionId());
				if(session != null) {
					sessions.add(session);
				}
			}
			if(sessions.size() > 0) {
				allSessions.put(((UserDetails) principal).getUsername(), sessions);
			}

		}
		model.addAttribute("currentSessions", allSessions);
		model.addAttribute("active", "sessions");
		return "admin/currentsessions";
	}

	@DeleteMapping
	public String deleteSessions(@RequestParam String sessionId, RedirectAttributes redirectAttributes) {
		Session session = sessionRepository.findById(sessionId);
		if(session != null) {
			sessionRepository.deleteById(session.getId());
		}
		return "redirect:/admin/currentsessions";
	}

}
