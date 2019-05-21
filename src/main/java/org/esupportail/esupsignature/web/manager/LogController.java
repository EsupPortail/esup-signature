package org.esupportail.esupsignature.web.manager;

import javax.annotation.Resource;

import org.esupportail.esupsignature.domain.Log;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.service.UserService;
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
	
	@Resource
	private UserService userService;
	
	@ModelAttribute("user")
	public User getUser() {
		return userService.getUserFromAuthentication();
	}

	@RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
        addDateTimeFormatPatterns(uiModel);
        uiModel.addAttribute("log", Log.findLog(id));
        uiModel.addAttribute("itemId", id);
        return "manager/logs/show";
    }

	@RequestMapping(produces = "text/html")
    public String list(@RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "sortFieldName", required = false) String sortFieldName, @RequestParam(value = "sortOrder", required = false) String sortOrder, Model uiModel) {
        if (page != null || size != null) {
            int sizeNo = size == null ? 10 : size.intValue();
            final int firstResult = page == null ? 0 : (page.intValue() - 1) * sizeNo;
            uiModel.addAttribute("logs", Log.findLogEntries(firstResult, sizeNo, sortFieldName, sortOrder));
            float nrOfPages = (float) Log.countLogs() / sizeNo;
            uiModel.addAttribute("maxPages", (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages));
        } else {
            uiModel.addAttribute("logs", Log.findAllLogs(sortFieldName, sortOrder));
        }
        addDateTimeFormatPatterns(uiModel);
        return "manager/logs/list";
    }

	void addDateTimeFormatPatterns(Model uiModel) {
        uiModel.addAttribute("log_logdate_date_format", "dd/MM/yyyy - HH:mm");
    }
}
