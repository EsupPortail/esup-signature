package org.esupportail.esupsignature.repository.impl;

import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.repository.custom.WorkflowRepositoryCustom;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;
import java.util.List;

@Repository
public class WorkflowRepositoryImpl implements WorkflowRepositoryCustom {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<Workflow> findAutorizedWorkflowByUser(User user) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Workflow> query = criteriaBuilder.createQuery(Workflow.class);
        Root<Workflow> queryRoot = query.from(Workflow.class);
        query.select(queryRoot);
		Expression<Boolean> publicUsageExpression = queryRoot.get("publicUsage");
		if(user.getRoles().size() >0 ) {
			Expression<String> roleExpression = queryRoot.get("role");
			query.where(criteriaBuilder.and(criteriaBuilder.or(publicUsageExpression.in(true), roleExpression.in(user.getRoles()))));
		} else {
			query.where(criteriaBuilder.and(publicUsageExpression.in(true)));
		}

       	return entityManager.createQuery(query).getResultList();
	}

}
