package org.esupportail.esupsignature;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessFactory;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EsupSignatureApplication.class)
@TestPropertySource(properties = {"app.scheduling.enable=false"})
public class FsAccessServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(FsAccessServiceTest.class);

    @Resource
    private FsAccessFactory fsAccessFactory;

    @Resource
    private GlobalProperties globalProperties;

    @Test
    public void testSmbAccessImpl() {
        FsAccessService fsAccessService = fsAccessFactory.getFsAccessService(DocumentIOType.smb);
        assumeTrue("SMB not configured", fsAccessService != null && fsAccessService.getUri() != null);
        try {
            if (fsAccessService.cd("/") == null) {
                logger.error(fsAccessService.getDriveName() + " unable to change to / directory. Please check configuration");
                fail();
            } else {
                logger.info(fsAccessService.getDriveName() + " ready");
            }
        } catch (Exception e) {
            logger.error(fsAccessService.getDriveName() + " configuration error. You can disable it in application.yml", e.getMessage());
            fail();
        }

    }

    @Test
    public void testCmisAccessImpl() {
        FsAccessService fsAccessService = fsAccessFactory.getFsAccessService(DocumentIOType.cmis);
        assumeTrue("cmis not configured", fsAccessService != null && fsAccessService.getUri() != null);
        try {
            if (fsAccessService.cd("/") == null) {
                logger.error(fsAccessService.getDriveName() + " unable to change to / directory. Please check configuration");
                fail();
            } else {
                logger.info(fsAccessService.getDriveName() + " ready");
            }
        } catch (Exception e) {
            logger.error(fsAccessService.getDriveName() + " configuration error. You can disable it in application.yml", e);
            fail();
        }
    }

    @Test
    public void testVfsAccessImpl() {
        FsAccessService fsAccessService = fsAccessFactory.getFsAccessService(DocumentIOType.vfs);
        assumeTrue("vfs not configured", fsAccessService != null && fsAccessService.getUri() != null);
        try {
            if (fsAccessService.cd("/") == null) {
                logger.error(fsAccessService.getDriveName() + " unable to change to / directory. Please check configuration");
                fail();
            } else {
                logger.info(fsAccessService.getDriveName() + " ready");
            }
        } catch (Exception e) {
            logger.error(fsAccessService.getDriveName() + "configuration error. You can disable it in application.properties", e.getMessage());
            fail();
        }
    }

    @Test
    public void testArchiveUri() throws EsupSignatureFsException {
        assumeTrue("archive url not configured", globalProperties.getArchiveUri() != null);
        FsAccessService fsAccessService = fsAccessFactory.getFsAccessService(fsAccessFactory.getPathIOType(globalProperties.getArchiveUri()));
        fsAccessService.createURITree(globalProperties.getArchiveUri());
    }

}
