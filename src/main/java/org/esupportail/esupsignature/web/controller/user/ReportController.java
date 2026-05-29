package org.esupportail.esupsignature.web.controller.user;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.dto.ui.global.UiMessageDto;
import org.esupportail.esupsignature.service.ReportService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RequestMapping("/user/reports")
@Controller
public class ReportController {

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "reports";
    }

    @Resource
    private ReportService reportService;

    @GetMapping
    public String list(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @SortDefault(value = "createDate", direction = Sort.Direction.DESC) @PageableDefault(size = 10) Pageable pageable, Model model) {
        model.addAttribute("reports", reportService.buildReportListView(authUserEppn).getReports());
        return "user/reports/list";
    }

    @PreAuthorize("@preAuthorizeService.reportOwner(#id, #authUserEppn)")
    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        reportService.delete(id);
        redirectAttributes.addFlashAttribute("message", new UiMessageDto("info", "Suppression effectuée"));
        return "redirect:/user/reports";
    }

    @DeleteMapping(value = "/all/", produces = "text/html")
    public String deleteAll(@ModelAttribute("authUserEppn") String authUserEppn, RedirectAttributes redirectAttributes) {
        reportService.deleteAll(authUserEppn);
        redirectAttributes.addFlashAttribute("message", new UiMessageDto("info", "Suppression effectuée"));
        return "redirect:/user/reports";
    }
}
