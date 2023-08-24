package org.esupportail.esupsignature;

import org.esupportail.esupsignature.service.mail.MailService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import jakarta.annotation.Resource;
import java.util.Arrays;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EsupSignatureApplication.class)
@TestPropertySource(properties = {"app.scheduling.enable=false"})
public class MailServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(MailServiceTest.class);

    @Resource
    private MailService mailService;

    @Test
    public void testMail() {
        assumeTrue("SMTP not configured",  mailService.getMailConfig() != null && mailService.getMailConfig().getMailFrom()!= null && mailService.getMailSender() != null);
        try {
            mailService.sendTest(Arrays.asList(mailService.getMailConfig().getMailFrom()));
        } catch (Exception e) {
            logger.error("Send mail failed", e);
            fail();
        }
    }

}
