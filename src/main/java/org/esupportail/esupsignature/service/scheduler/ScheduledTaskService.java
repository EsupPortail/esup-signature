package org.esupportail.esupsignature.service.scheduler;

import org.esupportail.esupsignature.dss.web.service.OJService;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.repository.WorkflowRepository;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Component
public class ScheduledTaskService {

	private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);

	@Resource
	private SignBookRepository signBookRepository;

	@Resource
	private SignBookService signBookService;

	@Resource
	private WorkflowService workflowService;

	@Resource
	private WorkflowRepository workflowRepository;

	@Resource
	private SignRequestService signRequestService;

	@Resource
	private SignRequestRepository signRequestRepository;

	@Resource
	private UserService userService;
	
	@Resource
	private OJService oJService;

	//@Scheduled(fixedRate = 10000)
	@Transactional
	public void scanAllSignbooksSources() {
		Iterable<Workflow> workflows = workflowRepository.findAll();
		for(Workflow workflow : workflows) {
			workflowService.importFilesFromSource(workflow, getSchedulerUser());
		}
	}

	@Scheduled(fixedRate = 10000)
	@Transactional
	public void scanAllSignbooksTargets() {
		logger.trace("scan all signRequest to export");
		List<SignBook> signBooks = signBookRepository.findByStatusAndDocumentsTargetUriIsNotNull(SignRequestStatus.completed);
		for(SignBook signBook : signBooks) {
			try {
				signBookService.exportFilesToTarget(signBook, getSchedulerUser());
			} catch (EsupSignatureException e) {
				logger.error(e.getMessage());
			}
		}
	}
	
	@Scheduled(fixedRate = 10000)
	@Transactional
	public void sendAllEmailAlerts() {
		List<User> users = userService.getAllUsers();
		for(User user : users) {
			logger.trace("check email alert for " + user.getEppn());
			if(userService.checkEmailAlert(user)) {
				userService.sendEmailAlert(user);
			}
		}
	}
						   
	@Scheduled(initialDelay = 8640000, fixedRate = 8640000)
	public void refreshOJKeystore() {
		oJService.refresh();
	}
	
	
	public User getSchedulerUser() {
		User user = new User();
		user.setEppn("Scheduler");
		user.setIp("127.0.0.1");
		return user;
	}
}
