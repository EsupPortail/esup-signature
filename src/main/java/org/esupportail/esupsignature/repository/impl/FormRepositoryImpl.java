package org.esupportail.esupsignature.repository.impl;

import org.eclipse.persistence.internal.jpa.JPAQuery;
import org.eclipse.persistence.internal.jpa.querydef.PredicateImpl;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.FormRepositoryCustom;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class FormRepositoryImpl implements FormRepositoryCustom {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<Form> findAutorizedFormByUser(User user) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Form> query = criteriaBuilder.createQuery(Form.class);
        Root<Form> queryRoot = query.from(Form.class);
        query.select(queryRoot);
		Expression<Boolean> activeExpression = queryRoot.get("activeVersion");
		Expression<Boolean> publicUsageExpression = queryRoot.get("publicUsage");
		if(user.getRoles().size() >0 ) {
			Expression<String> roleExpression = queryRoot.get("role");
			query.where(criteriaBuilder.and(criteriaBuilder.or(publicUsageExpression.in(true), roleExpression.in(user.getRoles())), activeExpression.in(true)));
		} else {
			query.where(criteriaBuilder.and(activeExpression.in(true), publicUsageExpression.in(true)));
		}

       	return entityManager.createQuery(query).getResultList();
	}

}
