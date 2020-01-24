package org.esupportail.esupsignature.repository.impl;

import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.SignRequestRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SignRequestRepositoryImpl implements SignRequestRepositoryCustom {

	@PersistenceContext
	private EntityManager entityManager;

    @Override
    public Page<SignRequest> findBySignResquestByStatus(SignRequestStatus status, Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<SignRequest> query = criteriaBuilder.createQuery(SignRequest.class);
        Root<SignRequest> queryRoot = query.from(SignRequest.class);
        CriteriaQuery<Long> count = criteriaBuilder.createQuery(Long.class);
        Root<SignRequest> countRoot = count.from(SignRequest.class);


        final List<Predicate> predicates = new ArrayList<Predicate>();
        final List<Predicate> predicatesCount = new ArrayList<Predicate>();

        if(status != null) {
            predicates.add(criteriaBuilder.or(criteriaBuilder.equal(queryRoot.get("status"), status)));
            predicatesCount.add(criteriaBuilder.or(criteriaBuilder.equal(countRoot.get("status"), status)));
        }

        query.select(queryRoot);
        query.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
        query.orderBy(QueryUtils.toOrders(pageable.getSort(), queryRoot, criteriaBuilder));

        count.select(criteriaBuilder.count(countRoot));
        count.where(criteriaBuilder.and(predicatesCount.toArray(new Predicate[predicatesCount.size()])));
        long nbSignRequest = entityManager.createQuery(count).getSingleResult();
        Page<SignRequest> page = new PageImpl<SignRequest>(entityManager.createQuery(query).setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize()).getResultList(), pageable, nbSignRequest);
        return page;
    }
}
