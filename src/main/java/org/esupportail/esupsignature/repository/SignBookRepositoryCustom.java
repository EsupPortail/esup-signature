package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;

import java.util.List;

public interface SignBookRepositoryCustom {

    List<SignBook> findBySignBookTypeFilterByCreateBy(SignBookType signBookType, String createBy);

    List<SignBook> findByNotCreateBy(String createBy);
    
}
