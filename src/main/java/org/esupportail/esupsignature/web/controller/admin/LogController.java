package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.annotation.Resource;

@RequestMapping("/admin/logs")
@Controller
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class LogController {

	private static final Logger logger = LoggerFactory.getLogger(LogController.class);

	@ModelAttribute("adminMenu")
	public String getAdminMenu() {
		return "active";
	}

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "logs";
	}

	private SignRequestStatus statusFilter = null;

	@Resource
	private LogService logService;

	@GetMapping
	public String list(
			@RequestParam(value = "statusFilter", required = false) String statusFilter,
			@SortDefault(value = "logDate", direction = Direction.DESC) @PageableDefault(size = 20) Pageable pageable, Model model) {
		if(statusFilter != null) {
		}

		Page<Log> logs = logService.getAllLogs(pageable);
		model.addAttribute("logs", logs);
		model.addAttribute("statusFilter", this.statusFilter);
		model.addAttribute("statuses", SignRequestStatus.values());
		return "admin/logs/list";
	}
}
