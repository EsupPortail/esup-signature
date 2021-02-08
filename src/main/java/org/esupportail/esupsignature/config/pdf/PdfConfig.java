package org.esupportail.esupsignature.config.pdf;

import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

    @Bean
    public void setPdfColorProfileUrl() {
        try {
            Path iccPath = Path.of(PdfConfig.class.getResource("/srgb.icc").getPath());
            Path pdfADefPath = Path.of(PdfConfig.class.getResource("/PDFA_def.ps").getPath());
            List<String> lines = Files.readAllLines(pdfADefPath, StandardCharsets.UTF_8);
            lines.set(7, "/ICCProfile (" + iccPath.toString() + ") % Customise");
            Files.write(pdfADefPath, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new EsupSignatureRuntimeException("unable to modify PDFA_def.ps");
        }

    }
}
