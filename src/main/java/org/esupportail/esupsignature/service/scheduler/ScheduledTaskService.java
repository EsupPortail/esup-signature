package org.esupportail.esupsignature.service.scheduler;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.service.OJService;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@ConditionalOnProperty(value = "app.scheduling.enable", havingValue = "true", matchIfMissing = true)
@EnableScheduling
@Component
public class ScheduledTaskService {

	private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);

	@Resource
	private GlobalProperties globalProperties;

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

	@Autowired
	private ObjectProvider<OJService> oJService;

	@Scheduled(fixedRate = 300000)
	@Transactional
	public void scanAllSignbooksSources() {
		Iterable<Workflow> workflows = workflowService.getAllWorkflows();
		User userScheduler = userService.getSchedulerUser();
		for(Workflow workflow : workflows) {
			workflowService.importFilesFromSource(workflow, userScheduler, userScheduler);
		}
	}

	@Scheduled(fixedRate = 10000)
	@Transactional
	public void scanAllSignbooksTargets() {
		logger.trace("scan all signRequest to export");
		List<SignBook> signBooks = signBookRepository.findByStatus(SignRequestStatus.completed);
		for(SignBook signBook : signBooks) {
			try {
				signBookService.exportFilesToTarget(signBook, "scheduler");
				if(globalProperties.getArchiveUri() != null) {
					signBookService.archivesFiles(signBook, "scheduler");
				}
			} catch (EsupSignatureException e) {
				logger.error(e.getMessage());
			}
		}
	}

	@Scheduled(initialDelay = 120000, fixedRate = 300000)
	@Transactional
	public void scanAllSignbooksToClean() {
		logger.trace("scan all signRequest to export");
		if(globalProperties.getDelayBeforeCleaning() > -1) {
			List<SignBook> signBooks = signBookRepository.findByStatus(SignRequestStatus.archived);
			for (SignBook signBook : signBooks) {
				signBookService.cleanFiles(signBook, "scheduler");
			}
		} else {
			logger.debug("cleaning documents was skipped because neg value");
		}
	}

	@Scheduled(fixedRate = 300000)
	@Transactional
	public void sendAllEmailAlerts() {
		List<User> users = userService.getAllUsers();
		for(User user : users) {
			logger.trace("check email alert for " + user.getEppn());
			if(userService.checkEmailAlert(user)) {
				signRequestService.sendEmailAlertSummary(user);
			}
		}
	}

	@Scheduled(initialDelay = 86400000, fixedRate = 86400000)
	public void refreshOJKeystore() {
		oJService.ifAvailable(OJService::refresh);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void init() throws EsupSignatureException {
		workflowService.copyClassWorkflowsIntoDatabase();
		oJService.ifAvailable(OJService::getCertificats);
	}

}
