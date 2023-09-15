package org.esupportail.esupsignature.service.utils.chartjs;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Pie implements Chart {

    String type = "pie";
    Data data;

    public Pie(Data data) {
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
