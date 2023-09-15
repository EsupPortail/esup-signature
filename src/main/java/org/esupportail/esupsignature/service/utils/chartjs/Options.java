package org.esupportail.esupsignature.service.utils.chartjs;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Options {

    public List<Integer> yAxesTicks;
    public boolean beginAtZero;


}
