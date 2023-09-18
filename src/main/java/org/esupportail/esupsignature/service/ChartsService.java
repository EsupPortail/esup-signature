package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.dto.charts.SignaturesByYears;
import org.esupportail.esupsignature.dto.charts.SignedDocumentsByYears;
import org.esupportail.esupsignature.repository.AuditStepRepository;
import org.esupportail.esupsignature.repository.LogRepository;
import org.springframework.stereotype.Service;
import software.xdev.chartjs.model.charts.BarChart;
import software.xdev.chartjs.model.color.Color;
import software.xdev.chartjs.model.data.BarData;
import software.xdev.chartjs.model.dataset.BarDataset;
import software.xdev.chartjs.model.options.BarOptions;
import software.xdev.chartjs.model.options.Legend;
import software.xdev.chartjs.model.options.Plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChartsService {

    private final LogRepository logRepository;

    private final AuditStepRepository auditStepRepository;

    public ChartsService(LogRepository logRepository, AuditStepRepository auditStepRepository) {
        this.logRepository = logRepository;
        this.auditStepRepository = auditStepRepository;
    }

    public String getSignsByYears() {
        List<SignedDocumentsByYears> signsByYears = logRepository.countAllByYears();
        List<SignaturesByYears> signaturesByYears = auditStepRepository.countAllByYears();
        Set<String> labels = signsByYears.stream().map(SignedDocumentsByYears::getYear).collect(Collectors.toSet());
        labels.addAll(signaturesByYears.stream().map(SignaturesByYears::getYear).collect(Collectors.toSet()));
        BarDataset countDocsDataset = new BarDataset().setLabel("Nombre de documents signés par année").addBackgroundColor(new Color(255, 99, 132));
        BarDataset countSignsDataset = new BarDataset().setLabel("Nombre de signatures par année").addBackgroundColor(new Color(54, 162, 235));
        List<BarDataset> datasets = new ArrayList<>();
        for(String label: labels) {
            countDocsDataset.addData(Integer.parseInt(signsByYears.stream().filter(s -> s.getYear().equals(label)).map(SignedDocumentsByYears::getCount).findFirst().orElse("0")));
            countSignsDataset.addData(Integer.parseInt(signaturesByYears.stream().filter(s -> s.getYear().equals(label)).map(SignaturesByYears::getCount).findFirst().orElse("0")));
        }
        datasets.add(countDocsDataset);
        datasets.add(countSignsDataset);
        BarOptions options = new BarOptions().setResponsive(true).setPlugins(new Plugins().setLegend(new Legend().setPosition(Legend.Position.RIGHT).setDisplay(true)));
        BarData data = new BarData().setLabels(labels).setDatasets(datasets);
        return new BarChart(data, options).toJson();
    }

}
