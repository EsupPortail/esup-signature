package org.esupportail.esupsignature.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import javax.annotation.Resource;

import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.dss.web.service.OJService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ScheduledTaskService {

	@Resource
	private SignBookService signBookService;
	
	private OJService oJService = new OJService();

	@Transactional
	public void scanAllSignbooksSources() {
		List<SignBook> signBooks = SignBook.findAllSignBooks();
		for(SignBook signBook : signBooks) {
			signBookService.importFilesFromSource(signBook);
			
		}
	}
	
	public void refreshOJKeystore() throws MalformedURLException, IOException {
		oJService.refresh();
	}
	
}
