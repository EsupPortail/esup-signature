package org.esupportail.esupsignature.service.scheduler;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.service.DSSService;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.service.SignBookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
public class TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    private final GlobalProperties globalProperties;

    private final DSSService dssService;

    public TaskService(GlobalProperties globalProperties, @Autowired(required = false) DSSService dssService) {
        this.globalProperties = globalProperties;
        this.dssService = dssService;
    }

    @Resource
    private SignBookService signBookService;

    @Resource
    private SignBookRepository signBookRepository;

    private boolean enableArchiveTask = false;

    private boolean enableCleanTask = false;

    private boolean enableCleanUploadingSignBookTask = false;

    private boolean enableDssRefreshTask = false;

    public boolean isEnableArchiveTask() {
        return enableArchiveTask;
    }

    public void setEnableArchiveTask(boolean enableArchiveTask) {
        this.enableArchiveTask = enableArchiveTask;
    }


    public boolean isEnableCleanTask() {
        return enableCleanTask;
    }

    public void setEnableCleanTask(boolean enableCleanTask) {
        this.enableCleanTask = enableCleanTask;
    }

    public boolean isEnableCleanUploadingSignBookTask() {
        return enableCleanUploadingSignBookTask;
    }

    public void setEnableCleanUploadingSignBookTask(boolean enableCleanUploadingSignBookTask) {
        this.enableCleanUploadingSignBookTask = enableCleanUploadingSignBookTask;
    }

    public boolean isEnableDssRefreshTask() {
        return enableDssRefreshTask;
    }

    public void setEnableDssRefreshTask(boolean enableDssRefreshTask) {
        this.enableDssRefreshTask = enableDssRefreshTask;
    }

    @Async
    public void initCleanning(String userEppn) {
        if(globalProperties.getDelayBeforeCleaning() > -1 && !isEnableCleanTask()) {
            logger.info("start cleanning archives");
            setEnableCleanTask(true);
            List<SignBook> signBooks = signBookRepository.findByStatus(SignRequestStatus.archived);
            int i = 0;
            for (SignBook signBook : signBooks) {
                logger.info("clean signbook : " + signBook.getId());
                signBookService.cleanFiles(signBook.getId(), userEppn);
                i++;
                if(!isEnableCleanTask()) {
                    logger.info("cleanning stopped");
                    return;
                }
            }
            logger.info(i + " achived item are cleaned");
        } else {
            logger.debug("cleaning documents was skipped because neg value");
        }
        if(globalProperties.getTrashKeepDelay() > -1) {
            logger.info("start cleanning trashes");
            List<SignBook> signBooks = signBookRepository.findByDeletedIsTrue();
            int i = 0;
            for (SignBook signBook : signBooks) {
                Date date = signBook.getCreateDate();
                if (signBook.getUpdateDate() != null) {
                    date = signBook.getUpdateDate();
                }
                LocalDateTime deleteDate = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
                LocalDateTime nowDate = LocalDateTime.ofInstant(new Date().toInstant(), ZoneId.systemDefault());
                long nbDays = ChronoUnit.DAYS.between(deleteDate, nowDate);
                if (Math.abs(nbDays) >= globalProperties.getTrashKeepDelay()) {
                    signBookService.deleteDefinitive(signBook.getId(), userEppn);
                    i++;
                }
            }
            logger.info(i + " deleted item are cleaned");
        } else {
            logger.debug("cleaning trashes was skipped because neg value");
        }
        setEnableCleanTask(false);
    }


    @Async
    public void initArchive() {
        if(globalProperties.getArchiveUri() != null && !isEnableArchiveTask()) {
            setEnableArchiveTask(true);
            logger.debug("scan all signRequest to archive");
            List<Long> signBooksIds = signBookRepository.findIdByStatus(SignRequestStatus.completed);
            signBooksIds.addAll(signBookRepository.findIdByStatus(SignRequestStatus.refused));
            signBooksIds.addAll(signBookRepository.findIdByStatus(SignRequestStatus.exported));
            for(Long signBookId : signBooksIds) {
                try {
                    if(signBookService.needToBeExported(signBookId)) {
                        /* on n’archive pas les demandes en cours d’export */
                        continue;
                    }
                    signBookService.archiveSignRequests(signBookId, "scheduler");
                } catch(EsupSignatureRuntimeException e) {
                    logger.error(e.getMessage());
                }
                if(!isEnableArchiveTask()) {
                    logger.info("archiving stopped");
                    return;
                }
            }
        }
        setEnableArchiveTask(false);
    }

    @Async
    public void initCleanUploadingSignBooks() {
        if(!isEnableCleanUploadingSignBookTask()) {
            setEnableCleanUploadingSignBookTask(true);
            signBookService.cleanUploadingSignBooks();
            setEnableCleanUploadingSignBookTask(false);
        }

    }

    @Async
    public void initDssRefresh() {
        if(!isEnableDssRefreshTask()) {
            setEnableDssRefreshTask(true);
            try {
                if(dssService != null) {
                    dssService.refreshOj();
                }
            } catch (Exception e) {
                logger.error("Error updating certificates", e);
            }
            setEnableDssRefreshTask(false);
        }
    }

}
