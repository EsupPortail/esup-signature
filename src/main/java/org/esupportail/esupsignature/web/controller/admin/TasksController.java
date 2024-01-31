package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.service.scheduler.TaskService;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.annotation.Resource;

@RequestMapping("/admin/tasks")
@Controller
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class TasksController {

    @ModelAttribute("adminMenu")
    String getCurrentMenu() {
        return "active";
    }

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "tasks";
    }

    @Resource
    private TaskService taskService;

    @GetMapping
    public String scheduler(Model model) {
        model.addAttribute("isEnableArchiveTask", taskService.isEnableArchiveTask());
        model.addAttribute("isEnableCleanTask", taskService.isEnableCleanTask());
        model.addAttribute("isEnableCleanUploadingSignBookTask", taskService.isEnableCleanUploadingSignBookTask());
        return "admin/tasks";
    }

    @PostMapping("/run-archive")
    public String runArchive(RedirectAttributes redirectAttributes) {
        if(!taskService.isEnableArchiveTask()) {
            taskService.initArchive();
            redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Archivage démarré"));
        } else {
            taskService.setEnableArchiveTask(false);
            redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Archivage arrêté"));
        }
        return "redirect:/admin/tasks";
    }

    @PostMapping("/run-clean")
    public String runClean(RedirectAttributes redirectAttributes) {
        if(!taskService.isEnableCleanTask()) {
            taskService.initCleanning("system");
            redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Nettoyage démarré"));
        } else {
            taskService.setEnableCleanTask(false);
            redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Nettoyage arrêté"));
        }
        return "redirect:/admin/tasks";
    }

    @PostMapping("/run-clean-uploading-sign-books")
    public String runCleanUploadingSignBooks(RedirectAttributes redirectAttributes) {
        if(!taskService.isEnableCleanUploadingSignBookTask()) {
            taskService.initCleanUploadingSignBooks();
            redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Nettoyage signbooks temporaires démarré"));
        } else {
            taskService.setEnableCleanUploadingSignBookTask(false);
            redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Nettoyage signbooks temporaires arrêté"));
        }
        return "redirect:/admin/tasks";
    }

}
