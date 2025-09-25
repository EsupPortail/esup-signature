package org.esupportail.esupsignature.service.scheduler;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureMailException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@EnableScheduling
@Profile("!dev")
@Component
@EnableConfigurationProperties(GlobalProperties.class)
public class ScheduledTaskService {

	private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);

	private final GlobalProperties globalProperties;
	private final SignBookRepository signBookRepository;
	private final SignBookService signBookService;
	private final TaskService taskService;
	private final WorkflowService workflowService;
	private final UserService userService;
	private final SignRequestRepository signRequestRepository;
	private final OtpService otpService;

	public ScheduledTaskService(GlobalProperties globalProperties, SignBookRepository signBookRepository, SignBookService signBookService, TaskService taskService, WorkflowService workflowService, UserService userService, SignRequestRepository signRequestRepository, OtpService otpService) {
        this.globalProperties = globalProperties;
        this.signBookRepository = signBookRepository;
        this.signBookService = signBookService;
        this.taskService = taskService;
        this.workflowService = workflowService;
        this.userService = userService;
        this.signRequestRepository = signRequestRepository;
		this.otpService = otpService;
	}

	/**
     * Scanne toutes les sources de workflows disponibles et tente
     * d'importer les fichiers associés depuis les sources configurées pour générer
     * des demandes de signature.
     *
     * Cette méthode est programmée pour être exécutée de manière régulière selon
     * un délai initial de 12 secondes et une fréquence d'exécution de 5 minutes.
     *
     * Le processus consiste à parcourir tous les workflows récupérés via le service
     * {@code workflowService}, puis, pour chacun, tente d'importer les fichiers depuis
     * leurs sources associées en utilisant le service {@code signBookService}.
     *
     * - Le processus est exécuté avec l'utilisateur technique {@code userScheduler}
     * - Journalisation si un problème survient lors de l'importation d'un workflow.
     */
    @Scheduled(initialDelay = 12000, fixedRate = 300000)
	public void scanAllWorkflowsSources() {
		logger.debug("scan workflows sources");
		Iterable<Workflow> workflows = workflowService.getAllWorkflows();
		User userScheduler = userService.getSchedulerUser();
		for(Workflow workflow : workflows) {
			try {
				signBookService.importFilesFromSource(workflow.getId(), userScheduler, userScheduler);
			} catch (EsupSignatureRuntimeException e) {
				logger.error("unable to import into " + workflow.getName(), e);
			}
		}
	}

	/**
     * Scanne l'ensemble des signataires des SignBooks complets et tente
     * d'envoyer leurs SignRequests aux cibles spécifiées.
     *
     * Cette méthode est planifiée pour s'exécuter périodiquement avec un délai
     * initial de 12 secondes et une fréquence d'exécution toutes les 30 secondes.
     *
     * Le processus suit les étapes suivantes :
     * - Récupère tous les SignBooks ayant le statut {@link SignRequestStatus#completed}
     *   et contenant des cibles de workflow actives.
     * - Pour chaque SignBook récupéré, appelle le service {@code signBookService}
     *   afin d'exporter ses Documents vers leurs cibles associées.
     * - Enregistre dans les logs toute erreur rencontrée lors de l'envoi d'une SignRequest
     *   pour permettre une analyse ultérieure.
     */
    @Scheduled(initialDelay = 12000, fixedRate = 30000)
	public void scanAllSignbooksTargets() {
		logger.debug("scan all signRequest to export");
		List<SignBook> signBooks = signBookRepository.findByStatusAndLiveWorkflowTargetsNotEmpty(SignRequestStatus.completed);
		for(SignBook signBook : signBooks) {
			try {
				signBookService.sendSignRequestsToTarget(signBook.getId(), "scheduler");
			} catch(Exception e) {
				logger.debug("export error for signbook " + signBook.getId() + " : " + e.getMessage());
			}
		}
	}


    /**
     * Programme une tâche pour scanner tous les SignBooks afin de détecter ceux qui
     * nécessitent une action d'archivage et initier leur traitement.
     *
     * Cette méthode est exécutée périodiquement avec un délai initial de 12 secondes
     * et une fréquence d'exécution de 5 minutes. Elle vérifie si la configuration
     * de nettoyage programmé est activée via la propriété
     * {@code globalProperties.getEnableScheduledCleanup()}. Si cette condition est remplie,
     * elle lance le processus d'archivage en utilisant le service {@code taskService}.
     *
     * Fonctionnement :
     * - Vérifie si le nettoyage programmé est activé.
     * - Si activé, lance l'initialisation de l'archivage des SignBooks éligibles.
     *
     * Conditions préalables :
     * - La configuration d'activation du nettoyage programmé
     *   (propriété {@code enableScheduledCleanup}) doit être vraie.
     *
     * Journalisation :
     * - L'état du processus d'archivage est enregistré, y compris les éventuelles erreurs
     *   survenues lors du traitement.
     */
    @Scheduled(initialDelay = 12000, fixedRate = 300000)
	public void scanAllSignbooksToArchive() {
		if(globalProperties.getEnableScheduledCleanup()) {
			taskService.initArchive();
		}
	}

	/**
     * Scanne l'ensemble des SignBooks afin d'effectuer un nettoyage des archives et des
     * éléments supprimés conformément aux politiques définies.
     *
     * Cette méthode est planifiée pour s'exécuter périodiquement avec un délai initial de
     * 12 secondes et une fréquence d'exécution toutes les 5 minutes.
     *
     * Fonctionnement :
     * - Vérifie si le nettoyage programmé est activé via la propriété
     *   {@code globalProperties.getEnableScheduledCleanup()}.
     * - En cas d'activation, initie un processus asynchrone de nettoyage via le service
     *   {@code taskService}.
     *
     * Tâches exécutées :
     * - Nettoyage des SignBooks archivés dont le statut est {@code archived}.
     * - Suppression définitive des SignBooks marqués comme supprimés et ayant dépassé le
     *   délai de conservation spécifié dans {@code globalProperties.getTrashKeepDelay()}.
     *
     * Journalisation :
     * - Début et fin des processus de nettoyage.
     * - Progression et éventuel arrêt des tâches de nettoyage.
     * - Nombre d'éléments nettoyés, par catégorie (archives et corbeille).
     *
     * Prérequis :
     * - La tâche est déclenchée uniquement si le nettoyage programmé est activé
     *   (propriété {@code enableScheduledCleanup}).
     * - Les paramètres de délai pour le nettoyage des archives et des éléments supprimés
     *   doivent être correctement configurés et supérieurs ou égaux à zéro.
     *
     * En cas d'erreur, les événements seront enregistrés dans les journaux pour analyse.
     */
    @Scheduled(initialDelay = 12000, fixedRate = 300000)
	public void scanAllSignbooksToClean() {
		if(globalProperties.getEnableScheduledCleanup()) {
			taskService.initCleanning("scheduler");
		}
	}

	/**
     * Envoie des alertes par email à tous les utilisateurs pour lesquels des notifications
     * sont activées et nécessaires.
     *
     * Cette méthode est planifiée pour s'exécuter périodiquement avec un délai initial
     * de 12 secondes et une fréquence d'exécution toutes les 5 minutes.
     *
     * Fonctionnement :
     * - Récupère la liste de tous les utilisateurs via le service {@code userService}.
     * - Pour chaque utilisateur, vérifie si une alerte email doit être envoyée grâce à
     *   la méthode {@code userService.checkEmailAlert}.
     * - Si une alerte est nécessaire, appelle le service {@code signBookService} pour
     *   envoyer un résumé de l'alerte par email à l'utilisateur correspondant.
     */
    @Scheduled(initialDelay = 12000, fixedRate = 300000)
	@Transactional
	public void sendAllEmailAlerts() throws EsupSignatureMailException {
		List<User> users = userService.getAllUsers();
		for(User user : users) {
			logger.trace("check email alert for " + user.getEppn());
			if(userService.checkEmailAlert(user)) {
				signBookService.sendEmailAlertSummary(user.getEppn());
			}
		}
	}

	/**
     * Programme une tâche pour nettoyer les SignBooks en cours de téléversement.
     *
     * Cette méthode est planifiée pour s'exécuter quotidiennement à 02h02, selon le
     * format cron spécifié.
     *
     * Notes :
     * - Cette tâche est destinée à supprimer ou traiter les SignBooks marqués comme
     *   étant en cours de téléversement, conformément aux politiques définies.
     */
    @Scheduled(cron="00 02 02 * * *")
	public void cleanUploadingSignBooks() {
		taskService.initCleanUploadingSignBooks();
	}

	/**
     * Planifie le rafraîchissement du keystore OJ (Object-Java) à une fréquence horaire.
     *
     * Cette méthode est exécutée automatiquement toutes les heures à l'aide de l'expression cron "0 0 * * * *".
     * Elle déclenche le processus d'initialisation du rafraîchissement via le service {@code taskService}.
     *
     * Notes :
     * - Le service DSS est utilisé pour assurer la mise à jour des certificats nécessaires.
     */
    @Scheduled(cron="0 0 * * * *")
	public void refreshOJKeystore() {
		taskService.initDssRefresh();
	}

	/**
     * Nettoie les demandes de signature ayant le statut "en attente" et dont les
     * avertissements ont été marqués comme lus, si elles dépassent un certain délai.
     *
     * Cette tâche est programmée pour s'exécuter périodiquement, avec un délai initial
     * de 12 secondes et une fréquence d'exécution de 5 minutes. La période de conservation
     * définie est évaluée à partir de la propriété {@code globalProperties.getNbDaysBeforeDeleting()}.
     *
     * Fonctionnement :
     * - Vérifie si la propriété {@code nbDaysBeforeDeleting} est activée et supérieure ou égale à 0.
     * - Récupère les demandes de signature ayant un avertissement lu et dépassant
     *   le délai configuré.
     * - Pour chaque demande identifiée, supprime le SignBook
     *   parent associé, en utilisant le service {@code signBookService}.
     *
     * Prérequis :
     * - La propriété {@code nbDaysBeforeDeleting} doit être configurée avec une
     *   valeur valide (supérieure ou égale à 0).
     */
    @Scheduled(initialDelay = 12000, fixedRate = 300000)
	@Transactional
	public void cleanWarningReadSignRequests() {
		if(globalProperties.getNbDaysBeforeDeleting() > -1) {
			List<SignRequest> signRequests = signRequestRepository.findByOlderPendingAndWarningReaded(globalProperties.getNbDaysBeforeDeleting());
			for (SignRequest signRequest : signRequests) {
				signBookService.delete(signRequest.getParentSignBook().getId(), "scheduler");
			}
		}
	}

	/**
     * Cette méthode programmée est exécutée de façon automatique à un intervalle fixe.
     * Elle supprime les OTP (One-Time Passwords) expirés ou obsolètes.
     *
     * La méthode s'exécute initialement avec un délai de 12 secondes après le démarrage
     * du système, puis elle est appelée régulièrement toutes les 5 minutes.
     */
    @Scheduled(initialDelay = 12000, fixedRate = 300000)
	@Transactional
	public void cleanOtps() {
		otpService.cleanEndedOtp();
	}

}
