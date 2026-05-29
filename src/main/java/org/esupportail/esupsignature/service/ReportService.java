package org.esupportail.esupsignature.service;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dto.page.user.report.ReportListViewDto;
import org.esupportail.esupsignature.entity.Report;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.WsAccessToken;
import org.esupportail.esupsignature.entity.enums.ReportStatus;
import org.esupportail.esupsignature.repository.ReportRepository;
import org.esupportail.esupsignature.repository.WsAccessTokenRepository;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    @Resource
    private ReportRepository reportRepository;

    private final UserService userService;
    private final GlobalProperties globalProperties;
    private final WsAccessTokenRepository wsAccessTokenRepository;

    public ReportService(UserService userService, GlobalProperties globalProperties, WsAccessTokenRepository wsAccessTokenRepository) {
        this.userService = userService;
        this.globalProperties = globalProperties;
        this.wsAccessTokenRepository = wsAccessTokenRepository;
    }

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

    @Transactional(readOnly = true)
    public ReportListViewDto buildReportListView(String userEppn) {
        ReportListViewDto reportListViewDto = new ReportListViewDto();
        reportListViewDto.setReports(
                reportRepository.findByUserEppn(userEppn).stream()
                        .sorted(Comparator.comparing(Report::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                        .map(this::toReportRowDto)
                        .toList()
        );
        return reportListViewDto;
    }

    public Report createReport(String userEppn) {
        Report report = new Report();
        report.setUser(userService.getByEppn(userEppn));
        report.setDate(new Date());
        reportRepository.save(report);
        return report;
    }

    @Transactional
    public void addSignRequestToReport(Long massSignReportId, Long signRequestId, ReportStatus reportStatus) {
        reportRepository.findById(massSignReportId).get().getSignRequestReportStatusMap().put(signRequestId, reportStatus);
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

    public byte[] getReportBytes(SignRequest signRequest) {
        RestTemplate restTemplate = new RestTemplate();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        List<WsAccessToken> wsAccessTokens = wsAccessTokenRepository.findByWorkflowsEmpty();
        if(!wsAccessTokens.isEmpty()) {
            headers.set("X-Api-Key", wsAccessTokens.get(0).getToken());
        }
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(
                globalProperties.getRootUrl() + "/ws/signrequests/get-last-file-and-report/" + signRequest.getId(),
                HttpMethod.GET,
                entity,
                byte[].class
        );
        return response.getBody();
    }

    @Transactional
    public void deleteAll(String authUserEppn) {
        List<Report> reports = reportRepository.findByUserEppn(authUserEppn);
        reportRepository.deleteAll(reports);
    }

    private ReportListViewDto.RowDto toReportRowDto(Report report) {
        Map<ReportStatus, List<Long>> signRequestIdsByStatus = new EnumMap<>(ReportStatus.class);
        report.getSignRequestReportStatusMap().forEach((signRequestId, reportStatus) ->
                signRequestIdsByStatus.computeIfAbsent(reportStatus, key -> new ArrayList<>()).add(signRequestId)
        );

        ReportListViewDto.RowDto rowDto = new ReportListViewDto.RowDto();
        rowDto.setId(report.getId());
        rowDto.setDate(report.getDate());
        rowDto.setStatuses(
                signRequestIdsByStatus.entrySet().stream()
                        .map(this::toStatusDto)
                        .toList()
        );
        return rowDto;
    }

    private ReportListViewDto.StatusDto toStatusDto(Map.Entry<ReportStatus, List<Long>> signRequestIdsByStatus) {
        ReportListViewDto.StatusDto statusDto = new ReportListViewDto.StatusDto();
        statusDto.setStatus(signRequestIdsByStatus.getKey());
        statusDto.setSignRequestIds(signRequestIdsByStatus.getValue().stream().sorted().toList());
        return statusDto;
    }
}
