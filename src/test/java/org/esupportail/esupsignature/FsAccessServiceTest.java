package org.esupportail.esupsignature;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessFactoryService;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EsupSignatureApplication.class)
@TestPropertySource(properties = {"app.scheduling.enable=false"})
public class FsAccessServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(FsAccessServiceTest.class);

    @Resource
    private FsAccessFactoryService fsAccessFactoryService;

    @Resource
    private GlobalProperties globalProperties;

    @Test
    public void testSmbAccessImpl() throws EsupSignatureFsException {
        FsAccessService fsAccessService = fsAccessFactoryService.getFsAccessService("smb://test");
        assumeTrue("SMB not configured", fsAccessService != null && fsAccessService.getUri() != null && !fsAccessService.getUri().isEmpty());
        try {
            if (fsAccessService.cd(fsAccessService.getUri()) == null) {
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
    public void testCmisAccessImpl() throws EsupSignatureFsException {
        FsAccessService fsAccessService = fsAccessFactoryService.getFsAccessService("cmis://test");
        assumeTrue("cmis not configured", fsAccessService != null && fsAccessService.getUri() != null && !fsAccessService.getUri().isEmpty());
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
    public void testVfsAccessImpl() throws EsupSignatureFsException {
        FsAccessService fsAccessService = fsAccessFactoryService.getFsAccessService("ftp://test");
        assumeTrue("vfs not configured", fsAccessService != null && fsAccessService.getUri() != null && !fsAccessService.getUri().isEmpty());
        try {
            if (fsAccessService.cd(fsAccessService.getUri()) == null) {
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
        FsAccessService fsAccessService = fsAccessFactoryService.getFsAccessService(globalProperties.getArchiveUri());
        fsAccessService.createURITree(globalProperties.getArchiveUri());
    }

}
