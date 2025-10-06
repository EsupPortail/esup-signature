package org.esupportail.esupsignature.service.scheduler;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.service.DSSService;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.enums.ArchiveStatus;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.service.SignBookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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

    /**
     * Initialise une tâche asynchrone pour nettoyer les archives et les éléments supprimés
     * conformément aux politiques définies.
     *
     * Cette méthode effectue deux nettoyages distincts :
     * 1. Les archives : Les objets `SignBook` dont le statut est `archived` sont nettoyés.
     * 2. La corbeille : Les objets `SignBook` marqués comme supprimés sont définitivement
     *    effacés s'ils dépassent le délai défini dans `globalProperties.getTrashKeepDelay()`.
     *
     * Conditions :
     * - Le délai de nettoyage des archives (`globalProperties.getDelayBeforeCleaning()`) doit
     *   être supérieur ou égal à zéro.
     * - La tâche de nettoyage ne doit pas déjà être en cours d'exécution.
     * - Le délai de garde des éléments supprimés (`globalProperties.getTrashKeepDelay()`) doit
     *   être supérieur ou égal à zéro pour déclencher le nettoyage de la corbeille.
     *
     * Journalisation :
     * - Début et fin des processus de nettoyage.
     * - Progrès et arrêt éventuel de chaque tâche.
     * - Nombre d'éléments nettoyés pour les archives et la corbeille.
     */
    @Async
    public void initCleanning(String userEppn) {
        if(globalProperties.getDelayBeforeCleaning() > -1 && !isEnableCleanTask()) {
            logger.info("start cleanning archives");
            setEnableCleanTask(true);
            List<SignBook> signBooks = signBookRepository.findByArchiveStatus(ArchiveStatus.archived);
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


    /**
     * Lance une tâche asynchrone pour archiver les demandes de signature éligibles.
     *
     * Cette méthode parcourt les SignBooks identifiés comme devant être archivés
     * selon certains critères (statut `completed`, `refused` ou `exported` et
     * `archiveStatus` égal à `none` ou non défini). Si une demande est en cours
     * d'exportation, elle est ignorée. En cas d'erreur lors de l'archivage d'un
     * SignBook, celle-ci est enregistrée sans interrompre le processus global.
     *
     * Conditions préalables :
     * - Une URI d'archivage (`globalProperties.getArchiveUri()`) doit être définie.
     * - La tâche d'archivage ne doit pas déjà être en cours (`!isEnableArchiveTask()`).
     *
     * Journalisation :
     * - Début et fin du processus d'archivage.
     * - Événements notables tels que les arrêts ou erreurs spécifiques pour
     *   chaque SignBook.
     */
    @Async
    public void initArchive() {
        if(globalProperties.getArchiveUri() != null && !isEnableArchiveTask()) {
            setEnableArchiveTask(true);
            logger.debug("scan all signRequest to archive");
            List<Long> signBooksIds = signBookRepository.findIdToArchive();
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

    /**
     * Initialise une tâche asynchrone pour nettoyer les SignBooks en cours de téléversement.
     *
     * Cette méthode effectue le nettoyage en invoquant la méthode `cleanUploadingSignBooks`
     * du service `SignBookService`. Le nettoyage est encapsulé dans une gestion d'état qui
     * empêche plusieurs exécutions concurrentes de la tâche.
     */
    @Async
    public void initCleanUploadingSignBooks() {
        if(!isEnableCleanUploadingSignBookTask()) {
            setEnableCleanUploadingSignBookTask(true);
            signBookService.cleanUploadingSignBooks();
            setEnableCleanUploadingSignBookTask(false);
        }

    }

    /**
     * Méthode asynchrone pour initialiser le rafraîchissement DSS.
     *
     * Cette méthode vérifie si la tâche de rafraîchissement DSS est activée. Si elle
     * ne l'est pas, elle l'active, exécute le processus de rafraîchissement via le
     * service DSS, puis désactive la tâche. Une gestion des exceptions est incluse pour
     * capturer et enregistrer toute erreur survenant lors du rafraîchissement des certificats.
     */
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
