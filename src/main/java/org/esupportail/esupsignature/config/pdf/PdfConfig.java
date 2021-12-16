package org.esupportail.esupsignature.config.pdf;

import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
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
            Path iccPath ;
            Path pdfADefPath;
            if(pdfProperties.getIccPath() == null ) {
                iccPath = Path.of(new ClassPathResource("/srgb.icc").getPath());
            } else {
                iccPath = new File(pdfProperties.getIccPath()).toPath();
            }
            if(pdfProperties.getPdfADefPath() == null ) {
                pdfADefPath = Path.of(new ClassPathResource("/PDFA_def.ps").getPath());
            } else {
                pdfADefPath = new File(pdfProperties.getPdfADefPath()).toPath();
            }
            logger.info("iccPath : " + iccPath);
            logger.info("pdfADefPath : " + pdfADefPath);
            List<String> lines = Files.readAllLines(pdfADefPath, StandardCharsets.UTF_8);
            lines.set(7, "/ICCProfile (" + iccPath + ") % Customise");
            Files.write(pdfADefPath, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new EsupSignatureRuntimeException("unable to modify PDFA_def.ps");
        }

    }
}
