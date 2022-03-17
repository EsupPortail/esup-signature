package org.esupportail.esupsignature.service.scheduler;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.service.OJService;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.exception.EsupSignatureMailException;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
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
	private WorkflowService workflowService;

	@Resource
	private UserService userService;

	@Resource
	private SignRequestRepository signRequestRepository;

	private OJService oJService;

	public ScheduledTaskService(GlobalProperties globalProperties) {
		this.globalProperties = globalProperties;
	}

	@Autowired(required = false)
	public void setoJService(OJService oJService) {
		this.oJService = oJService;
	}

	@Scheduled(initialDelay = 12000, fixedRate = 300000)
	@Transactional
	public void scanAllWorkflowsSources() throws EsupSignatureFsException {
		logger.debug("scan workflows sources");
		Iterable<Workflow> workflows = workflowService.getAllWorkflows();
		User userScheduler = userService.getSchedulerUser();
		for(Workflow workflow : workflows) {
			signBookService.importFilesFromSource(workflow.getId(), userScheduler, userScheduler);
		}
	}

	@Scheduled(initialDelay = 12000, fixedRate = 300000)
	@Transactional
	public void scanAllSignbooksTargets() {
		logger.debug("scan all signRequest to export");
		List<SignBook> signBooks = signBookRepository.findByStatus(SignRequestStatus.completed);
		for(SignBook signBook : signBooks) {
			try {
				if(signBook.getLiveWorkflow() != null && signBook.getLiveWorkflow().getTargets().size() > 0) {
					signBookService.sendSignRequestsToTarget(signBook.getSignRequests(), signBook.getSubject(), signBook.getLiveWorkflow().getTargets(), "scheduler");
				}
			} catch(EsupSignatureFsException | EsupSignatureException e) {
				logger.error(e.getMessage());
			}
		}
	}

	@Scheduled(initialDelay = 12000, fixedRate = 300000)
	public void scanAllSignbooksToArchive() {
		if(globalProperties.getEnableScheduledCleanup()) {
			signBookService.setEnableArchiveTask(true);
			signBookService.initArchive();
		}
	}

	@Scheduled(initialDelay = 12000, fixedRate = 300000)
	public void scanAllSignbooksToClean() {
		if(globalProperties.getEnableScheduledCleanup()) {
			signBookService.setEnableCleanTask(true);
			signBookService.initCleanning();
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

	@Scheduled(initialDelay = 86400000, fixedRate = 86400000)
	public void refreshOJKeystore() {
		if(oJService != null) {
			oJService.refresh();
		}
	}

	@Scheduled(initialDelay = 12000, fixedRate = 300000)
	@Transactional
	public void cleanWarningReadedSignRequests() {
		if(globalProperties.getNbDaysBeforeDeleting() > -1) {
			List<SignRequest> signRequests = signRequestRepository.findByOlderPendingAndWarningReaded(globalProperties.getNbDaysBeforeDeleting());
			for (SignRequest signRequest : signRequests) {
				signBookService.delete(signRequest.getParentSignBook().getId(), "scheduler");
			}
		}
	}

}
