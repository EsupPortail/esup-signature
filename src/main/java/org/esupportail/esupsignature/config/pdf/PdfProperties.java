package org.esupportail.esupsignature.config.pdf;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pdf")
public class PdfProperties {

    private int pdfToImageDpi = 72;
    private String pathToGS = "/usr/bin/gs";
    private boolean convertToPdfA = true;
    private int pdfALevel = 2;

    public int getPdfToImageDpi() {
        return pdfToImageDpi;
    }

    public void setPdfToImageDpi(int pdfToImageDpi) {
        this.pdfToImageDpi = pdfToImageDpi;
    }

    public String getPathToGS() {
        return pathToGS;
    }

    public void setPathToGS(String pathToGS) {
        this.pathToGS = pathToGS;
    }

    public boolean isConvertToPdfA() {
        return convertToPdfA;
    }

    public void setConvertToPdfA(boolean convertToPdfA) {
        this.convertToPdfA = convertToPdfA;
    }

    public int getPdfALevel() {
        return pdfALevel;
    }

    public void setPdfALevel(int pdfALevel) {
        this.pdfALevel = pdfALevel;
    }
}
