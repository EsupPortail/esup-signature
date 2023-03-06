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

import org.esupportail.esupsignature.service.MessageService;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.text.ParseException;

@RequestMapping("/admin/messages")
@Controller
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class MessageAdminController {

	@ModelAttribute("adminMenu")
	public String getAdminMenu() {
		return "active";
	}

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "messages";
	}

	@Resource
	private MessageService messageService;

	@GetMapping
	public String messages(Pageable pageable, Model model) {
		model.addAttribute("messages", messageService.getAll(pageable));
		return "admin/messages";
	}

	@PostMapping("/add")
	public String addMessage(@RequestParam String text, @RequestParam String endDate) throws ParseException {
		messageService.createMessage(endDate, text);
		return "redirect:/admin/messages";
	}

	@DeleteMapping("{id}")
	public String messages(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		messageService.deleteMessage(id);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Message supprimé"));
		return "redirect:/admin/messages";
	}

}
