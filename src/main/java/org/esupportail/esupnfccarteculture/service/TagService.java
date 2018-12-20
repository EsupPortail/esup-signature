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
package org.esupportail.esupnfccarteculture.service;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.esupportail.esupnfccarteculture.domain.Etudiant;
import org.esupportail.esupnfccarteculture.domain.Salle;
import org.esupportail.esupnfccarteculture.domain.TagLog;
import org.esupportail.esupnfccarteculture.domain.TypeSalle;
import org.esupportail.esupnfccarteculture.domain.TypeSalleInscription;
import org.esupportail.esupnfccarteculture.domain.TypeSalleJoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

@Service
public class TagService {

	private final Logger log = LoggerFactory.getLogger(TagService.class);

	private int nbRechargeMax = 3;

	private List<TypeSalle> typeSalles;
	
	private List<TypeSalle> typeSallesBases = Arrays.asList(new TypeSalle[]{TypeSalleInscription.getTypeSalleInscriptionSingleton()});

	public List<TypeSalle> getTypeSallesBadgeables() {
		return typeSalles.stream().filter(typeSalle -> typeSalle.isTypeSalleBadgeable()).collect(Collectors.toList());
	}
		
	public List<TypeSalle> getTypeSallesDebitables() {
		return typeSalles.stream().filter(typeSalle -> typeSalle.isTypeSalleDebitable()).collect(Collectors.toList());
	}
	
	public TypeSalle getTypeSalle(String nom) {
		return typeSalles.stream().filter(typeSalle -> typeSalle.getNom().equals(nom)).collect(Collectors.toList()).get(0);
	}

	public void setTypeSalles(List<TypeSalle> typeSalles) {		
		this.typeSalles = typeSalles;
		this.typeSalles.addAll(typeSallesBases);
	}

	public int getNbRechargeMax() {
		return nbRechargeMax;
	}

	public void setNbRechargeMax(int nbRechargeMax) {
		this.nbRechargeMax = nbRechargeMax;
	}
	
	public String checkEtudiant(String eppn, String nomSalle) {
		if(Etudiant.countFindEtudiantsByEppnEquals(eppn) > 0) {
			return checkEtudiant(Etudiant.findEtudiantsByEppnEquals(eppn).getSingleResult(), nomSalle);
		}
		return null;
	}
	
	public String checkEtudiant(Etudiant etudiant, String nomSalle) {
		try {
			Salle salle = Salle.findSallesByNomEquals(nomSalle).getSingleResult();
			if (!salle.getTypeSalle().equals(TypeSalleInscription.getTypeSalleInscriptionSingleton().toString())) {
				TypeSalle typeSalle = getTypeSalle(salle.getTypeSalle());
				if(etudiant.getCoupons().get(typeSalle.getNom()) != null && (etudiant.getCoupons().get(typeSalle.getNom()) > 0 || etudiant.getCoupons().get(TypeSalleJoker.JOKER_NAME) > 0)) {
					log.info("Etudiant " + etudiant.getEppn() + " check OK for : " + salle.getNom());
				} else {
					log.info("L'etudiant " + etudiant.getEppn() + " n'a plus de coupon " + typeSalle.getNom() + " lors du check");
					return etudiant.getPrenom() + " " + etudiant.getNom() + ":\nPlus de coupon " + typeSalle.getNom();
				}
			}
		} catch (EmptyResultDataAccessException e) {
			log.warn("Etudiant " + etudiant.getEppn() + " non trouvé lors du check", e);
			return "Pas de carte culture :\ncarte inactive";
		}
		return null;
	}

	public boolean debitCoupon(Etudiant etudiant, String nomSalle, String eppnInit) {
		boolean debitOk = false;
		Salle salle = null;
		try {
			salle = Salle.findSallesByNomEquals(nomSalle).getSingleResult();
			TypeSalle typeSalle = getTypeSalle(salle.getTypeSalle());
			if(etudiant.getCoupons().get(typeSalle.getNom()) > 0) {
				etudiant.getCoupons().replace(typeSalle.getNom(), etudiant.getCoupons().get(typeSalle.getNom()) - 1);
				log.info("Etudiant " + etudiant.getEppn() + " debit OK for : " + salle.getNom() + " du type " + typeSalle.getNom());
				debitOk = true;
			} else if(etudiant.getCoupons().get(TypeSalleJoker.JOKER_NAME) > 0) {
				etudiant.getCoupons().put(TypeSalleJoker.JOKER_NAME, etudiant.getCoupons().get(TypeSalleJoker.JOKER_NAME) - 1);
				log.info("Etudiant " + etudiant.getEppn() + " debit OK for : " + salle.getNom() + " with joker");
				debitOk = true;
			} else {
				log.info("L'etudiant " + etudiant.getEppn() + " n'a plus de coupon lors du debit");
			}
			if (debitOk) {
				createNewTagLog(etudiant, salle, eppnInit);
			} 
		} catch (EmptyResultDataAccessException e) {
			log.warn("Salle " + salle + " non trouvé lors du debit", e);
		}
		
		return debitOk;
	}

	public boolean checkRecharge(Etudiant etudiant) {
		if(etudiant.getCouponsSum() == 0 && etudiant.getNbRecharge() < nbRechargeMax){
			return true;
		} else {
			log.info("Recharge impossible pour "+etudiant.getEppn());
			return false;
		}
	}

	public boolean recharge(Etudiant etudiant) {
		if(checkRecharge(etudiant)){
			log.info("Recharge de " + etudiant.getEppn());
	        Map<String, Integer> coupons = new HashMap<String, Integer>();
	        for(TypeSalle typeSalle : getTypeSallesDebitables()) {
	        	coupons.put(typeSalle.getNom(), typeSalle.getMaxCoupon());
	        }
	        etudiant.setCoupons(coupons);
	        etudiant.setNbRecharge(etudiant.getNbRecharge() + 1);
	        etudiant.setDateRecharge(new Date());
	        return true;
		} else {
			return false;
		}
	}

	public void createNewTagLog(Etudiant etudiant, Salle salle, String eppnInit) {
		TagLog tagLog = new TagLog();
		tagLog.setEtudiant(etudiant);
		tagLog.setDate(new Date());
		tagLog.setSalle(salle);
		tagLog.setTarif(salle.getTarif());
		tagLog.setEppnInit(eppnInit);
		tagLog.persist();
	}

	
}
