package org.esupportail.esupsignature;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EsupSignatureApplication.class)
@TestPropertySource(properties = {"app.scheduling.enable=false"})
public class PdfServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(PdfServiceTest.class);

    @Resource
    private PdfService pdfService;

    @Test
    public void testPdtAConversion() {
        try {
            pdfService.convertToPDFA(new ClassPathResource("/dummy.pdf").getInputStream().readAllBytes());
        } catch (Exception e) {
            logger.error("GhostScript convert not working, please check gs install or PDFA_def.ps and srgb.icc locations", e);
            fail();
        }

    }

}
