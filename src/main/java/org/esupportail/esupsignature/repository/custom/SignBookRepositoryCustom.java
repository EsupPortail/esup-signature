package org.esupportail.esupsignature.repository.custom;

import org.esupportail.esupsignature.entity.SignBook;

import java.util.List;

public interface SignBookRepositoryCustom {
    List<SignBook> findByNotCreateBy(String createBy);
}
