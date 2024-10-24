package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.dto.chart.CountByYearsChartDto;
import org.esupportail.esupsignature.dto.chart.WorkflowStatusChartDto;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.repository.AuditStepRepository;
import org.esupportail.esupsignature.repository.LogRepository;
import org.esupportail.esupsignature.repository.WorkflowRepository;
import org.springframework.stereotype.Service;
import software.xdev.chartjs.model.charts.BarChart;
import software.xdev.chartjs.model.charts.DoughnutChart;
import software.xdev.chartjs.model.data.BarData;
import software.xdev.chartjs.model.data.DoughnutData;
import software.xdev.chartjs.model.dataset.BarDataset;
import software.xdev.chartjs.model.dataset.DoughnutDataset;
import software.xdev.chartjs.model.options.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChartsService {

    private final LogRepository logRepository;

    private final AuditStepRepository auditStepRepository;

    private final WorkflowRepository workflowRepository;

    public ChartsService(LogRepository logRepository, AuditStepRepository auditStepRepository, WorkflowRepository workflowRepository) {
        this.logRepository = logRepository;
        this.auditStepRepository = auditStepRepository;
        this.workflowRepository = workflowRepository;
    }

    public String getSignsByYears() {
        List<CountByYearsChartDto> signsByYears = logRepository.countAllByYears();
        List<CountByYearsChartDto> signaturesByYears = auditStepRepository.countAllByYears();
        List<CountByYearsChartDto> signaturesCertByYears = auditStepRepository.countAllCertByYears();
        List<CountByYearsChartDto> refusedByYears = logRepository.countAllRefusedByYears();
        Set<String> labels = signsByYears.stream().map(CountByYearsChartDto::getYear).collect(Collectors.toSet());
        labels.addAll(signaturesByYears.stream().map(CountByYearsChartDto::getYear).collect(Collectors.toSet()));
        labels.addAll(signaturesCertByYears.stream().map(CountByYearsChartDto::getYear).collect(Collectors.toSet()));
        labels.addAll(refusedByYears.stream().map(CountByYearsChartDto::getYear).collect(Collectors.toSet()));
        BarDataset countDocsDataset = new BarDataset().setStack("0").setLabel("Nombre de documents signés par année").addBackgroundColor(toHex(new Color(170, 222, 167)));
        BarDataset countCertDataset = new BarDataset().setStack("1").setLabel("Nombre de signatures avec certificat par année").addBackgroundColor(toHex(new Color(255, 206, 86)));
        BarDataset countSignsDataset = new BarDataset().setStack("1").setLabel("Nombre de signatures sans certificat par année").addBackgroundColor(toHex(new Color(54, 162, 235)));
        BarDataset countRefusedDataset = new BarDataset().setStack("2").setLabel("Nombre de signatures refusées par année").addBackgroundColor(toHex(new Color(255, 99, 132)));
        if(!labels.isEmpty()) {
            List<BarDataset> datasets = new ArrayList<>();
            for (String label : labels.stream().sorted(Comparator.comparingInt(Integer::parseInt)).toList()) {
                countDocsDataset.addData(Integer.parseInt(signsByYears.stream().filter(s -> s.getYear().equals(label)).map(CountByYearsChartDto::getCount).findFirst().orElse("0")));
                countCertDataset.addData(Integer.parseInt(signaturesCertByYears.stream().filter(s -> s.getYear().equals(label)).map(CountByYearsChartDto::getCount).findFirst().orElse("0")));
                countSignsDataset.addData(Integer.parseInt(signaturesByYears.stream().filter(s -> s.getYear().equals(label)).map(CountByYearsChartDto::getCount).findFirst().orElse("0")) - Integer.parseInt(signaturesCertByYears.stream().filter(s -> s.getYear().equals(label)).map(CountByYearsChartDto::getCount).findFirst().orElse("0")));
                countRefusedDataset.addData(Integer.parseInt(refusedByYears.stream().filter(s -> s.getYear().equals(label)).map(CountByYearsChartDto::getCount).findFirst().orElse("0")));
            }
            datasets.add(countDocsDataset);
            datasets.add(countCertDataset);
            datasets.add(countSignsDataset);
            datasets.add(countRefusedDataset);
            BarOptions options = new BarOptions().setResponsive(true).setPlugins(new Plugins().setLegend(new Legend().setPosition(Legend.Position.RIGHT).setDisplay(true)));
            BarData data = new BarData().setLabels(labels.stream().sorted(Comparator.comparingInt(Integer::parseInt)).toList()).setDatasets(datasets);
            return new BarChart(data, options).toJson();
        } else {
            return "";
        }
    }

    public String getWorkflowSignBooksStatus(Long id) {
        Workflow workflow = workflowRepository.findById(id).orElseThrow();
        List<WorkflowStatusChartDto> workflowStatusChartDtos = workflowRepository.findWorkflowStatusCount(id);
        if(workflowStatusChartDtos.isEmpty()) {
            return null;
        }
        Set<String> labels = workflowStatusChartDtos.stream().map(WorkflowStatusChartDto::getStatus).collect(Collectors.toSet());
        DoughnutDataset doughnutDataset = new DoughnutDataset().setLabel("Status des demandes").addBackgroundColor(toHex(new Color(170, 222, 167))).addBackgroundColor(toHex(new Color(255, 206, 86))).addBackgroundColor(toHex(new Color(255, 99, 132)));
        for(WorkflowStatusChartDto workflowStatusChartDto : workflowStatusChartDtos) {
            doughnutDataset.addData(workflowStatusChartDto.getCount());
        }
        DoughnutOptions options = new DoughnutOptions().setResponsive(true).setPlugins(new Plugins().setLegend(new Legend().setTitle(new LegendTitle().setDisplay(true).setColor("#000000").setText("Circuit " + workflow.getDescription())).setPosition(Legend.Position.RIGHT)));
        DoughnutData doughnutData = new DoughnutData().setLabels(labels.stream().sorted().toList()).addDataset(doughnutDataset);
        return new DoughnutChart(doughnutData, options).toJson();
    }

    private String toHex(Color color) {
        return "#" + Integer.toHexString(color.getRGB()).substring(2);
    }

}
