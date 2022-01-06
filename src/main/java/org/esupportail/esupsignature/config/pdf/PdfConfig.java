package org.esupportail.esupsignature.config.pdf;

import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Configuration
@EnableConfigurationProperties(PdfProperties.class)
public class PdfConfig {

    private static final Logger logger = LoggerFactory.getLogger(PdfConfig.class);

    private final PdfProperties pdfProperties;

    public PdfConfig(PdfProperties pdfProperties) {
        this.pdfProperties = pdfProperties;
    }

    public PdfProperties getPdfProperties() {
        return pdfProperties;
    }

    @PostConstruct
    public void setPdfColorProfileUrl() {
        try {
            File pdfAFile = File.createTempFile("PDFA_def", "ps");
            OutputStream pdfAoutStream = new FileOutputStream(pdfAFile);
            pdfAoutStream.write(new ClassPathResource("/PDFA_def.ps").getInputStream().readAllBytes());

            File iccFile = File.createTempFile("srgb", "icc");
            OutputStream iccOutStream = new FileOutputStream(iccFile);
            iccOutStream.write(new ClassPathResource("/srgb.icc").getInputStream().readAllBytes());

            logger.info("iccPath : " + iccFile.getAbsolutePath());
            logger.info("pdfADefPath : " + pdfAFile.getAbsolutePath());
            List<String> lines = Files.readAllLines(Path.of(pdfAFile.getAbsolutePath()), StandardCharsets.UTF_8);
            lines.set(7, "/ICCProfile (" + iccFile.getAbsolutePath() + ") % Customise");
            Files.write(Path.of(pdfAFile.getAbsolutePath()), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("PDFA_def.ps read error", e);
            throw new EsupSignatureRuntimeException("unable to modify PDFA_def.ps", e);
        }

    }
}
