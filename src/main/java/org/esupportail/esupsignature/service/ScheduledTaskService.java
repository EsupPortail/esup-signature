package org.esupportail.esupsignature.service;

import java.util.List;

import javax.annotation.Resource;
import javax.transaction.Transactional;

import org.esupportail.esupsignature.domain.SignBook;

@Transactional
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
