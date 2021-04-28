package org.esupportail.esupsignature.repository.impl;

import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.repository.custom.FormRepositoryCustom;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;
import java.util.List;

@Repository
public class FormRepositoryImpl implements FormRepositoryCustom {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<Form> findAuthorizedFormByRole(String role) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Form> query = criteriaBuilder.createQuery(Form.class);
        Root<Form> queryRoot = query.from(Form.class);
        query.select(queryRoot);
		Expression<Boolean> activeExpression = queryRoot.get("activeVersion");
		Expression<Boolean> publicUsageExpression = queryRoot.get("publicUsage");
		if(role != null) {
			Expression<List<String>> roleExpression = queryRoot.get("roles");
			query.where(criteriaBuilder.and(criteriaBuilder.or(publicUsageExpression.in(true), roleExpression.in(role)), activeExpression.in(true)));
		} else {
			query.where(criteriaBuilder.and(activeExpression.in(true), publicUsageExpression.in(true)));
		}

       	return entityManager.createQuery(query).getResultList();
	}

}
