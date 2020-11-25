package org.esupportail.esupsignature.config.pdf;

import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

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

    @Autowired
    private ResourceLoader resourceLoader;

    @Bean
    public void setPdfColorProfileUrl() {
        Resource resource = resourceLoader.getResource("classpath:srgb.icc");
        try {
            String iccPath = resource.getFile().getAbsolutePath();
            Path path = resourceLoader.getResource("classpath:PDFA_def.ps").getFile().toPath();
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            lines.set(7, "/ICCProfile (" + iccPath + ") % Customise");
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new EsupSignatureRuntimeException("unable to modify PDFA_def.ps");
        }

    }
}
