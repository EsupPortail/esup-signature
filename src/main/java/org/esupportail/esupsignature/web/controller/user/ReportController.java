package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.entity.Report;
import org.esupportail.esupsignature.service.ReportService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import java.util.List;

@RequestMapping("/user/reports")
@Controller
public class ReportController {

    @Resource
    private ReportService reportService;

    @GetMapping
    public String list(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @SortDefault(value = "createDate", direction = Sort.Direction.DESC) @PageableDefault(size = 10) Pageable pageable, Model model) {
        List<Report> reports = reportService.getByUser(authUserEppn);
        reports.sort((o1, o2) -> o2.getDate().compareTo(o1.getDate()));
        model.addAttribute("reports", reports);
        return "user/reports/list";
    }
}
