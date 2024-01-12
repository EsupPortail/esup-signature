package org.esupportail.esupsignature.dto.js;

public class JsSlimSelect {

    String text;
    String value;

    public JsSlimSelect(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
