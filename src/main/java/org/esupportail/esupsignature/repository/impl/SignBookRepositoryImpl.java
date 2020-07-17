package org.esupportail.esupsignature.repository.impl;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.repository.custom.SignBookRepositoryCustom;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SignBookRepositoryImpl implements SignBookRepositoryCustom {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<SignBook> findByNotCreateBy(String createBy) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<SignBook> query = criteriaBuilder.createQuery(SignBook.class);
        Root<SignBook> queryRoot = query.from(SignBook.class);
        
        final List<Predicate> predicates = new ArrayList<>();
        final List<Order> orders = new ArrayList<>();
        
       	predicates.add(criteriaBuilder.and(criteriaBuilder.notEqual(queryRoot.get("createBy"), createBy)));

        orders.add(criteriaBuilder.desc(queryRoot.get("signBookType")));
        orders.add(criteriaBuilder.asc(queryRoot.get("name")));
       	
        query.select(queryRoot);
        query.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
        query.orderBy(orders);
       	
       	return entityManager.createQuery(query).getResultList();
	}

	
}
