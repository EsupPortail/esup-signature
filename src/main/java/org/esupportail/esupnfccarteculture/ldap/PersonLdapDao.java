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
package org.esupportail.esupnfccarteculture.ldap;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

import java.util.List;

import javax.naming.Name;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.LikeFilter;

@SuppressWarnings("deprecation")
public class PersonLdapDao {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private LdapTemplate ldapTemplate;

	public void setLdapTemplate(LdapTemplate ldapTemplate) {
		this.ldapTemplate = ldapTemplate;
	}
	
	public LdapTemplate getLdapTemplate() {
		return this.ldapTemplate;
	}

	public PersonLdap findByPrimaryKey(String uid) {
		Name dn = buildDn(uid);
		try {
			PersonLdap personLdap = (PersonLdap) ldapTemplate.lookup(dn, new PersonAttributMapper());
			return personLdap;
		} catch(NameNotFoundException nnfe) {
			log.warn("Problème lors de la récupération LDAP de " + uid + " : " + nnfe.getMessage());
		}
		return null;
	}
	
	private Name buildDn(String uid) {
		DistinguishedName dn = new DistinguishedName();
		dn.add("ou", "people");
		dn.add("uid", uid);
		return dn;
	}
	
	public List<PersonLdap> getPersonNamesByEppn(String eppn) {
		AndFilter filter = new AndFilter();
		filter.and(new EqualsFilter("objectclass", "person"));
		filter.and(new LikeFilter("eduPersonPrincipalName", eppn));
		return ldapTemplate.search("", filter.encode(),
				new PersonAttributMapper());
	}
	
	public List<PersonLdap> getPersonNamesByEppn(String eppn, String moreFilter) {
		AndFilter filter = new AndFilter();
		filter.and(new EqualsFilter("objectclass", "person"));
		filter.and(new LikeFilter("eduPersonPrincipalName", eppn));
		filter.and(query().filter(moreFilter).filter());
		return ldapTemplate.search("", filter.encode(),
				new PersonAttributMapper());
	}
	
	public List<PersonLdap> getPersonLdaps(String attribut, String value) {
		AndFilter filter = new AndFilter();
		filter.and(new EqualsFilter("objectclass", "person"));
		filter.and(new LikeFilter(attribut, value));
		log.info(filter.encode() + ", " + filter.toString());
		return ldapTemplate.search("", filter.encode(),
				new PersonAttributMapper());
	} 

	public List<PersonLdap> getPersonLdaps(String attribut, String value, String moreFilter) {
		AndFilter filter = new AndFilter();
		filter.and(new EqualsFilter("objectclass", "person"));
		filter.and(new LikeFilter(attribut, value));
		log.info(filter.encode() + ", " + filter.toString());
		return ldapTemplate.search("", filter.encode(),
				new PersonAttributMapper());
	} 
	
}
