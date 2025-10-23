package org.esupportail.esupsignature.config.pdf;

import jakarta.annotation.PostConstruct;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Configuration
public class PdfConfig {

    private static final Logger logger = LoggerFactory.getLogger(PdfConfig.class);

    private final PdfProperties pdfProperties;

    public PdfConfig(PdfProperties pdfProperties) {
        this.pdfProperties = pdfProperties;
    }

    public PdfProperties getPdfProperties() {
        return pdfProperties;
    }

    private Path pdfADefPath;

    public Path getPdfADefPath() {
        return pdfADefPath;
    }

    @PostConstruct
    public void setPdfColorProfileUrl() throws IOException {
        OutputStream pdfAoutStream = null;
        OutputStream iccOutStream = null;
        try {
            String tmpDirectory = System.getProperty("java.io.tmpdir");
            File pdfAFile = new File(tmpDirectory, "PDFA_def.ps");
            pdfAoutStream = new FileOutputStream(pdfAFile);
            pdfAoutStream.write(new ClassPathResource("/PDFA_def.ps").getInputStream().readAllBytes());
            File iccFile = new File(tmpDirectory, "srgb.icc");
            iccOutStream = new FileOutputStream(iccFile);
            iccOutStream.write(new ClassPathResource("/srgb.icc").getInputStream().readAllBytes());
            logger.info("iccPath : " + iccFile.getAbsolutePath());
            logger.info("pdfADefPath : " + pdfAFile.getAbsolutePath());
            pdfADefPath = pdfAFile.toPath();
            List<String> lines = Files.readAllLines(Path.of(pdfAFile.getAbsolutePath()), StandardCharsets.UTF_8);
            lines.set(11, "/ICCProfile (" + iccFile.getAbsolutePath() + ") % Customise");
            Files.write(Path.of(pdfAFile.getAbsolutePath()), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("PDFA_def.ps read error", e);
            throw new EsupSignatureRuntimeException("unable to modify PDFA_def.ps", e);
        } finally {
            if(pdfAoutStream != null && iccOutStream != null) {
                pdfAoutStream.close();
                iccOutStream.close();
            }
        }

    }
}
