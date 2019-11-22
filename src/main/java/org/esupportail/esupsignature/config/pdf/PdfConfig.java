package org.esupportail.esupsignature.config.pdf;

import org.esupportail.esupsignature.config.sign.SignProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PdfProperties.class)
public class PdfConfig {

    private PdfProperties pdfProperties;

    public PdfConfig(PdfProperties pdfProperties) {
        this.pdfProperties = pdfProperties;
    }

    public PdfProperties getPdfProperties() {
        return pdfProperties;
    }
}
