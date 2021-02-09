package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Report;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ReportService {

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "reports";
    }

    @Resource
    private ReportRepository reportRepository;

    @Resource
    private UserService userService;

    public List<Report> getAll() {
        List<Report> list = new ArrayList<>();
        reportRepository.findAll().forEach(list::add);
        return list;
    }

    public List<Report> getByUser(String userEppn) {
        return reportRepository.findByUserEppn(userEppn);
    }

    public Report createReport(String userEppn) {
        Report report = new Report();
        report.setUser(userService.getByEppn(userEppn));
        report.setDate(new Date());
        reportRepository.save(report);
        return report;
    }

    @Transactional
    public void addsignRequestsSigned(Long id, SignRequest signRequest) {
        reportRepository.findById(id).get().getSignRequestsSigned().add(signRequest);
    }

    @Transactional
    public void addsignRequestsNoField(Long id, SignRequest signRequest) {
        reportRepository.findById(id).get().getSignRequestsNoField().add(signRequest);
    }

    @Transactional
    public void addsignRequestsError(Long id, SignRequest signRequest) {
        reportRepository.findById(id).get().getSignRequestsError().add(signRequest);
    }
}
