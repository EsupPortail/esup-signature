package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.entity.Tag;
import org.esupportail.esupsignature.service.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RequestMapping("/admin/tags")
@Controller
public class TagAdminController {

    private static final Logger logger = LoggerFactory.getLogger(TagAdminController.class);

    @ModelAttribute("adminMenu")
    String getCurrentMenu() {
        return "active";
    }

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "tags";
    }

    private final TagService tagService;

    public TagAdminController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public String list(Model model, Pageable pageable) {
        model.addAttribute("tags", tagService.getAllTags(pageable));
        return "admin/tags/list";
    }

    @PostMapping
    public String create(Tag tag, RedirectAttributes redirectAttributes) {
        tagService.createTag(tag);
        redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Tag enregistré"));
        return "redirect:/admin/tags";
    }

    @DeleteMapping("{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        tagService.deleteTag(id);
        redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Tag supprimé"));
        return "redirect:/admin/tags";
    }
}
