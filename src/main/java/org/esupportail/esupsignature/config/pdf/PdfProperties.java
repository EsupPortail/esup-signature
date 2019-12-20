package org.esupportail.esupsignature.config.pdf;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pdf")
public class PdfProperties {

    private int width = 594;
    private int height = 842;
    private int pdfToImageDpi = 150;
    private int signWidthThreshold = 150;
    private String pathToGS = "/usr/bin/gs";
    private boolean convertToPdfA = true;
    private int pdfALevel = 2;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getPdfToImageDpi() {
        return pdfToImageDpi;
    }

    public void setPdfToImageDpi(int pdfToImageDpi) {
        this.pdfToImageDpi = pdfToImageDpi;
    }

    public int getSignWidthThreshold() {
        return signWidthThreshold;
    }

    public void setSignWidthThreshold(int signWidthThreshold) {
        this.signWidthThreshold = signWidthThreshold;
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
