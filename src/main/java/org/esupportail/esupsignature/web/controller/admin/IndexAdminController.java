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

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@RequestMapping("/admin")
@Controller
public class IndexAdminController {

	@ModelAttribute("active")
	public String getActiveMenu() {
		return "admin";
	}

	private SignRequest.SignRequestStatus statusFilter = null;

	@Resource
	private UserService userService;

	@Autowired
	private SignRequestRepository signRequestRepository;

	@Resource
	private SignRequestService signRequestService;

	@Autowired
	private SignRequestParamsRepository signRequestParamsRepository;

	@Autowired
	private SignBookRepository signBookRepository;

	@Resource
	private SignBookService signBookService;

	@Autowired
	private LogRepository logRepository;

	@Autowired
	private DocumentRepository documentRepository;

	@Resource
	private DocumentService documentService;

	@Resource
	private PdfService pdfService;

	@Resource
	private FileService fileService;
	
	@RequestMapping(produces = "text/html")
	public String list(
			@RequestParam(value = "statusFilter", required = false) String statusFilter,
			@RequestParam(value = "signBookId", required = false) Long signBookId,
			@RequestParam(value = "messageError", required = false) String messageError,
			@SortDefault(value = "createDate", direction = Sort.Direction.DESC) @PageableDefault(size = 5) Pageable pageable, RedirectAttributes redirectAttrs, Model model) {
		User user = userService.getUserFromAuthentication();
		if (user == null || !userService.isUserReady(user)) {
			return "redirect:/user/users/?form";
		}

		if(statusFilter != null) {
			if(!statusFilter.equals("all")) {
				this.statusFilter = SignRequest.SignRequestStatus.valueOf(statusFilter);
			} else {
				this.statusFilter = null;
			}
		}

		List<SignRequest> signRequestsToSign = new ArrayList<>();
		List<SignBook> signBooksGroup = signBookRepository.findByRecipientEmailsContainAndSignBookType(user.getEmail(), SignBook.SignBookType.group);
		signBooksGroup.addAll(signBookRepository.findByRecipientEmailsContainAndSignBookType(user.getEmail(), SignBook.SignBookType.user));
		SignBook signBook = signBookRepository.findByName(user.getFirstname() + " " + user.getName()).get(0);
		for(SignBook signBookGroup : signBooksGroup) {
			for(SignRequest signRequest : signBookGroup.getSignRequests()) {
				if(signRequest.getStatus().equals(SignRequest.SignRequestStatus.pending)) {
					signRequestsToSign.add(signRequest);
				}
			}

			List<SignBook> signBooksWorkflows = signBookRepository.findBySignBookContain(signBookGroup);
			for(SignBook signBookWorkflow : signBooksWorkflows) {
				for(SignRequest signRequest : signBookWorkflow.getSignRequests()) {
					if(signRequest.getStatus().equals(SignRequest.SignRequestStatus.pending) && signRequest.getSignBooks().containsKey(signBook.getId()) && !signRequest.getSignBooks().get(signBook.getId())) {
						signRequestsToSign.add(signRequest);
					}
				}
			}
		}

		signRequestsToSign = signRequestsToSign.stream().sorted(Comparator.comparing(SignRequest::getCreateDate).reversed()).collect(Collectors.toList());

		Page<SignRequest> signRequests = signRequestRepository.findBySignResquestByCreateByAndStatus(user.getEppn(), this.statusFilter,  pageable);

		for(SignRequest signRequest : signRequests) {
			signRequest.setOriginalSignBooks(signBookService.getOriginalSignBook(signRequest));
			Map<String, Boolean> signBookNames = new HashMap<>();
			for(Map.Entry<Long, Boolean> signBookMap : signRequest.getSignBooks().entrySet()) {
				signBookNames.put(signBookRepository.findById(signBookMap.getKey()).get().getName(), signBookMap.getValue());
			}
			signRequest.setSignBooksLabels(signBookNames);
		}
		if(user.getKeystore() != null) {
			model.addAttribute("keystore", user.getKeystore().getFileName());
		}
		model.addAttribute("signType", signBookService.getUserSignBook(user).getSignRequestParams().get(0).getSignType());
		model.addAttribute("mydocs", "active");
		model.addAttribute("signRequestsToSign", signRequestsToSign);
		model.addAttribute("signBookId", signBookId);
		model.addAttribute("signRequests", signRequests);
		model.addAttribute("statusFilter", this.statusFilter);
		model.addAttribute("statuses", SignRequest.SignRequestStatus.values());
		model.addAttribute("messageError", messageError);

		return "user/signrequests/list";
	}

}
