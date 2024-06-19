package org.esupportail.esupsignature;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.dss.service.DSSService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EsupSignatureApplication.class)
@TestPropertySource(properties = {"app.scheduling.enable=false"})
public class DSSTest {

    @Resource
    private DSSService dssService;

    @Test
    public void testDss() throws IOException {
        assumeTrue(dssService != null, "DSS not configured");
        assertFalse(dssService.refreshIsNeeded());
    }

}
