package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.dto.charts.SignaturesByYears;
import org.esupportail.esupsignature.dto.charts.SignedDocumentsByYears;
import org.esupportail.esupsignature.repository.AuditStepRepository;
import org.esupportail.esupsignature.repository.LogRepository;
import org.esupportail.esupsignature.service.utils.chartjs.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChartsService {

    private final LogRepository logRepository;

    private final AuditStepRepository auditStepRepository;

    public ChartsService(LogRepository logRepository, AuditStepRepository auditStepRepository) {
        this.logRepository = logRepository;
        this.auditStepRepository = auditStepRepository;
    }

    public Chart getSignsByYears() {
        List<SignedDocumentsByYears> signsByYears = logRepository.countAllByYears();
        List<SignaturesByYears> signaturesByYears = auditStepRepository.countAllByYears();

        List<String> countsDocs = signsByYears.stream().map(SignedDocumentsByYears::getCount).toList();
        List<String> countsSigns = signaturesByYears.stream().map(SignaturesByYears::getCount).toList();
        List<Integer> yAxesTicks = null;

        List<Dataset> datasets = new ArrayList<>();
        datasets.add(new Dataset("Nombre de documents signés par année", countsDocs, null, null, 1, 0.1,null));
        datasets.add(new Dataset("Nombre de signatures par année", countsSigns, null, null, 1, 0.1,null));
        List<String> labels = signsByYears.stream().map(SignedDocumentsByYears::getYear).toList();

        Options options = new Options();
        options.beginAtZero = true;
        options.yAxesTicks = yAxesTicks;

        Data data = new Data(labels, datasets);
        return new Bar(data, options);
    }

}
