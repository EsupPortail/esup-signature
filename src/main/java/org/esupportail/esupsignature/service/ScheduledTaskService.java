package org.esupportail.esupsignature.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import javax.annotation.Resource;

import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.dss.web.service.OJService;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.service.fs.EsupStockException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ScheduledTaskService {

	@Resource
	private SignBookService signBookService;

	@Resource
	private UserService userService;
	
	@Resource
	private OJService oJService;

	@Transactional
	public void scanAllSignbooksSources() throws EsupSignatureIOException, EsupStockException {
		List<SignBook> signBooks = SignBook.findAllSignBooks();
		for(SignBook signBook : signBooks) {
			signBookService.importFilesFromSource(signBook, getSchedulerUser());
			
		}
	}

	@Transactional
	public void scanAllSignbooksTargets() throws EsupSignatureException {
		List<SignBook> signBooks = SignBook.findAllSignBooks();
		for(SignBook signBook : signBooks) {
			signBookService.exportFilesToTarget(signBook, getSchedulerUser());
		}
	}
	
	@Transactional
	public void sendAllEmailAlerts() throws EsupSignatureException {
		List<User> users = User.findAllUsers();
		for(User user : users) {
			userService.sendEmailAlert(user);
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
