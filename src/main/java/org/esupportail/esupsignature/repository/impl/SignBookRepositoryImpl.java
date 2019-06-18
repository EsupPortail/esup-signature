package org.esupportail.esupsignature.repository.impl;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.repository.SignBookRepositoryCustom;
import org.springframework.stereotype.Repository;

@Repository
public class SignBookRepositoryImpl implements SignBookRepositoryCustom {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<SignBook> findBySignBookTypeFilterByCreateBy(SignBookType signBookType, String createBy) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<SignBook> query = criteriaBuilder.createQuery(SignBook.class);
        Root<SignBook> queryRoot = query.from(SignBook.class);
        
        final List<Predicate> predicates = new ArrayList<Predicate>();
        final List<Order> orders = new ArrayList<Order>();
        
       	predicates.add(criteriaBuilder.and(criteriaBuilder.notEqual(queryRoot.get("createBy"), createBy)));

        query.select(queryRoot);
        query.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
        query.orderBy(orders);
       	
       	return entityManager.createQuery(query).getResultList();
	}

	@Override
	public List<SignBook> findByNotCreateBy(String createBy) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<SignBook> query = criteriaBuilder.createQuery(SignBook.class);
        Root<SignBook> queryRoot = query.from(SignBook.class);
        
        final List<Predicate> predicates = new ArrayList<Predicate>();
        final List<Order> orders = new ArrayList<Order>();
        
       	predicates.add(criteriaBuilder.and(criteriaBuilder.notEqual(queryRoot.get("createBy"), createBy)));

        orders.add(criteriaBuilder.desc(queryRoot.get("signBookType")));
        orders.add(criteriaBuilder.asc(queryRoot.get("name")));
       	
        query.select(queryRoot);
        query.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
        query.orderBy(orders);
       	
       	return entityManager.createQuery(query).getResultList();
	}

	
}
