package org.esupportail.esupsignature.repository.impl;

import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.FormRepositoryCustom;
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
	public List<Form> findFormByUserAndActiveVersion(User user, Boolean activeVersion) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Form> query = criteriaBuilder.createQuery(Form.class);
        Root<Form> queryRoot = query.from(Form.class);
        query.select(queryRoot);
        Expression<String> roleExpression = queryRoot.get("role");
        query.where(roleExpression.in(user.getRoles()));
		Expression<Boolean> activeExpression = queryRoot.get("activeVersion");
		query.where(activeExpression.in(activeVersion));
       	return entityManager.createQuery(query).getResultList();
	}

}
