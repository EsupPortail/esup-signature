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

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
            byte[] source = new ClassPathResource("/dummy.pdf").getInputStream().readAllBytes();
            byte[] result = pdfService.convertToPDFA(source);
            assertNotNull(result, "La conversion PDF/A ne doit pas retourner null.");
            assertTrue(result.length > 4, "Le résultat de conversion PDF/A doit contenir un PDF non vide.");
            assertEquals("%PDF", new String(result, 0, 4, StandardCharsets.US_ASCII), "Le résultat de conversion PDF/A doit rester un document PDF valide.");
        } catch (Exception e) {
            logger.error("GhostScript convert not working, please check gs install or PDFA_def.ps and srgb.icc locations", e);
            fail();
        }

    }

}
