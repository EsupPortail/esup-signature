package org.esupportail.esupsignature;

import org.esupportail.esupsignature.dss.service.OJService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.IOException;

import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EsupSignatureApplication.class)
@TestPropertySource(properties = {"app.scheduling.enable=false"})
public class DSSTest {

    @Resource
    private ObjectProvider<OJService> ojService;

    @Test
    public void testDss() throws IOException {
    	assumeNotNull(ojService.getIfAvailable());
        ojService.getIfAvailable().getCertificats();
        assumeTrue("dss cache not fresh !", !ojService.getIfAvailable().checkOjFreshness());
    }

}
