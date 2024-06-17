//package org.esupportail.esupsignature;
//
//import jakarta.annotation.Resource;
//import org.esupportail.esupsignature.dss.service.DSSService;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import java.io.IOException;
//
//import static org.junit.Assume.assumeTrue;
//
//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = EsupSignatureApplication.class)
//@TestPropertySource(properties = {"app.scheduling.enable=false"})
//public class DSSTest {
//
//    private static final Logger logger = LoggerFactory.getLogger(DSSTest.class);
//
//    @Resource
//    private DSSService dssService;
//
//    @Test
//    public void testDss() throws IOException {
//        assumeTrue("DSS not configured",  dssService != null);
//        logger.info("Updating DSS OJ...");
//        dssService.getCertificats();
//        logger.info("Update done.");
//    }
//
//}
