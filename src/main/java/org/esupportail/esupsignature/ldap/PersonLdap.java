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

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;

public class PersonLdap {
	
	private String uid;
	private String cn;
	private String sn; 
	private String givenName;
	private String displayName;
	private String schacDateOfBirth;
	private String mail;
	private String md5UserPassword;
	private String cryptUserPassword;
	private String shaUserPassword;
	private List<String> eduPersonAffiliation;
	private String eduPersonPrimaryAffiliation;
	private String eduPersonPrincipalName;
	private String mailDrop;
	private String mailHost;
	private String sambaSID;
	private String sambaPrimaryGroupSID;
	private String sambaPwdLastSet;
	private String sambaLMPassword;
	private String sambaNTPassword;
	private String sambaAcctFlags;
	private String homeDirectory;
	private String uidNumber;
	private String gidNumber;
	private String postalAddress;
	private String facsimileTelephoneNumber;
	private String telephoneNumber;
	private String supannCivilite;
	private String supannListeRouge;
	private String supannEtablissement;
	private String supannEntiteAffectation;
	private String supannEntiteAffectationPrincipale;
	private String supannEmpId;
	private String supannEmpCorps;
	private String supannActivite;
	private String supannAutreTelephone;
	private String supannCodeINE;
	private String supannEtuId;
	private String supannEtuEtape;
	private String supannEtuAnneeInscription;
	private String supannEtuSecteurDisciplinaire;
	private String supannEtuDiplome;
	private String supannEtuTypeDiplome;
	private List<String> supannEtuCursusAnnee;
	private String supannParrainDN;
	private String supannMailPerso;
	private String supannAliasLogin;
	private List<String> supannRefId;
	private String supannRoleGenerique;
	private String supannAutreMail;
	private Long mailuserquota;

	public String getValueByFieldName(String fieldName) {
		Field[] fields = this.getClass().getDeclaredFields();
		for(Field field : fields) {
			if(field.getName().equals(fieldName)) {
				try {
					return field.get(this).toString();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	

	public String toJson() {
        return new JSONSerializer()
        .exclude("*.class").serialize(this);
    }

	public String toJson(String[] fields) {
        return new JSONSerializer()
        .include(fields).exclude("*.class").serialize(this);
    }

	public static PersonLdap fromJsonToPersonLdap(String json) {
        return new JSONDeserializer<PersonLdap>()
        .use(null, PersonLdap.class).deserialize(json);
    }

	public static String toJsonArray(Collection<PersonLdap> collection) {
        return new JSONSerializer()
        .exclude("*.class").serialize(collection);
    }

	public static String toJsonArray(Collection<PersonLdap> collection, String[] fields) {
        return new JSONSerializer()
        .include(fields).exclude("*.class").serialize(collection);
    }

	public static Collection<PersonLdap> fromJsonArrayToPersonLdaps(String json) {
        return new JSONDeserializer<List<PersonLdap>>()
        .use("values", PersonLdap.class).deserialize(json);
    }

	public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

	public String getUid() {
        return this.uid;
    }

	public void setUid(String uid) {
        this.uid = uid;
    }

	public String getCn() {
        return this.cn;
    }

	public void setCn(String cn) {
        this.cn = cn;
    }

	public String getSn() {
        return this.sn;
    }

	public void setSn(String sn) {
        this.sn = sn;
    }

	public String getGivenName() {
        return this.givenName;
    }

	public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

	public String getDisplayName() {
        return this.displayName;
    }

	public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

	public String getSchacDateOfBirth() {
        return this.schacDateOfBirth;
    }

	public void setSchacDateOfBirth(String schacDateOfBirth) {
        this.schacDateOfBirth = schacDateOfBirth;
    }

	public String getMail() {
        return this.mail;
    }

	public void setMail(String mail) {
        this.mail = mail;
    }

	public String getMd5UserPassword() {
        return this.md5UserPassword;
    }

	public void setMd5UserPassword(String md5UserPassword) {
        this.md5UserPassword = md5UserPassword;
    }

	public String getCryptUserPassword() {
        return this.cryptUserPassword;
    }

	public void setCryptUserPassword(String cryptUserPassword) {
        this.cryptUserPassword = cryptUserPassword;
    }

	public String getShaUserPassword() {
        return this.shaUserPassword;
    }

	public void setShaUserPassword(String shaUserPassword) {
        this.shaUserPassword = shaUserPassword;
    }

	public List<String> getEduPersonAffiliation() {
        return this.eduPersonAffiliation;
    }

	public void setEduPersonAffiliation(List<String> eduPersonAffiliation) {
        this.eduPersonAffiliation = eduPersonAffiliation;
    }

	public String getEduPersonPrimaryAffiliation() {
        return this.eduPersonPrimaryAffiliation;
    }

	public void setEduPersonPrimaryAffiliation(String eduPersonPrimaryAffiliation) {
        this.eduPersonPrimaryAffiliation = eduPersonPrimaryAffiliation;
    }

	public String getEduPersonPrincipalName() {
        return this.eduPersonPrincipalName;
    }

	public void setEduPersonPrincipalName(String eduPersonPrincipalName) {
        this.eduPersonPrincipalName = eduPersonPrincipalName;
    }

	public String getMailDrop() {
        return this.mailDrop;
    }

	public void setMailDrop(String mailDrop) {
        this.mailDrop = mailDrop;
    }

	public String getMailHost() {
        return this.mailHost;
    }

	public void setMailHost(String mailHost) {
        this.mailHost = mailHost;
    }

	public String getSambaSID() {
        return this.sambaSID;
    }

	public void setSambaSID(String sambaSID) {
        this.sambaSID = sambaSID;
    }

	public String getSambaPrimaryGroupSID() {
        return this.sambaPrimaryGroupSID;
    }

	public void setSambaPrimaryGroupSID(String sambaPrimaryGroupSID) {
        this.sambaPrimaryGroupSID = sambaPrimaryGroupSID;
    }

	public String getSambaPwdLastSet() {
        return this.sambaPwdLastSet;
    }

	public void setSambaPwdLastSet(String sambaPwdLastSet) {
        this.sambaPwdLastSet = sambaPwdLastSet;
    }

	public String getSambaLMPassword() {
        return this.sambaLMPassword;
    }

	public void setSambaLMPassword(String sambaLMPassword) {
        this.sambaLMPassword = sambaLMPassword;
    }

	public String getSambaNTPassword() {
        return this.sambaNTPassword;
    }

	public void setSambaNTPassword(String sambaNTPassword) {
        this.sambaNTPassword = sambaNTPassword;
    }

	public String getSambaAcctFlags() {
        return this.sambaAcctFlags;
    }

	public void setSambaAcctFlags(String sambaAcctFlags) {
        this.sambaAcctFlags = sambaAcctFlags;
    }

	public String getHomeDirectory() {
        return this.homeDirectory;
    }

	public void setHomeDirectory(String homeDirectory) {
        this.homeDirectory = homeDirectory;
    }

	public String getUidNumber() {
        return this.uidNumber;
    }

	public void setUidNumber(String uidNumber) {
        this.uidNumber = uidNumber;
    }

	public String getGidNumber() {
        return this.gidNumber;
    }

	public void setGidNumber(String gidNumber) {
        this.gidNumber = gidNumber;
    }

	public String getPostalAddress() {
        return this.postalAddress;
    }

	public void setPostalAddress(String postalAddress) {
        this.postalAddress = postalAddress;
    }

	public String getFacsimileTelephoneNumber() {
        return this.facsimileTelephoneNumber;
    }

	public void setFacsimileTelephoneNumber(String facsimileTelephoneNumber) {
        this.facsimileTelephoneNumber = facsimileTelephoneNumber;
    }

	public String getTelephoneNumber() {
        return this.telephoneNumber;
    }

	public void setTelephoneNumber(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }

	public String getSupannCivilite() {
        return this.supannCivilite;
    }

	public void setSupannCivilite(String supannCivilite) {
        this.supannCivilite = supannCivilite;
    }

	public String getSupannListeRouge() {
        return this.supannListeRouge;
    }

	public void setSupannListeRouge(String supannListeRouge) {
        this.supannListeRouge = supannListeRouge;
    }

	public String getSupannEtablissement() {
        return this.supannEtablissement;
    }

	public void setSupannEtablissement(String supannEtablissement) {
        this.supannEtablissement = supannEtablissement;
    }

	public String getSupannEntiteAffectation() {
        return this.supannEntiteAffectation;
    }

	public void setSupannEntiteAffectation(String supannEntiteAffectation) {
        this.supannEntiteAffectation = supannEntiteAffectation;
    }

	public String getSupannEntiteAffectationPrincipale() {
        return this.supannEntiteAffectationPrincipale;
    }

	public void setSupannEntiteAffectationPrincipale(String supannEntiteAffectationPrincipale) {
        this.supannEntiteAffectationPrincipale = supannEntiteAffectationPrincipale;
    }

	public String getSupannEmpId() {
        return this.supannEmpId;
    }

	public void setSupannEmpId(String supannEmpId) {
        this.supannEmpId = supannEmpId;
    }

	public String getSupannEmpCorps() {
        return this.supannEmpCorps;
    }

	public void setSupannEmpCorps(String supannEmpCorps) {
        this.supannEmpCorps = supannEmpCorps;
    }

	public String getSupannActivite() {
        return this.supannActivite;
    }

	public void setSupannActivite(String supannActivite) {
        this.supannActivite = supannActivite;
    }

	public String getSupannAutreTelephone() {
        return this.supannAutreTelephone;
    }

	public void setSupannAutreTelephone(String supannAutreTelephone) {
        this.supannAutreTelephone = supannAutreTelephone;
    }

	public String getSupannCodeINE() {
        return this.supannCodeINE;
    }

	public void setSupannCodeINE(String supannCodeINE) {
        this.supannCodeINE = supannCodeINE;
    }

	public String getSupannEtuId() {
        return this.supannEtuId;
    }

	public void setSupannEtuId(String supannEtuId) {
        this.supannEtuId = supannEtuId;
    }

	public String getSupannEtuEtape() {
        return this.supannEtuEtape;
    }

	public void setSupannEtuEtape(String supannEtuEtape) {
        this.supannEtuEtape = supannEtuEtape;
    }

	public String getSupannEtuAnneeInscription() {
        return this.supannEtuAnneeInscription;
    }

	public void setSupannEtuAnneeInscription(String supannEtuAnneeInscription) {
        this.supannEtuAnneeInscription = supannEtuAnneeInscription;
    }

	public String getSupannEtuSecteurDisciplinaire() {
        return this.supannEtuSecteurDisciplinaire;
    }

	public void setSupannEtuSecteurDisciplinaire(String supannEtuSecteurDisciplinaire) {
        this.supannEtuSecteurDisciplinaire = supannEtuSecteurDisciplinaire;
    }

	public String getSupannEtuDiplome() {
        return this.supannEtuDiplome;
    }

	public void setSupannEtuDiplome(String supannEtuDiplome) {
        this.supannEtuDiplome = supannEtuDiplome;
    }

	public String getSupannEtuTypeDiplome() {
        return this.supannEtuTypeDiplome;
    }

	public void setSupannEtuTypeDiplome(String supannEtuTypeDiplome) {
        this.supannEtuTypeDiplome = supannEtuTypeDiplome;
    }

	public List<String> getSupannEtuCursusAnnee() {
        return this.supannEtuCursusAnnee;
    }

	public void setSupannEtuCursusAnnee(List<String> supannEtuCursusAnnee) {
        this.supannEtuCursusAnnee = supannEtuCursusAnnee;
    }

	public String getSupannParrainDN() {
        return this.supannParrainDN;
    }

	public void setSupannParrainDN(String supannParrainDN) {
        this.supannParrainDN = supannParrainDN;
    }

	public String getSupannMailPerso() {
        return this.supannMailPerso;
    }

	public void setSupannMailPerso(String supannMailPerso) {
        this.supannMailPerso = supannMailPerso;
    }

	public String getSupannAliasLogin() {
        return this.supannAliasLogin;
    }

	public void setSupannAliasLogin(String supannAliasLogin) {
        this.supannAliasLogin = supannAliasLogin;
    }

	public List<String> getSupannRefId() {
        return this.supannRefId;
    }

	public void setSupannRefId(List<String> supannRefId) {
        this.supannRefId = supannRefId;
    }

	public String getSupannRoleGenerique() {
        return this.supannRoleGenerique;
    }

	public void setSupannRoleGenerique(String supannRoleGenerique) {
        this.supannRoleGenerique = supannRoleGenerique;
    }

	public String getSupannAutreMail() {
        return this.supannAutreMail;
    }

	public void setSupannAutreMail(String supannAutreMail) {
        this.supannAutreMail = supannAutreMail;
    }

	public Long getMailuserquota() {
        return this.mailuserquota;
    }

	public void setMailuserquota(Long mailuserquota) {
        this.mailuserquota = mailuserquota;
    }
}
	
	
