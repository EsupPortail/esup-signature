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

import jakarta.annotation.Resource;
import org.apache.commons.io.FileUtils;
import org.esupportail.esupsignature.service.security.SessionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.session.Session;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
	private SessionService sessionService;

	@GetMapping
	public String getCurrentSessions(Model model) {
		Map<String, List<Session>> allSessions = sessionService.getAllSessionsListMap();
		long sessionSize = sessionService.getSessionsSize(allSessions.values().stream().flatMap(List::stream).toList());
		model.addAttribute("currentSessions", allSessions);
		model.addAttribute("sessionSize", FileUtils.byteCountToDisplaySize(sessionSize));
		model.addAttribute("active", "sessions");
		return "admin/currentsessions";
	}

	@DeleteMapping
	public String deleteSessions(@RequestParam String sessionId) {
		sessionService.deleteSessionById(sessionId);
		return "redirect:/admin/currentsessions";
	}

}
