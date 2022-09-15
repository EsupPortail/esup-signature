package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Report;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.enums.ReportStatus;
import org.esupportail.esupsignature.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ReportService {

    @Resource
    private ReportRepository reportRepository;

    @Resource
    private UserService userService;

    public Report getById(Long id) {
        return reportRepository.findById(id).get();
    }

    public int countByUser(String eppn) {
        return reportRepository.countByUserEppn(eppn);
    }

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
    public void addSignRequestToReport(Long id, SignRequest signRequest, ReportStatus reportStatus) {
        reportRepository.findById(id).get().getSignRequestReportStatusMap().put(signRequest.getId(), reportStatus);
    }

    @Transactional
    public void delete(Long id) {
        Report report = reportRepository.findById(id).get();
        reportRepository.delete(report);
    }

    @Transactional
    public void anonymize(String userEppn) {
        List<Report> reports = reportRepository.findByUserEppn(userEppn);
        for (Report report : reports) {
            report.setUser(userService.getAnonymousUser());
        }
    }

}
