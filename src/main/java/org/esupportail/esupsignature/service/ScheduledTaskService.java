package org.esupportail.esupsignature.service;

import java.util.List;

import javax.annotation.Resource;

import org.esupportail.esupsignature.domain.SignBook;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ScheduledTaskService {

	@Resource
	SignBookService signBookService;

	@Transactional
	public void scanAllSignbooksSources() {
		List<SignBook> signBooks = SignBook.findAllSignBooks();
		for(SignBook signBook : signBooks) {
			signBookService.importFilesFromSource(signBook);
			
		}
		
	}
	
}
