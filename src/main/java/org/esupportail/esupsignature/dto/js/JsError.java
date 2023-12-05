package org.esupportail.esupsignature.dto.js;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class JsError {

    String msg;
    String url;
    String lineNumber;
    String columnNumber;
    @JsonIgnore
    Object error;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(String lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getColumnNumber() {
        return columnNumber;
    }

    public void setColumnNumber(String columnNumber) {
        this.columnNumber = columnNumber;
    }

    public Object getError() {
        return error;
    }

    public void setError(Object error) {
        this.error = error;
    }

    public String toString() {
        return "\n" +
                "{msg : " + msg + "\n" +
                "url : " + url + ", line : " + lineNumber + ", col : " + columnNumber + "}";
    }

}
