package org.esupportail.esupsignature;

import org.esupportail.esupsignature.service.pdf.PdfService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;

import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EsupSignatureApplication.class)
@TestPropertySource(properties = {"app.scheduling.enable=false"})
public class PdfServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(PdfServiceTest.class);

    @Resource
    private PdfService pdfService;

    @Test
    public void testPdtAConversion() {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("dummy.pdf").getFile());
        try {
            pdfService.convertGS(new FileInputStream(file));
        } catch (Exception e) {
            logger.error("GhostScript convert not working, please check gs install or PDFA_def.ps and srgb.icc locations", e);
            fail();
        }

    }

}
