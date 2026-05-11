package org.esupportail.esupsignature.dto.ui.global;

import java.io.Serializable;

public class UiMessageDto implements Serializable {

    String type;
    String text;
    Object object;

    public UiMessageDto(String type, String text) {
        this.type = type;
        this.text = text;
    }

    public UiMessageDto(String type, String text, Object object) {
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
