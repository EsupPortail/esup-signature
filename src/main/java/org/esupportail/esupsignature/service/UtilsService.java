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
package org.esupportail.esupsignature.service;

import java.util.Calendar;
import java.util.List;

import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.ldap.PersonLdapDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.stereotype.Service;

@Service
public class UtilsService {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	List<PersonLdapDao> personDaos;

	
	public int getAnnee(){
		int annee = Calendar.getInstance().get(Calendar.YEAR);
		if(Calendar.getInstance().get(Calendar.MONTH) < Calendar.SEPTEMBER){
			annee--;
		}
		return annee;
	}

	public PersonLdap getPersonFromCsn(String csn) {
		for (PersonLdapDao personLdapDao : personDaos) {
			LdapContextSource contextSource = (LdapContextSource) personLdapDao.getLdapTemplate().getContextSource();
			List<PersonLdap> persons = personLdapDao.getPersonLdaps("csn", csn.toUpperCase());
			if (persons.size() > 0 && persons.get(0) != null && persons.get(0).getEduPersonPrincipalName().matches("")) {
				log.info(csn + " trouvé dans ldap" + contextSource.getUrls()[0]);
				return  persons.get(0);
			} else {
				log.warn(csn + " non trouvé dans ldap" + contextSource.getUrls()[0]);
			}
			
		}
		return null;
	}
}
