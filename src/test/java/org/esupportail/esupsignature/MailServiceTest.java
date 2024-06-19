package org.esupportail.esupsignature;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.service.mail.MailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EsupSignatureApplication.class)
@TestPropertySource(properties = {"app.scheduling.enable=false"})
public class MailServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(MailServiceTest.class);

    @Resource
    private MailService mailService;

    @Test
    public void testMail() {
        assumeTrue( mailService.getMailConfig() != null && mailService.getMailConfig().getMailFrom()!= null && mailService.getMailSender() != null, "SMTP not configured");
        try {
            mailService.sendTest(Arrays.asList(mailService.getMailConfig().getMailFrom()));
        } catch (Exception e) {
            logger.error("Send mail failed", e);
            fail();
        }
    }

}
