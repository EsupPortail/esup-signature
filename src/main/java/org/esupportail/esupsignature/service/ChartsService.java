package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.dto.charts.BarChart;
import org.esupportail.esupsignature.dto.charts.BarData;
import org.esupportail.esupsignature.dto.charts.BarDataset;
import org.esupportail.esupsignature.dto.charts.CountByYears;
import org.esupportail.esupsignature.repository.AuditStepRepository;
import org.esupportail.esupsignature.repository.LogRepository;
import org.springframework.stereotype.Service;
import software.xdev.chartjs.model.color.Color;
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
        List<CountByYears> signsByYears = logRepository.countAllByYears();
        List<CountByYears> signaturesByYears = auditStepRepository.countAllByYears();
        List<CountByYears> signaturesCertByYears = auditStepRepository.countAllCertByYears();
        List<CountByYears> refusedByYears = logRepository.countAllRefusedByYears();
        Set<String> labels = signsByYears.stream().map(CountByYears::getYear).collect(Collectors.toSet());
        labels.addAll(signaturesByYears.stream().map(CountByYears::getYear).collect(Collectors.toSet()));
        labels.addAll(signaturesCertByYears.stream().map(CountByYears::getYear).collect(Collectors.toSet()));
        labels.addAll(refusedByYears.stream().map(CountByYears::getYear).collect(Collectors.toSet()));
        BarDataset countDocsDataset = new BarDataset().setStack("0").setLabel("Nombre de documents signés par année").addBackgroundColor(new Color(170, 222, 167));
        BarDataset countCertDataset = new BarDataset().setStack("1").setLabel("Nombre de signatures avec certificat par année").addBackgroundColor(new Color(255, 206, 86));
        BarDataset countSignsDataset = new BarDataset().setStack("1").setLabel("Nombre de signatures sans certificat par année").addBackgroundColor(new Color(54, 162, 235));
        BarDataset countRefusedDataset = new BarDataset().setStack("2").setLabel("Nombre de signatures refusées par année").addBackgroundColor(new Color(255, 99, 132));
        if(!labels.isEmpty()) {
            List<BarDataset> datasets = new ArrayList<>();
            for (String label : labels) {
                countDocsDataset.addData(Integer.parseInt(signsByYears.stream().filter(s -> s.getYear().equals(label)).map(CountByYears::getCount).findFirst().orElse("0")));
                countCertDataset.addData(Integer.parseInt(signaturesCertByYears.stream().filter(s -> s.getYear().equals(label)).map(CountByYears::getCount).findFirst().orElse("0")));
                countSignsDataset.addData(Integer.parseInt(signaturesByYears.stream().filter(s -> s.getYear().equals(label)).map(CountByYears::getCount).findFirst().orElse("0")) - Integer.parseInt(signaturesCertByYears.stream().filter(s -> s.getYear().equals(label)).map(CountByYears::getCount).findFirst().orElse("0")));
                countRefusedDataset.addData(Integer.parseInt(refusedByYears.stream().filter(s -> s.getYear().equals(label)).map(CountByYears::getCount).findFirst().orElse("0")));
            }
            datasets.add(countDocsDataset);
            datasets.add(countCertDataset);
            datasets.add(countSignsDataset);
            datasets.add(countRefusedDataset);
            BarOptions options = new BarOptions().setResponsive(true).setPlugins(new Plugins().setLegend(new Legend().setPosition(Legend.Position.RIGHT).setDisplay(true)));
            BarData data = new BarData().setLabels(labels).setDatasets(datasets);
            return new BarChart(data, options).toJson();
        } else {
            return "";
        }
    }

}
