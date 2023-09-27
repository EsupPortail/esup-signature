package org.esupportail.esupsignature.service.scheduler;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.service.OJService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@EnableScheduling
@Profile("!dev")
@Component
@EnableConfigurationProperties(GlobalProperties.class)
public class ScheduledTaskService {

	private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);

	private final GlobalProperties globalProperties;

	@Resource
	private SignBookRepository signBookRepository;

	@Resource
	private SignBookService signBookService;

	@Resource
	private TaskService taskService;

	@Resource
	private WorkflowService workflowService;

	@Resource
	private UserService userService;

	@Resource
	private SignRequestRepository signRequestRepository;

	private final OJService oJService;

	public ScheduledTaskService(GlobalProperties globalProperties, OJService oJService) {
		this.globalProperties = globalProperties;
		this.oJService = oJService;
	}

	@Scheduled(initialDelay = 12000, fixedRate = 300000)
	@Transactional
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

	@Scheduled(initialDelay = 12000, fixedRate = 300000)
	public void scanAllSignbooksTargets() {
		logger.debug("scan all signRequest to export");
		List<SignBook> signBooks = signBookRepository.findByStatus(SignRequestStatus.completed);
		for(SignBook signBook : signBooks) {
			try {
				signBookService.sendSignRequestsToTarget(signBook.getId(), "scheduler");
			} catch(Exception e) {
				logger.error("export error for signbook " + signBook.getId(), e);
			}
		}
	}

	@Scheduled(initialDelay = 12000, fixedRate = 300000)
	public void scanAllSignbooksToArchive() {
		if(globalProperties.getEnableScheduledCleanup()) {
			SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
			Date date = new Date(System.currentTimeMillis());
			System.out.println(formatter.format(date));
			logger.info("init archive : " + formatter.format(date));
			taskService.initArchive();
			Date date2 = new Date(System.currentTimeMillis());
			logger.info("end archive" + formatter.format(date2));

		}
	}

	@Scheduled(initialDelay = 12000, fixedRate = 300000)
	public void scanAllSignbooksToClean() {
		if(globalProperties.getEnableScheduledCleanup()) {
			SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
			Date date = new Date(System.currentTimeMillis());
			System.out.println(formatter.format(date));
			logger.info("init clean : " + formatter.format(date));
			taskService.initCleanning("scheduler");
			Date date2 = new Date(System.currentTimeMillis());
			logger.info("end clean : " + formatter.format(date2));
		}
	}

	@Scheduled(initialDelay = 12000, fixedRate = 300000)
	@Transactional
	public void sendAllEmailAlerts() throws EsupSignatureMailException {
		List<User> users = userService.getAllUsers();
		for(User user : users) {
			logger.trace("check email alert for " + user.getEppn());
			if(userService.checkEmailAlert(user)) {
				signBookService.sendEmailAlertSummary(user);
			}
		}
	}

	@Scheduled(initialDelay = 1000, fixedRate = 86400000)
	public void cleanUploadingSignBooks() {
		taskService.initCleanUploadingSignBooks();
	}

	@Scheduled(initialDelay = 86400000, fixedRate = 86400000)
	public void refreshOJKeystore() {
		if(oJService != null) {
			oJService.refresh();
		}
	}

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

}
