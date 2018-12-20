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

import java.util.ArrayList;
import java.util.List;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.springframework.ldap.core.AttributesMapper;

public class PersonAttributMapper implements AttributesMapper {
	
	public PersonLdap mapFromAttributes(Attributes attrs)
			throws javax.naming.NamingException {
		PersonLdap p = new PersonLdap();
		Attribute attrGivenName = attrs.get("givenName");
		if(attrGivenName != null)
			p.setGivenName(attrGivenName.get().toString());
		p.setSn(attrs.get("sn").get().toString());
		p.setUid(attrs.get("uid").get().toString());
		p.setCn(attrs.get("cn").get().toString());
		if (null!=attrs.get("mail"))
			p.setMail(attrs.get("mail").get().toString());
		if (null!=attrs.get("telephoneNumber"))
			p.setTelephoneNumber(attrs.get("telephoneNumber").get().toString());
		if (null!=attrs.get("supannAutreMail"))
			p.setSupannAutreMail(attrs.get("supannAutreMail").get().toString());	
		if (null!=attrs.get("supannRefId")){
			Attribute supannRefIds=attrs.get("supannRefId");
			List<String> ids = new ArrayList<String>();
			for(int i = 0; i< supannRefIds.size(); i++){
				ids.add(supannRefIds.get(i).toString());
			}
			p.setSupannRefId(ids);
		}
		if (null!=attrs.get("supannEtuCursusAnnee")){
			Attribute supannRefIds=attrs.get("supannEtuCursusAnnee");
			List<String> ids = new ArrayList<String>();
			for(int i = 0; i< supannRefIds.size(); i++){
				ids.add(supannRefIds.get(i).toString());
			}
			p.setSupannEtuCursusAnnee(ids);
		}
		if (null!=attrs.get("supannEtuEtape"))
			p.setSupannAutreMail(attrs.get("supannEtuEtape").get().toString());
		if (null!=attrs.get("supannEntiteAffectation"))
			p.setSupannAutreMail(attrs.get("supannEntiteAffectation").get().toString());		
		if (null!=attrs.get("eduPersonPrincipalName"))
			p.setEduPersonPrincipalName(attrs.get("eduPersonPrincipalName").get().toString());
		if (null!=attrs.get("supannEntiteAffectation"))
			p.setSupannEntiteAffectation(attrs.get("supannEntiteAffectation").get().toString());
		if (null!=attrs.get("supannEntiteAffectationPrincipale"))
			p.setSupannEntiteAffectationPrincipale(attrs.get("supannEntiteAffectationPrincipale").get().toString());
		if (null!=attrs.get("supannCivilite"))
			p.setSupannCivilite(attrs.get("supannCivilite").get().toString());
		if (null!=attrs.get("schacDateOfBirth"))
			p.setSchacDateOfBirth(attrs.get("schacDateOfBirth").get().toString());
		if (null!=attrs.get("eduPersonAffiliation")){
			Attribute eduPersonAffiliations=attrs.get("eduPersonAffiliation");
			List<String> affils = new ArrayList<String>();
			for(int i = 0; i< eduPersonAffiliations.size(); i++){
				affils.add(eduPersonAffiliations.get(i).toString());
			}
			p.setEduPersonAffiliation(affils);
		}
		return p;
	}
	
	
}
