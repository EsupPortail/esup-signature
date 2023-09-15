package org.esupportail.esupsignature.service.utils.chartjs;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Doughnut implements Chart {

    String type = "doughnut";
    Data data;


    public Doughnut(Data data) {
        this.data = data;

    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public Data getData() {
        return data;
    }

    @Override
    public Options getOptions() {
        return null;
    }

    public void setData(Data data) {
        this.data = data;
    }
}
