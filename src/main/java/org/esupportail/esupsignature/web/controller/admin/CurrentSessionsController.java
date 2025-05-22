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

import org.esupportail.esupsignature.dto.view.HttpSessionViewDto;
import org.esupportail.esupsignature.repository.custom.SessionRepositoryCustom;
import org.esupportail.esupsignature.service.security.HttpSessionsListenerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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

	private final JdbcIndexedSessionRepository sessionRepository;

	private final SessionRepositoryCustom sessionRepositoryCustom;

	public CurrentSessionsController(SessionRegistry sessionRegistry, HttpSessionsListenerService httpSessionsListenerService, JdbcIndexedSessionRepository sessionRepository, SessionRepositoryCustom sessionRepositoryCustom) {
		this.sessionRegistry = sessionRegistry;
        this.httpSessionsListenerService = httpSessionsListenerService;
        this.sessionRepository = sessionRepository;
        this.sessionRepositoryCustom = sessionRepositoryCustom;
    }

	@GetMapping
	public String getCurrentSessions(Model model) {
		List<HttpSessionViewDto> sessions = new ArrayList<>();
		List<String> allSessionIds = sessionRepositoryCustom.findAllSessionIds();
        for (String sessionId : allSessionIds) {
			Session session = sessionRepository.findById(sessionId);
			if(session != null) {
				HttpSessionViewDto httpSession = new HttpSessionViewDto();
				httpSession.setSessionId(session.getId());
				httpSession.setCreatedDate(Date.from(session.getCreationTime()));
				httpSession.setLastRequest(Date.from(session.getLastAccessedTime()));
				httpSession.setUserEppn(session.getAttribute("userEppn"));
				httpSession.setExpired(session.isExpired());
				sessions.add(httpSession);
			}
		}
		sessions.sort(Comparator.comparing(HttpSessionViewDto::getLastRequest, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
		model.addAttribute("httpSessions", sessions);
		long now = System.currentTimeMillis();
		model.addAttribute("httpSessionsAlive", sessions.stream().filter(s -> now - s.getLastRequest().getTime() < 60 * 1000).toList());
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
