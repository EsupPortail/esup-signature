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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@EnableScheduling
//@Profile("!dev")
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

	@Scheduled(initialDelay = 12000, fixedRate = 300000)
	public void scanAllSignbooksToArchive() {
		if(globalProperties.getEnableScheduledCleanup()) {
			taskService.initArchive();
		}
	}

	@Scheduled(initialDelay = 12000, fixedRate = 300000)
	public void scanAllSignbooksToClean() {
		if(globalProperties.getEnableScheduledCleanup()) {
			taskService.initCleanning("scheduler");
		}
	}

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

	@Scheduled(cron="00 02 02 * * *")
	public void cleanUploadingSignBooks() {
		taskService.initCleanUploadingSignBooks();
	}

	@Scheduled(cron="0 0 * * * *")
	public void refreshOJKeystore() {
		taskService.initDssRefresh();
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

	@Scheduled(initialDelay = 12000, fixedRate = 300000)
	@Transactional
	public void cleanOtps() {
		otpService.cleanEndedOtp();
	}

}
