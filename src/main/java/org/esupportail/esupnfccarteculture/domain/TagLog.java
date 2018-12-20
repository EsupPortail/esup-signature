/**
 * Licensed to ESUP-Portail under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * ESUP-Portail licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.esupportail.esupnfccarteculture.domain;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.persistence.ManyToOne;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.addon.tostring.RooToString;

@RooJavaBean
@RooToString
@RooJpaActiveRecord(finders = { "findTagLogsBySalle", "findTagLogsByEtudiant", "findTagLogsByDateBetween", "findTagLogsByDateBetweenAndSalle" })
public class TagLog {

	protected final static Logger log = LoggerFactory.getLogger(Etudiant.class);
	
    @ManyToOne
    private Etudiant etudiant;

    @ManyToOne
    private Salle salle;

    @DateTimeFormat(style = "MM")
    private Date date;

    private int tarif;

    private String eppnInit;

    public String getEtudiantNomPrenom() {
        return this.etudiant.getNom() + " " + etudiant.getPrenom();
    }

    public long getEtudiantId() {
        return this.etudiant.getId();
    }

    public String getSalleNom() {
        return this.salle.getNom();
    }

    public static TypedQuery<TagLog> findTagLogsBySalleAndDateBetween(Salle salle, Date minDate, Date maxDate) {
        if (minDate == null) throw new IllegalArgumentException("The minDate argument is required");
        if (maxDate == null) throw new IllegalArgumentException("The maxDate argument is required");
        EntityManager em = TagLog.entityManager();
        TypedQuery<TagLog> q = em.createQuery("SELECT o FROM TagLog AS o WHERE o.salle = :salle AND o.date BETWEEN :minDate AND :maxDate ORDER BY o.date DESC", TagLog.class);
        q.setParameter("minDate", minDate);
        q.setParameter("maxDate", maxDate);
        q.setParameter("salle", salle);
        return q;
    }

    public String getTarifEuro() {
        BigDecimal tarifBig = BigDecimal.valueOf(this.tarif);
        DecimalFormat decimalFormat = new java.text.DecimalFormat("####.00");
        return decimalFormat.format(tarifBig.divide(BigDecimal.valueOf(100)));
    }

    public static List<String> findAnnees() {
    	EntityManager em = Etudiant.entityManager();
    	String sql = "SELECT CAST(DATE_PART('year', date) AS INTEGER) AS year FROM tag_log GROUP BY year ORDER BY year DESC";
        return em.createNativeQuery(sql).getResultList();
    }
    
    public static TypedQuery<TagLog> findTagLogs(Integer annee, String salleFilter, String dateFilter, String searchString, Integer page, Integer size, String sortFieldName, String sortOrder) {
    
    	EntityManager em = TagLog.entityManager();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<TagLog> query = criteriaBuilder.createQuery(TagLog.class);
        Root<TagLog> taglogRoot = query.from(TagLog.class);
        List<Predicate> predicates = new ArrayList<Predicate>();
        List<Order> orders = new ArrayList<Order>();
        

        if(salleFilter != null && salleFilter != ""){
        	Join<TagLog, Salle> taglogSalleJoin = taglogRoot.join("salle");
        	predicates.add(criteriaBuilder.equal(taglogSalleJoin.get("nom"), salleFilter));
        }else{
        	salleFilter="";
        }
        
        if(sortOrder.equals("asc")){
        	orders.add(criteriaBuilder.asc(taglogRoot.get(sortFieldName)));
        } else {
        	orders.add(criteriaBuilder.desc(taglogRoot.get(sortFieldName)));
        }
        
        if(dateFilter != null && dateFilter != ""){
			try {
	    		DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE);
				Date dateBegin = format.parse(dateFilter);
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(dateBegin);
				calendar.add(Calendar.DATE, 1);
				Date dateEnd = calendar.getTime();
				predicates.add(criteriaBuilder.between(taglogRoot.<Date>get("date"), dateBegin, dateEnd));
			} catch (ParseException e) {
				log.warn("format de date incorrect : " + dateFilter, e);
				dateFilter = "erreur";
			}
    	} else {
    		dateFilter = "";
		 	Date now = new Date();
	        if(annee==null) {
	        	SimpleDateFormat anneeDf = new SimpleDateFormat("yyyy");
	        	annee = Integer.valueOf(anneeDf.format(now));
	        	if(now.getMonth() >= 0 && now.getMonth() <8) annee--;
	        }
	        DateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
			try {
				Date dateBegin = format.parse("01/09/"+annee);
				Date dateEnd = format.parse("31/08/"+(annee+1));
		        predicates.add(criteriaBuilder.between(taglogRoot.<Date>get("date"), dateBegin, dateEnd));
			} catch (ParseException e) {
				log.error("Problème date de debut ou de fin", e);
			}
    	}
    	

        if(searchString != null && searchString != ""){
	        Expression<Boolean> fullTestSearchExpression = criteriaBuilder.function("fts", Boolean.class, criteriaBuilder.literal(searchString));
	        Expression<Double> fullTestSearchRanking = criteriaBuilder.function("ts_rank", Double.class, criteriaBuilder.literal(searchString));
	        predicates.add(criteriaBuilder.isTrue(fullTestSearchExpression));
	        orders.add(criteriaBuilder.desc(fullTestSearchRanking));
        }else{
        	searchString = "";
        }
        
        query.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
        query.orderBy(orders);
        query.select(taglogRoot);

        int sizeNo = size == null ? 10 : size.intValue();
        final int firstResult = page == null ? 0 : (page.intValue() - 1) * sizeNo;
        
        return em.createQuery(query).setFirstResult(firstResult).setMaxResults(sizeNo);
    }
    
    public static long countFindTagLogs(Integer annee, String salleFilter, String dateFilter, String searchString) {
        
    	EntityManager em = TagLog.entityManager();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<TagLog> taglogRoot = query.from(TagLog.class);
        List<Predicate> predicates = new ArrayList<Predicate>();

        if(salleFilter != null && salleFilter != ""){
        	Join<TagLog, Salle> taglogSalleJoin = taglogRoot.join("salle");
        	predicates.add(criteriaBuilder.equal(taglogSalleJoin.get("nom"), salleFilter));
        }else{
        	salleFilter="";
        }
        
        if(dateFilter != null && dateFilter != ""){
			try {
	    		DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE);
				Date dateBegin = format.parse(dateFilter);
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(dateBegin);
				calendar.add(Calendar.DATE, 1);
				Date dateEnd = calendar.getTime();
				predicates.add(criteriaBuilder.between(taglogRoot.<Date>get("date"), dateBegin, dateEnd));
			} catch (ParseException e) {
				log.warn("format de date incorrect : " + dateFilter, e);
				dateFilter = "erreur";
			}
    	} else {
    		dateFilter = "";
		 	Date now = new Date();
	        if(annee==null) {
	        	SimpleDateFormat anneeDf = new SimpleDateFormat("yyyy");
	        	annee = Integer.valueOf(anneeDf.format(now));
	        	if(now.getMonth() >= 0 && now.getMonth() <8) annee--;
	        }
	        DateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
			try {
				Date dateBegin = format.parse("01/09/"+annee);
				Date dateEnd = format.parse("31/08/"+(annee+1));
		        predicates.add(criteriaBuilder.between(taglogRoot.<Date>get("date"), dateBegin, dateEnd));
			} catch (ParseException e) {
				log.error("Problème date de debut ou de fin", e);
			}
    	}
    	

        if(searchString != null && searchString != ""){
	        Expression<Boolean> fullTestSearchExpression = criteriaBuilder.function("fts", Boolean.class, criteriaBuilder.literal(searchString));
	        predicates.add(criteriaBuilder.isTrue(fullTestSearchExpression));
        }else{
        	searchString = "";
        }
        
        query.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
        query.select(criteriaBuilder.count(taglogRoot));
        
        return em.createQuery(query).getSingleResult();
    }
    
}
