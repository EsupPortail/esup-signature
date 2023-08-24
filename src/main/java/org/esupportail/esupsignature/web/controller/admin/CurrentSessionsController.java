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

import org.apache.commons.io.FileUtils;
import org.esupportail.esupsignature.service.security.SessionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;
import org.springframework.session.Session;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.HashMap;
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

	@Resource
	@Qualifier("sessionRegistry")
	private SessionRegistry sessionRegistry;

	@Resource
	private SessionService sessionService;

	@PersistenceContext
	private EntityManager entityManager;

	@GetMapping
	@SuppressWarnings("unchecked")
	public String getCurrentSessions(Model model) {
		Map<String, List<Session>> allSessions = new HashMap<>();
		List<String> sessionIds = entityManager.createNativeQuery("select session_id from spring_session").getResultList();
		long sessionSize = 0;
		for(String sessionId : sessionIds) {
			List<Session> sessions = new ArrayList<>();
			Session session = sessionService.getSessionById(sessionId);
			if(session != null) {
				SessionInformation sessionInformation = sessionRegistry.getSessionInformation(sessionId);
				for (String attr : session.getAttributeNames()) {
					sessionSize += session.getAttribute(attr).toString().getBytes().length;
				}
				sessions.add(session);
				if(sessionInformation != null && sessionInformation.getPrincipal() instanceof LdapUserDetailsImpl) {
					LdapUserDetailsImpl ldapUserDetails = (LdapUserDetailsImpl) sessionInformation.getPrincipal();
					if(!allSessions.containsKey(ldapUserDetails.getUsername())) {
						allSessions.put(ldapUserDetails.getUsername(), sessions);
					} else {
						allSessions.get(ldapUserDetails.getUsername()).addAll(sessions);
					}
				} else {
					List<String> userNames = entityManager.createNativeQuery("select principal_name from spring_session where session_id = '" + sessionId + "'").getResultList();
					if(userNames.get(0) != null) {
						if(!allSessions.containsKey(userNames.get(0))) {
							allSessions.put(userNames.get(0), sessions);
						} else {
							allSessions.get(userNames.get(0)).addAll(sessions);
						}
					}
				}
			}
		}
		model.addAttribute("currentSessions", allSessions);
		model.addAttribute("sessionSize", FileUtils.byteCountToDisplaySize(sessionSize));
		model.addAttribute("active", "sessions");
		return "admin/currentsessions";
	}

	@DeleteMapping
	public String deleteSessions(@RequestParam String sessionId) {
		Session session = sessionService.getSessionById(sessionId);
		if(session != null) {
			sessionService.deleteSessionById(session.getId());
		}
		return "redirect:/admin/currentsessions";
	}

}
