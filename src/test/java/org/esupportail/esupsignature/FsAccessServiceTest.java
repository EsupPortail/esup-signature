package org.esupportail.esupsignature;

import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.service.fs.FsAccessFactory;
import org.esupportail.esupsignature.service.fs.FsAccessService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.annotation.BeforeTestMethod;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import javax.validation.constraints.AssertTrue;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EsupSignatureApplication.class)
@TestPropertySource(properties = "app.scheduling.enable=false")
public class FsAccessServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(FsAccessServiceTest.class);

    @Resource
    private FsAccessFactory fsAccessFactory;

    @Test(timeout=5000)
    public void testSmbAccessImpl() {
        FsAccessService fsAccessService = fsAccessFactory.getFsAccessService(DocumentIOType.smb);
        assumeTrue("smb not configured", fsAccessService.getUri() != null);
        try {
            if (fsAccessService.cd("/") == null) {
                logger.error(fsAccessService.getDriveName() + "unable to change to / directory ");
                fail();
            }
        } catch (Exception e) {
            logger.error(fsAccessService.getDriveName() + "configuration error : ", e.getMessage());
            fail();
        }

    }

    @Test(timeout=5000)
    public void testCmisAccessImpl() {
        FsAccessService fsAccessService = fsAccessFactory.getFsAccessService(DocumentIOType.cmis);
        assumeTrue("cmis not configured", fsAccessService.getUri() != null);
        try {
            if (fsAccessService.cd("/") == null) {
                logger.error(fsAccessService.getDriveName() + "unable to change to / directory ");
                fail();
            }
        } catch (Exception e) {
            logger.error(fsAccessService.getDriveName() + "configuration error : ", e.getMessage());
            fail();
        }
    }

    @Test(timeout=5000)
    public void testVfsAccessImpl() {
        FsAccessService fsAccessService = fsAccessFactory.getFsAccessService(DocumentIOType.vfs);
        assumeTrue("vfs not configured", fsAccessService.getUri() != null);
        try {
            if (fsAccessService.cd("/") == null) {
                logger.error(fsAccessService.getDriveName() + "unable to change to / directory ");
                fail();
            }
        } catch (Exception e) {
            logger.error(fsAccessService.getDriveName() + "configuration error : ", e.getMessage());
            fail();
        }
    }

}
