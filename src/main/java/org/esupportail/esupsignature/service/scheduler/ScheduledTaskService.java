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
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@ConditionalOnProperty(value = "app.scheduling.enable", havingValue = "true", matchIfMissing = true)
@EnableScheduling
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
	private SignRequestService signRequestService;

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
					signRequestService.sendSignRequestsToTarget(signBook.getSignRequests(), signBook.getName(), signBook.getLiveWorkflow().getTargets(), "scheduler");
				}
			} catch (EsupSignatureFsException | EsupSignatureException e) {
				logger.error(e.getMessage());
			}
		}
	}

	@Scheduled(initialDelay = 12000, fixedRate = 300000)
	@Transactional
	public void scanAllSignbooksToArchive() {
		if(globalProperties.getArchiveUri() != null) {
			logger.debug("scan all signRequest to archive");
			List<SignBook> signBooks = signBookRepository.findByStatus(SignRequestStatus.completed);
			signBooks.addAll(signBookRepository.findByStatus(SignRequestStatus.exported));
			for (SignBook signBook : signBooks) {
				try {
					if(signBook.getStatus().equals(SignRequestStatus.completed) && signBook.getLiveWorkflow() != null && signBook.getLiveWorkflow().getTargets().size() > 0) {
						continue;
					}
					signRequestService.archiveSignRequests(signBook.getSignRequests(), "scheduler");
				} catch (EsupSignatureFsException | EsupSignatureException e) {
					logger.error(e.getMessage());
				}
			}
		}
	}

	@Scheduled(initialDelay = 12000, fixedRate = 300000)
	@Transactional
	public void scanAllSignbooksToClean() {
		logger.debug("scan all signRequest to clean");
		if(globalProperties.getDelayBeforeCleaning() > -1) {
			List<SignBook> signBooks = signBookRepository.findByStatus(SignRequestStatus.archived);
			for (SignBook signBook : signBooks) {
				signRequestService.cleanFiles(signBook, "scheduler");
			}
		} else {
			logger.debug("cleaning documents was skipped because neg value");
		}
		if(globalProperties.getTrashKeepDelay() > -1) {
			List<SignBook> signBooks = signBookRepository.findByStatus(SignRequestStatus.deleted);
			int i = 0;
			for (SignBook signBook : signBooks) {
				if (signBook.getUpdateDate() != null) {
					LocalDateTime deleteDate = LocalDateTime.ofInstant(signBook.getUpdateDate().toInstant(), ZoneId.systemDefault());
					LocalDateTime nowDate = LocalDateTime.ofInstant(new Date().toInstant(), ZoneId.systemDefault());
					long nbDays = ChronoUnit.DAYS.between(deleteDate, nowDate);
					if (Math.abs(nbDays) >= globalProperties.getTrashKeepDelay()) {
						signBookService.deleteDefinitive(signBook.getId());
						i++;
					}
				}
			}
			if(i > 0) {
				logger.info(i + " item are deleted");
			}
		} else {
			logger.debug("cleaning trashes was skipped because neg value");
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

	@Async
	@EventListener(ApplicationReadyEvent.class)
	public void init() throws EsupSignatureException {
		logger.info("Checking Workflow classes...");
		workflowService.copyClassWorkflowsIntoDatabase();
		logger.info("Check done.");
		if(oJService != null) {
			logger.info("Updating DSS OJ...");
			oJService.getCertificats();
			logger.info("Update done.");
		}
	}

}
