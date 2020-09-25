package org.esupportail.esupsignature.web.controller.ws.json;

public class JsonMessage {

    String type;
    String text;
    Object object;

    public JsonMessage(String type, String text) {
        this.type = type;
        this.text = text;
    }

    public JsonMessage(String type, String text, Object object) {
        this.type = type;
        this.text = text;
        this.object  = object;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }
}
