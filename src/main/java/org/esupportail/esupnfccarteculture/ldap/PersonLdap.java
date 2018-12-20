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

import java.util.List;

import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.json.RooJson;
import org.springframework.roo.addon.tostring.RooToString;

@RooJavaBean
@RooToString
@RooJson
public class PersonLdap {
	
	private String uid;
	private String cn ;
	private String sn; 
	private String givenName ;
	private String displayName ;
	private String schacDateOfBirth;
	private String mail ;
	private String md5UserPassword ;
	private String cryptUserPassword ;
	private String shaUserPassword ;
	private List<String> eduPersonAffiliation ;
	private String eduPersonPrimaryAffiliation ;
	private String eduPersonPrincipalName ;
	private String mailDrop ;
	private String mailHost ;
	private String sambaSID ;
	private String sambaPrimaryGroupSID ;
	private String sambaPwdLastSet ;
	private String sambaLMPassword ;
	private String sambaNTPassword ;
	private String sambaAcctFlags ;
	private String homeDirectory ;
	private String uidNumber ;
	private String gidNumber ;
	private String postalAddress ;
	private String facsimileTelephoneNumber ;
	private String telephoneNumber ;
	private String supannCivilite ;
	private String supannListeRouge ;
	private String supannEtablissement ;
	private String supannEntiteAffectation ;
	private String supannEntiteAffectationPrincipale ;
	private String supannEmpId ;
	private String supannEmpCorps ;
	private String supannActivite ;
	private String supannAutreTelephone ;
	private String supannCodeINE ;
	private String supannEtuId ;
	private String supannEtuEtape ;
	private String supannEtuAnneeInscription ;
	private String supannEtuSecteurDisciplinaire ;
	private String supannEtuDiplome ;
	private String supannEtuTypeDiplome ;
	private List<String> supannEtuCursusAnnee ;
	private String supannParrainDN ;
	private String supannMailPerso ;
	private String supannAliasLogin ;
	private List<String> supannRefId;
	private String supannRoleGenerique ;
	private String supannAutreMail ;
	private Long mailuserquota ;

}
	
	
