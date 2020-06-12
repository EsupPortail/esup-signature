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
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
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
	private WorkflowService workflowService;

	@Resource
	private UserService userService;

	@Resource
	private OJService oJService;

	//@Scheduled(fixedRate = 600000)
	@Transactional
	public void scanAllSignbooksSources() {
		Iterable<Workflow> workflows = workflowService.getAllWorkflows();
		for(Workflow workflow : workflows) {
			workflowService.importFilesFromSource(workflow, userService.getSchedulerUser());
		}
	}

	@Scheduled(initialDelay = 120000, fixedRate = 36000000)
	@Transactional
	public void scanAllSignbooksTargets() {
		logger.trace("scan all signRequest to export");
		List<SignBook> signBooks = signBookRepository.findByStatus(SignRequestStatus.completed);
		for(SignBook signBook : signBooks) {
			try {
				signBookService.exportFilesToTarget(signBook);
				signBookService.archivesFiles(signBook);
			} catch (EsupSignatureException e) {
				logger.error(e.getMessage());
			}
		}
	}

	@Scheduled(initialDelay = 120000, fixedRate = 36000000)
	@Transactional
	public void scanAllSignbooksToClean() throws EsupSignatureException {
		logger.trace("scan all signRequest to export");
		if(globalProperties.getDelayBeforeCleaning() > -1) {
			List<SignBook> signBooks = signBookRepository.findByStatus(SignRequestStatus.archived);
			for (SignBook signBook : signBooks) {
				signBookService.cleanFiles(signBook);
			}
		} else {
			logger.debug("cleanning documents was skipped because neg valu");
		}
	}



	//	@Scheduled(fixedRate = 300000)
	@Transactional
	public void sendAllEmailAlerts() {
		List<User> users = userService.getAllUsers();
		for(User user : users) {
			logger.trace("check email alert for " + user.getEppn());
			if(userService.checkEmailAlert(user)) {
				userService.sendEmailAlertSummary(user);
			}
		}
	}

	@Scheduled(initialDelay = 1000, fixedDelay=Long.MAX_VALUE)
	public void getOJKeystore() throws IOException {
		oJService.getCertificats();
	}

	@Scheduled(initialDelay = 86400000, fixedRate = 86400000)
	public void refreshOJKeystore() throws IOException {
		oJService.refresh();
	}
	
	

}
