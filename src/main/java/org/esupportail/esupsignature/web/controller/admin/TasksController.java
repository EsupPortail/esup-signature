package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;

@RequestMapping("/admin/tasks")
@Controller
public class TasksController {

    @ModelAttribute("adminMenu")
    String getCurrentMenu() {
        return "active";
    }

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "su";
    }

    @Resource
    private SignBookService signBookService;

    @GetMapping
    public String scheduler(Model model) {
        model.addAttribute("isEnableArchiveTask", signBookService.isEnableArchiveTask());
        model.addAttribute("isEnableCleanTask", signBookService.isEnableCleanTask());
        return "admin/tasks";
    }

    @PostMapping("/run-archive")
    public String runArchive(RedirectAttributes redirectAttributes) {
        if(!signBookService.isEnableArchiveTask()) {
            signBookService.setEnableArchiveTask(true);
            signBookService.initArchive();
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Archivage démarré"));
        } else {
            signBookService.setEnableArchiveTask(false);
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Archivage arrêté"));
        }
        return "redirect:/admin/tasks";
    }

    @PostMapping("/run-clean")
    public String runClean(RedirectAttributes redirectAttributes) {
        if(!signBookService.isEnableCleanTask()) {
            signBookService.setEnableCleanTask(true);
            signBookService.initCleanning();
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Nettoyage démarré"));
        } else {
            signBookService.setEnableCleanTask(false);
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Nettoyage arrêté"));
        }
        return "redirect:/admin/tasks";
    }

}
