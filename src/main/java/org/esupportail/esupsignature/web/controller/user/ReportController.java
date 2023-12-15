package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.entity.Report;
import org.esupportail.esupsignature.entity.enums.ReportStatus;
import org.esupportail.esupsignature.service.ReportService;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.annotation.Resource;
import java.util.*;

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
        List<Report> reports = reportService.getByUser(authUserEppn);
        reports.sort((o1, o2) -> o2.getDate().compareTo(o1.getDate()));
        Map<Report, Map<ReportStatus, Set<Long>>> reportsMap = new HashMap<>();
        for (Report report : reports) {
            Map<ReportStatus, Set<Long>> reportStatusListMap = new HashMap<>();
            for(Map.Entry<Long, ReportStatus> signRequestReportStatusEntry : report.getSignRequestReportStatusMap().entrySet()) {
                if(!reportStatusListMap.containsKey(signRequestReportStatusEntry.getValue())) {
                    reportStatusListMap.put(signRequestReportStatusEntry.getValue(), new HashSet<>());
                }
                reportStatusListMap.get(signRequestReportStatusEntry.getValue()).add(signRequestReportStatusEntry.getKey());
            }
            reportsMap.put(report, reportStatusListMap);
        }
        model.addAttribute("reports", reports);
        model.addAttribute("reportsMap", reportsMap);
        return "user/reports/list";
    }

    @PreAuthorize("@preAuthorizeService.reportOwner(#id, #authUserEppn)")
    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        reportService.delete(id);
        redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Suppression effectuée"));
        return "redirect:/user/reports";
    }
}
