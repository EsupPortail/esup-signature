package org.esupportail.esupsignature.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import javax.annotation.Resource;

import org.esupportail.esupsignature.dss.web.service.OJService;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.service.fs.EsupStockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ScheduledTaskService {

	private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);
	
	@Resource
	private SignBookService signBookService;

	@Resource
	private UserService userService;
	
	@Resource
	private OJService oJService;

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

	@Transactional
	public void scanAllSignbooksTargets() throws EsupSignatureException {
		List<SignBook> signBooks = signBookService.getAllSignBooks();
		for(SignBook signBook : signBooks) {
			signBookService.exportFilesToTarget(signBook, getSchedulerUser());
		}
	}
	
	@Transactional
	public void sendAllEmailAlerts() throws EsupSignatureException {
		List<User> users = userService.getAllUsers();
		for(User user : users) {
			logger.debug("check email alert for " + user.getEppn());
			if(userService.checkEmailAlert(user)) {
				userService.sendEmailAlert(user);
			}
		}
	}
	
	public void refreshOJKeystore() throws MalformedURLException, IOException {
		oJService.refresh();
	}
	
	
	public User getSchedulerUser() {
		User user = new User();
		user.setEppn("Scheduler");
		user.setIp("127.0.0.1");
		return user;
	}
}
