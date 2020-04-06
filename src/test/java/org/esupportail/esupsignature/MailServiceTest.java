package org.esupportail.esupsignature;

import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.service.mail.MailService;
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
import java.lang.reflect.Array;
import java.util.Arrays;

import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EsupSignatureApplication.class)
@TestPropertySource(properties = "app.scheduling.enable=false")
public class MailServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(MailServiceTest.class);

    @Resource
    private MailService mailService;

    @Test
    public void testMail() {
        try {
            mailService.sendTest(Arrays.asList(mailService.getMailConfig().getMailFrom()));
        } catch (Exception e) {
            logger.error("Send mail failed", e);
            fail();
        }
    }

}
