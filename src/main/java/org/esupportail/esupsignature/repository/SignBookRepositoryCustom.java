package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.SignBook;

import java.util.List;

public interface SignBookRepositoryCustom {
    List<SignBook> findByNotCreateBy(String createBy);
}
