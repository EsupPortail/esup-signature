package org.esupportail.esupsignature.repository;

import java.util.List;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;

public interface SignBookRepositoryCustom {

    List<SignBook> findBySignBookTypeFilterByCreateBy(SignBookType signBookType, String createBy);

    List<SignBook> findByNotCreateBy(String createBy);
    
}
