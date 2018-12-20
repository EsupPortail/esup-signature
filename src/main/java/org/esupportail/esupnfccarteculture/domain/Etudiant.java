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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
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
@RooJpaActiveRecord(finders = { 
		"findEtudiantsByEppnEquals",
		"findEtudiantsByEppnLike",
		"findEtudiantsByCsnEquals", 
		"findEtudiantsByEtablissementEquals", 
		"findEtudiantsByNomEquals",
		"findEtudiantsByNomLike"
		})
public class Etudiant {
	
	protected final static Logger log = LoggerFactory.getLogger(Etudiant.class);
	
    @Column(unique = true)
    private String eppn;

    private String csn;
    
    private String civilite;

    private String nom;

    private String prenom;

    private String email;
    
    // map coupons : key = nom du type de salle, value = nombre de coupons
    @ElementCollection(fetch = FetchType.EAGER)
    private Map<String, Integer> coupons = new HashMap<String, Integer>();

    private int nbRecharge;

    private String filiere;    
    
    private String niveauEtudes;

    private String affiliation;

    private String etablissement;

    @DateTimeFormat(style = "MM")
    private Date dateNaissance;
    
    @DateTimeFormat(style = "MM")
    private Date dateRecharge;

    @DateTimeFormat(style = "MM")
    private Date dateInscription;
    
    public int getCouponsSum() {
    	if(coupons != null) {
    		return coupons.values().stream().mapToInt(i -> i).sum();
    	} else {
    		return 0;
    	}
    }

    public String getCouponsLibelleLn() {
		StringBuilder sb = new StringBuilder();
		Iterator<Entry<String, Integer>> iter = coupons.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, Integer> entry = iter.next();
			sb.append("coupon " + entry.getKey());
			sb.append(" : ");
			sb.append(entry.getValue());
			if (iter.hasNext()) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	public String getCouponsLibelle() {
		StringBuilder sb = new StringBuilder();
		Iterator<Entry<String, Integer>> iter = coupons.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, Integer> entry = iter.next();
			sb.append("coupon " + entry.getKey());
			sb.append(" : ");
			sb.append(entry.getValue());
			if (iter.hasNext()) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}
    
    public static TypedQuery<Etudiant> findEtablissements(Integer annee, String dateFilter, String searchString) {
    	
    	EntityManager em = Etudiant.entityManager();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Etudiant> query = criteriaBuilder.createQuery(Etudiant.class);
        Root<Etudiant> etudiantRoot = query.from(Etudiant.class);
        final List<Predicate> predicates = new ArrayList<Predicate>();
        
        if(dateFilter != null && dateFilter != ""){
			try {
				DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE);
				Date dateBeginFilter = format.parse(dateFilter);
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(dateBeginFilter);
				calendar.add(Calendar.DATE, 1);
				Date dateEndFilter = calendar.getTime();
				predicates.add(criteriaBuilder.between(etudiantRoot.<Date>get("dateInscription"), dateBeginFilter, dateEndFilter));
			} catch (ParseException e) {
				log.warn("format de date incorrect : " + dateFilter, e);
			}
    	}else{
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
		        predicates.add(criteriaBuilder.between(etudiantRoot.<Date>get("dateInscription"), dateBegin, dateEnd));
			} catch (ParseException e) {
				log.error("Problème date de debut ou de fin", e);
			}
    	}
        
        if(searchString != null && searchString!=""){
	        Expression<Boolean> fullTestSearchExpression = criteriaBuilder.function("fts", Boolean.class, criteriaBuilder.literal(searchString));
	        predicates.add(criteriaBuilder.isTrue(fullTestSearchExpression));
        }else{
        	searchString="";
        }
    	
        query.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
        query.select(etudiantRoot.get("etablissement")).groupBy(etudiantRoot.get("etablissement"));
        return em.createQuery(query);
    }
    
    public static List<String> findAnnees() {
    	EntityManager em = Etudiant.entityManager();
    	String sql = "SELECT CAST(DATE_PART('year', date_inscription) AS INTEGER) AS year FROM etudiant GROUP BY year ORDER BY year DESC";
        return em.createNativeQuery(sql).getResultList();
    }
    
    public static TypedQuery<Etudiant> findEtudiants(Integer annee, String dateFilter, String etablissementFilter, String searchString, Integer page, Integer size, String sortFieldName, String sortOrder) {
    	int sizeNo = size == null ? 10 : size.intValue();
        final int firstResult = page == null ? 0 : (page.intValue() - 1) * sizeNo;
       
    	if(sortFieldName==null){
    		sortFieldName = "dateRecharge";
    		sortOrder = "desc";
    	}
   	
    	EntityManager em = Etudiant.entityManager();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Etudiant> query = criteriaBuilder.createQuery(Etudiant.class);
        Root<Etudiant> etudiantRoot = query.from(Etudiant.class);
        List<Predicate> predicates = new ArrayList<Predicate>();
        List<Order> orders = new ArrayList<Order>();
        
        if(dateFilter != null && dateFilter != ""){
			try {
				DateFormat formatFilter = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
				Date dateBeginFilter = formatFilter.parse(dateFilter);
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(dateBeginFilter);
				calendar.add(Calendar.DATE, 1);
				Date dateEndFilter = calendar.getTime();
				predicates.add(criteriaBuilder.between(etudiantRoot.<Date>get("dateInscription"), dateBeginFilter, dateEndFilter));
			} catch (ParseException e) {
				log.warn("format de date incorrect : " + dateFilter, e);
			}
    	}else{
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
		        predicates.add(criteriaBuilder.between(etudiantRoot.<Date>get("dateInscription"), dateBegin, dateEnd));
			} catch (ParseException e) {
				log.error("Problème date de debut ou de fin", e);
			}
    	}
        
        if(searchString != null && searchString!=""){
	        Expression<Boolean> fullTestSearchExpression = criteriaBuilder.function("fts", Boolean.class, criteriaBuilder.literal(searchString));
	        Expression<Double> fullTestSearchRanking = criteriaBuilder.function("ts_rank", Double.class, criteriaBuilder.literal(searchString));
	        predicates.add(criteriaBuilder.isTrue(fullTestSearchExpression));
	        if(sortOrder.equals("asc")) {
	        	orders.add(criteriaBuilder.asc(fullTestSearchRanking));
	        } else {
	        	orders.add(criteriaBuilder.desc(fullTestSearchRanking));
	        }
        }else{
        	searchString="";
        }
        
        if(etablissementFilter != null && etablissementFilter != ""){
        	predicates.add(criteriaBuilder.equal(etudiantRoot.<String>get("etablissement"), etablissementFilter));
        }else {
        	etablissementFilter = "";
        }
        if(sortOrder.equals("asc")) {
        	orders.add(criteriaBuilder.asc(etudiantRoot.get(sortFieldName)));
        } else {
        	orders.add(criteriaBuilder.desc(etudiantRoot.get(sortFieldName)));
        }
    	
        query.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
        query.orderBy(orders);
        query.select(etudiantRoot);
        return em.createQuery(query).setFirstResult(firstResult).setMaxResults(sizeNo);
    }
    
    public static long countFindEtudiants(Integer annee, String dateFilter, String etablissementFilter, String searchString, Integer page, Integer size) {

       	EntityManager em = Etudiant.entityManager();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<Etudiant> etudiantRoot = query.from(Etudiant.class);
        final List<Predicate> predicates = new ArrayList<Predicate>();
        
        if(dateFilter != null && dateFilter != ""){
			try {
				DateFormat formatFilter = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
				Date dateBeginFilter = formatFilter.parse(dateFilter);
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(dateBeginFilter);
				calendar.add(Calendar.DATE, 1);
				Date dateEndFilter = calendar.getTime();
				predicates.add(criteriaBuilder.between(etudiantRoot.<Date>get("dateRecharge"), dateBeginFilter, dateEndFilter));
			} catch (ParseException e) {
				log.warn("format de date incorrect : " + dateFilter, e);
			}
    	}else {
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
		        predicates.add(criteriaBuilder.between(etudiantRoot.<Date>get("dateInscription"), dateBegin, dateEnd));
			} catch (ParseException e) {
				log.error("Problème date de debut ou de fin", e);
			}
    	}
        
        if(searchString != null && searchString!=""){
	        Expression<Boolean> fullTestSearchExpression = criteriaBuilder.function("fts", Boolean.class, criteriaBuilder.literal(searchString));
	        predicates.add(criteriaBuilder.isTrue(fullTestSearchExpression));
        }else{
        	searchString="";
        }
        
        if(etablissementFilter != null && etablissementFilter != ""){
        	predicates.add(criteriaBuilder.equal(etudiantRoot.<String>get("etablissement"), etablissementFilter));
        }else {
        	etablissementFilter = "";
        }
    	
        query.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
        query.select(criteriaBuilder.count(etudiantRoot));
        return em.createQuery(query).getSingleResult();
    }
    
}
