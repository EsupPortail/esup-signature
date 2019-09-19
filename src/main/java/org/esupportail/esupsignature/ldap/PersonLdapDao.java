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
package org.esupportail.esupsignature.ldap;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.LikeFilter;

import javax.annotation.Resource;

public class PersonLdapDao {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Resource
	private LdapTemplate ldapTemplate;

	public List<PersonLdap> getPersonNamesByUid(String uid) {
		AndFilter filter = new AndFilter();
		filter.and(new EqualsFilter("objectclass", "person"));
		filter.and(new LikeFilter("uid", uid));
		return ldapTemplate.search("", filter.encode(), new PersonAttributMapper());
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
		return ldapTemplate.search("", filter.encode(), new PersonAttributMapper());
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
