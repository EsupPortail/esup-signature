package org.esupportail.esupsignature.service.scheduler;

import org.esupportail.esupsignature.dss.web.service.OJService;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.fs.EsupStockException;
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
	private SignBookService signBookService;

	@Resource
	private SignRequestService signRequestService;

	@Resource
	private SignRequestRepository signRequestRepository;

	@Resource
	private UserService userService;
	
	@Resource
	private OJService oJService;

	@Scheduled(fixedRate = 100000)
	@Transactional
	public void scanAllSignbooksSources() throws EsupStockException {
		List<SignBook> signBooks = signBookService.getAllSignBooks();
		for(SignBook signBook : signBooks) {
			try {
				signBookService.importFilesFromSource(signBook, getSchedulerUser());
			} catch (EsupSignatureIOException e) {
				logger.warn(e.getMessage());
			}
			
		}
	}

	@Scheduled(fixedRate = 10000)
	@Transactional
	public void scanAllSignbooksTargets() {
		logger.trace("scan all signRequest to export");
		List<SignRequest> signRequests = signRequestRepository.findByStatusAndDocumentsTargetUriIsNotNull(SignRequest.SignRequestStatus.completed);
		for(SignRequest signRequest : signRequests) {
			try {
				signRequestService.exportFilesToTarget(signRequest, getSchedulerUser());
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
						   
	//@Scheduled(initialDelay = 10000, fixedRate = 8640000)
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
