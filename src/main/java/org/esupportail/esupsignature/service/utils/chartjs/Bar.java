package org.esupportail.esupsignature.service.utils.chartjs;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)

public class Bar implements Chart{

    String type = "bar";
    Data data;

    Options options;

    public Bar(Data data, Options options) {
        this.data = data;
        this.options = options;
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

    public void setData(Data data) {
        this.data = data;
    }

    @Override
    public Options getOptions() {
        return options;
    }

    public  void setOptions(Options options) {
        this.options = options;
    }
}
