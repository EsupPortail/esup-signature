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

import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@RequestMapping("/")
@Controller
public class IndexController {

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "home";
	}

	@Autowired
	private List<SecurityService> securityConfigs;
	
	@Resource
	private UserService userService;

	@Resource
	private SignRequestService signRequestService;

	@Resource
	private DataRepository dataRepository;

	@Resource
	private FileService fileService;

	@ModelAttribute("user")
	public User getUser() {
		return userService.getUserFromAuthentication();
	}
	
	@GetMapping
	public String index(Model model, Pageable pageable) throws IOException {
		User user = userService.getUserFromAuthentication();
		model.addAttribute("user", user);
		if(user != null && !user.getEppn().equals("System")) {
			List<SignRequest> signRequestsToSign = signRequestService.getToSignRequests(user);
			model.addAttribute("signRequests", signRequestService.getSignRequestsPageGrouped(signRequestsToSign, pageable));
			List<Data> datas =  dataRepository.findByCreateByAndStatus(user.getEppn(), SignRequestStatus.draft);
			model.addAttribute("datas", datas);
			if(user.getSignImage() != null) {
				model.addAttribute("signFile", fileService.getBase64Image(user.getSignImage()));
			}
			return "index";
		} else {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if("anonymousUser".equals(auth.getName())) {
				model.addAttribute("securityConfigs", securityConfigs);
				return "signin";
			} else {
				userService.createUser(SecurityContextHolder.getContext().getAuthentication());
				return "signin";
			}
		}

	}
	
	@RequestMapping("/login/**")
	public String loginRedirection() {
		return "redirect:/";			
	}

}
