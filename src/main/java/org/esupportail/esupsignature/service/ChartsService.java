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
        List<BarDataset> datasets = new ArrayList<>();
        BarDataset countDocsDataset = new BarDataset().setLabel("Nombre de documents signés par année").addBackgroundColor(new Color(255, 99, 132));
        signsByYears.stream().map(SignedDocumentsByYears::getCount).forEach(s -> countDocsDataset.addData(Integer.parseInt(s)));
        datasets.add(countDocsDataset);
        BarDataset countSignsDataset = new BarDataset().setLabel("Nombre de signatures par année").addBackgroundColor(new Color(54, 162, 235));
        signaturesByYears.stream().map(SignaturesByYears::getCount).forEach(s -> countSignsDataset.addData(Integer.parseInt(s)));
        datasets.add(countSignsDataset);
        List<String> labels = signsByYears.stream().map(SignedDocumentsByYears::getYear).toList();
        BarOptions options = new BarOptions().setResponsive(true).setPlugins(new Plugins().setLegend(new Legend().setPosition(Legend.Position.RIGHT).setDisplay(true)));
        BarData data = new BarData().setLabels(labels).setDatasets(datasets);
        return new BarChart(data, options).toJson();
    }

}
