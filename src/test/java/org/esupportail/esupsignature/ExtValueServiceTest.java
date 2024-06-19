package org.esupportail.esupsignature;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.interfaces.extvalue.ExtValue;
import org.esupportail.esupsignature.service.interfaces.extvalue.ExtValueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EsupSignatureApplication.class)
@TestPropertySource(properties = {"app.scheduling.enable=false"})
public class ExtValueServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(ExtValueServiceTest.class);

    @Resource
    private ExtValueService extValueService;

    @Test
    public void testExtValues() {
        boolean extValueTest = true;
        for(ExtValue extValue : extValueService.getExtValues()) {
            try {
                if (extValue.initValues(new User(), null) != null) {
                    logger.info("Test ExtValue : " + extValue.getName() + " OK");
                } else {
                    extValueTest = false;
                    logger.info("Test ExtValue : " + extValue.getName() + " KO");
                }
            } catch (Exception e) {
                logger.info("Test ExtValue : " + extValue.getName() + " KO", e);
                extValueTest = false;
            }
        }
        if(!extValueTest) {
            fail();
        }
    }

}
