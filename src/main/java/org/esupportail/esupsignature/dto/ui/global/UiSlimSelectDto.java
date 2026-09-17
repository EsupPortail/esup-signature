package org.esupportail.esupsignature.dto.ui.global;

public class UiSlimSelectDto {

    String text;
    String value;
    String html;

    public UiSlimSelectDto(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public UiSlimSelectDto(String text, String value, String html) {
        this.text = text;
        this.value = value;
        this.html = html;
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

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }
}
