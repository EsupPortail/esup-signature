package org.esupportail.esupsignature.service.utils.chartjs;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Dataset {

    String label;
    List<String> data;
    List<String> backgroundColor = List.of("#FF5C4D", "#FF9933", "#88338C", "#FFFF4F", "#284EE8", "#E3FF3C", "#51FF62", "#5ADBFF", "#208A8C", "#5A9FFF", "#D759FF", "#FF5AC1", "#A880FF", "#F7F7F7", "#CCCCCC", "#B2B2B2", "#4D4D4D", "#A45077", "#FDCA59", "#E64D4D", "#985972");
    Integer hoverOffset;
    Integer borderWith;
    Double tension;

    List<String> percentages;

    List<Map<String, Object>> dataLabels;


    public Dataset(String label, List<String> data, List<String> backgroundColor, Integer hoverOffset, Integer borderWith, Double tension, List<String> percentages) {
        this.label = label;
        this.data = data;
        if (backgroundColor != null) {
            this.backgroundColor = backgroundColor;
        }
        this.hoverOffset = hoverOffset;
        this.borderWith = borderWith;
        this.tension = tension;
        this.percentages = percentages;
    }

    /* public void addDataLabels(List<String> percentages) {
         List<Map<String, Object>> dataLabels = new ArrayList<>();
         for (int i = 0; i < data.size(); i++) {
             Map<String, Object> dataLabel = new HashMap<>();
             dataLabel.put("nombre d'individus", String.format("%s - %s)", data.get(i), percentages.get(i)));
             dataLabels.add(dataLabel);
         }
         this.dataLabels = dataLabels;
     }*/

    public List<Map<String, Object>> getDataLabels(List<String> percentages) {
        List<Map<String, Object>> dataLabels = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> dataLabel = new HashMap<>();
            String label = String.format("%s (%s)", data.get(i), this.percentages.get(i));
            dataLabel.put("label", label);
            dataLabel.put("value", data.get(i));
            dataLabel.put("percentage", this.percentages.get(i));
            dataLabels.add(dataLabel);
        }
        return dataLabels;
    }

    public void addDataLabels(List<String> percentages) {
    }
}
