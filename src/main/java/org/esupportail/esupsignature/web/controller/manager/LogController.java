package org.esupportail.esupsignature.web.controller.manager;

import javax.annotation.Resource;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.LogRepository;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/manager/logs")
@Controller
public class LogController {

	@ModelAttribute("active")
	public String getActiveMenu() {
		return "manager/logs";
	}
	
	@Autowired
	private LogRepository logRepository;
	
	@Resource
	private UserService userService;
	
	@ModelAttribute("user")
	public User getUser() {
		return userService.getUserFromAuthentication();
	}

	@RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
        addDateTimeFormatPatterns(uiModel);
        uiModel.addAttribute("log", logRepository.findById(id).get());
        uiModel.addAttribute("itemId", id);
        return "manager/logs/show";
    }

	@RequestMapping(produces = "text/html")
    public String list(@RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "sortFieldName", required = false) String sortFieldName, @RequestParam(value = "sortOrder", required = false) String sortOrder, Model uiModel) {
        if (page != null || size != null) {
            int sizeNo = size == null ? 10 : size.intValue();
            final int firstResult = page == null ? 0 : (page.intValue() - 1) * sizeNo;
            uiModel.addAttribute("logs", logRepository.findAll());
            float nrOfPages = (float) logRepository.count() / sizeNo;
            uiModel.addAttribute("maxPages", (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages));
        } else {
            uiModel.addAttribute("logs", logRepository.findAll());
        }
        addDateTimeFormatPatterns(uiModel);
        return "manager/logs/list";
    }

	void addDateTimeFormatPatterns(Model uiModel) {
        uiModel.addAttribute("log_logdate_date_format", "dd/MM/yyyy - HH:mm");
    }
}
