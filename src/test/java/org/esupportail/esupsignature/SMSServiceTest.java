package org.esupportail.esupsignature;

import org.esupportail.esupsignature.service.interfaces.sms.SmsService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EsupSignatureApplication.class)
@TestPropertySource(properties = {"app.scheduling.enable=false"})
public class SMSServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(SMSServiceTest.class);

    @Autowired(required = false)
    private SmsService smsService;

    @Test
    public void testSms() {
        assumeTrue("SMS not configured",  smsService != null);
        try {
            smsService.testSms();
        } catch (Exception e) {
            logger.error("Send mail failed", e);
            fail();
        }
    }

}
