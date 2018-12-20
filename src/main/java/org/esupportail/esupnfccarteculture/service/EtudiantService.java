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

import static org.springframework.ldap.query.LdapQueryBuilder.query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.esupnfccarteculture.domain.Etudiant;
import org.esupportail.esupnfccarteculture.ldap.PersonLdap;
import org.esupportail.esupnfccarteculture.ldap.PersonLdapDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.stereotype.Service;

@Service
public class EtudiantService {

	@Autowired
	List<PersonLdapDao> personDaos;
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	private String autorizedStudentLdapFilter = "";
	private String eppnFilterRegex = "";
	private boolean affichageDetailCoupons;
	private boolean preInscription;
	private String preInscriptionNomSalle = "pre-inscription";
	private String ldapCsnSearchAttribut;
	private String ldapCsnMultiValueTagExtractRegex;
	private String ldapCsnMultiValueTag;
	
	public String getPreInscriptionNomSalle() {
		return preInscriptionNomSalle;
	}

	public void setPreInscriptionNomSalle(String preInscriptionNomSalle) {
		this.preInscriptionNomSalle = preInscriptionNomSalle;
	}

	public String getEppnFilterRegex() {
		return eppnFilterRegex;
	}

	public void setEppnFilterRegex(String eppnFilter) {
		this.eppnFilterRegex = eppnFilter;
	}

	public String getAutorizedStudentLdapFilter() {
		return autorizedStudentLdapFilter;
	}

	public void setAutorizedStudentLdapFilter(String autorizedStudentFilter) {
		this.autorizedStudentLdapFilter = autorizedStudentFilter;
	}

	public boolean isAffichageDetailCoupons() {
		return affichageDetailCoupons;
	}

	public void setAffichageDetailCoupons(boolean affichageDetailCoupons) {
		this.affichageDetailCoupons = affichageDetailCoupons;
	}

	public String getLdapCsnMultiValueTag() {
		return ldapCsnMultiValueTag;
	}

	public void setLdapCsnMultiValueTag(String ldapCsnMultiValueTag) {
		this.ldapCsnMultiValueTag = ldapCsnMultiValueTag;
	}

	public String getLdapCsnSearchAttribut() {
		return ldapCsnSearchAttribut;
	}

	public void setLdapCsnSearchAttribut(String ldapCsnSearchAttribut) {
		this.ldapCsnSearchAttribut = ldapCsnSearchAttribut;
	}

	public String getLdapCsnMultiValueTagExtractRegex() {
		return ldapCsnMultiValueTagExtractRegex;
	}

	public void setLdapCsnMultiValueTagExtractRegex(String ldapCsnMultiValueTagExtractRegex) {
		this.ldapCsnMultiValueTagExtractRegex = ldapCsnMultiValueTagExtractRegex;
	}

	public boolean isPreInscription() {
		return preInscription;
	}

	public void setPreInscription(boolean preInscription) {
		this.preInscription = preInscription;
	}

	public void updateEtudiant(Etudiant etudiant) throws Exception {
		PersonLdap person = getPersonFromEppn(etudiant.getEppn());
		if(person != null) {
			etudiant.setNom(person.getSn());
			etudiant.setPrenom(person.getGivenName());
			etudiant.setEmail(person.getMail());
			etudiant.setEtablissement(etudiant.getEppn().split("@")[1]);
			etudiant.setAffiliation(person.getEduPersonAffiliation().get(0));
			etudiant.setFiliere(person.getSupannEntiteAffectationPrincipale());
			etudiant.setCivilite(person.getSupannCivilite());
			try {
				etudiant.setDateNaissance(new SimpleDateFormat("yyyyMMdd").parse(person.getSchacDateOfBirth()));
			} catch (ParseException e) {
				log.warn("parse error dateNaissance pour : " + etudiant.getEppn());
			}
			String newCsn = searchLdapCsnByEppn(etudiant.getEppn());
			if(newCsn != null && !newCsn.equals(etudiant.getCsn())) {
				etudiant.setCsn(newCsn);
				log.info("Le CSN de l'etudiant " + etudiant.getEppn() + " a été mis à jour");
			}
			if(person.getSupannEtuCursusAnnee() != null){
				etudiant.setNiveauEtudes(person.getSupannEtuCursusAnnee().get(0).substring(person.getSupannEtuCursusAnnee().get(0).lastIndexOf("}") + 1));
			}
			log.info("etudiant " + etudiant.getEppn() + " mis à jour");
		} else {
			log.warn("Etudiant non trouvé dans le LDAP " + etudiant.getEppn());
		}
	}

	public PersonLdap getPersonFromEppn(String eppn) {
		if(eppn.matches(eppnFilterRegex)) {
			for (PersonLdapDao personLdapDao : personDaos) {
				LdapContextSource contextSource = (LdapContextSource) personLdapDao.getLdapTemplate().getContextSource();
				List<PersonLdap> persons = personLdapDao.getPersonNamesByEppn(eppn, autorizedStudentLdapFilter);
				if (persons.size() > 0 && persons.get(0) != null && persons.get(0).getEduPersonPrincipalName().matches(eppnFilterRegex)) {
					log.info(eppn + " trouvé dans ldap : " + contextSource.getUrls()[0]);
					return  persons.get(0);
				} else {
					log.warn(eppn + " non trouvé dans ldap : " + contextSource.getUrls()[0]);
				}
			}
		}
		return null;
	}
	
	public PersonLdap getPersonFromCsn(String csn) {
		for (PersonLdapDao personLdapDao : personDaos) {
			LdapContextSource contextSource = (LdapContextSource) personLdapDao.getLdapTemplate().getContextSource();
			List<PersonLdap> persons = personLdapDao.getPersonLdaps(getLdapCsnSearchAttribut(), getLdapCsnMultiValueTag() + csn.toUpperCase());
			if (persons.size() > 0 && persons.get(0) != null && persons.get(0).getEduPersonPrincipalName().matches(eppnFilterRegex)) {
				log.info(csn + " trouvé dans ldap" + contextSource.getUrls()[0]);
				return  persons.get(0);
			} else {
				log.warn(csn + " non trouvé dans ldap" + contextSource.getUrls()[0]);
			}
			
		}
		return null;
	}
	
	public String searchLdapCsnByEppn(String eppn) {
		if(eppn.matches(eppnFilterRegex)) {
			try {
				String[] attributes = {ldapCsnSearchAttribut};
				Map<String,String> resultMap = getUserInfos(eppn, attributes);
				if(resultMap.get(ldapCsnSearchAttribut) != null && resultMap.get(ldapCsnSearchAttribut) != "") {
					if(ldapCsnMultiValueTagExtractRegex != null && ldapCsnMultiValueTagExtractRegex != "") {
						if (resultMap.get(ldapCsnSearchAttribut).matches(ldapCsnMultiValueTagExtractRegex)) {
							return resultMap.get(ldapCsnSearchAttribut).replaceFirst(ldapCsnMultiValueTagExtractRegex, "$1");
						}
					}else{
						return resultMap.get(ldapCsnSearchAttribut);
					}
				}
			} catch (Exception e) {
				log.error("erreur lors de la recherche LDAP " + eppn, e);
			}
		}
		return null;
	}

	public String affichageCoupons(Etudiant etudiant) {
		if(affichageDetailCoupons) {
			return etudiant.getCouponsLibelle();
		} else {
			return String.valueOf(etudiant.getCouponsSum());
		}
	}

	public Map<String,String> getUserInfos(String eppn, String[] attributesToReturn) {	
		Map<String, String> userInfos = new HashMap<String, String>();
	
		for(PersonLdapDao personDao : personDaos) {
			
			LdapTemplate ldapTemplate = personDao.getLdapTemplate();
			
			List<Map<String, String>>  userInfosList = ldapTemplate.search(query().attributes(attributesToReturn).where("eduPersonPrincipalName").is(eppn),
					new AttributesMapper<Map<String, String>>() {
	
				@Override
				public Map<String, String> mapFromAttributes(Attributes attributes) throws NamingException {
					Map<String, String> userInfos = new HashMap<String, String>();
					for(String name: attributesToReturn) {
						Attribute attr = attributes.get(name);
						if(attr != null && attr.get() instanceof java.lang.String) {
							List<String> values = new ArrayList<String>();
							NamingEnumeration<?> attrEnum = attr.getAll();
							while(attrEnum.hasMoreElements()) {
								values.add((String)attrEnum.nextElement());
							}
							userInfos.put(name, StringUtils.join(values, ";"));
						} else if(attr != null && attr.get() instanceof byte[]) {
							byte[] value = (byte[])attr.get();
							userInfos.put(name, java.util.Base64.getEncoder().encodeToString(value));
						} else {
							userInfos.put(name, "");
						}
					}
					return userInfos;
				}
			});
			
			if(!userInfosList.isEmpty()) {
				userInfos = userInfosList.get(0);
			}
		}
		return userInfos;
	}
    
}
