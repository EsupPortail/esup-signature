package org.esupportail.esupsignature;

import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;

import javax.annotation.Resource;

import org.esupportail.esupsignature.dss.service.OJService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EsupSignatureApplication.class)
@TestPropertySource(properties = {"app.scheduling.enable=false"})
public class DSSTest {

    @Resource
    private ObjectProvider<OJService> ojService;

    @Test
    public void dssTest() throws IOException {
    	assumeNotNull(ojService.getIfAvailable());
        ojService.getIfAvailable().getCertificats();
        assumeTrue("dss cache not fresh !", !ojService.getIfAvailable().checkOjFreshness());
    }

}
