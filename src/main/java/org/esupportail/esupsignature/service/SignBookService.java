package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.domain.SignBook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SignBookService {
	
    public void create(SignBook signBook) {
        signBook.persist();
        throw new RuntimeException("oups :)");
    }
    
}
