package org.esupportail.esupsignature.service.ldap;

import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

import javax.naming.Name;
import java.util.List;

@Entry(objectClasses = {"inetOrgPerson"})
public final class PersonLdap {

	@Id
	private Name dn;
	private @Attribute(name = "uid") String uid;
	private @Attribute(name = "cn") String cn;
	private @Attribute(name = "sn") String sn;
	private @Attribute(name = "givenName") String givenName;
	private @Attribute(name = "displayName") String displayName;
	private @Attribute(name = "schacDateOfBirth") String schacDateOfBirth;
	private @Attribute(name = "schacPlaceOfBirth") String schacPlaceOfBirth;
	private @Attribute(name = "mail") String mail;
	private @Attribute(name = "md5UserPassword") String md5UserPassword;
	private @Attribute(name = "cryptUserPassword") String cryptUserPassword;
	private @Attribute(name = "shaUserPassword") String shaUserPassword;
	private @Attribute(name = "eduPersonAffiliation") List<String> eduPersonAffiliation;
	private @Attribute(name = "eduPersonPrimaryAffiliation") String eduPersonPrimaryAffiliation;
	private @Attribute(name = "eduPersonPrincipalName") String eduPersonPrincipalName;
	private @Attribute(name = "mailDrop") String mailDrop;
	private @Attribute(name = "mailHost") String mailHost;
	private @Attribute(name = "sambaSID") String sambaSID;
	private @Attribute(name = "sambaPrimaryGroupSID") String sambaPrimaryGroupSID;
	private @Attribute(name = "sambaPwdLastSet") String sambaPwdLastSet;
	private @Attribute(name = "sambaLMPassword") String sambaLMPassword;
	private @Attribute(name = "sambaNTPassword") String sambaNTPassword;
	private @Attribute(name = "sambaAcctFlags") String sambaAcctFlags;
	private @Attribute(name = "homeDirectory") String homeDirectory;
	private @Attribute(name = "uidNumber") String uidNumber;
	private @Attribute(name = "gidNumber") String gidNumber;
	private @Attribute(name = "postalAddress") String postalAddress;
	private @Attribute(name = "facsimileTelephoneNumber") String facsimileTelephoneNumber;
	private @Attribute(name = "telephoneNumber") String telephoneNumber;
	private @Attribute(name = "supannCivilite") String supannCivilite;
	private @Attribute(name = "supannListeRouge") String supannListeRouge;
	private @Attribute(name = "supannEtablissement") String supannEtablissement;
	private @Attribute(name = "supannEntiteAffectation") String supannEntiteAffectation;
	private @Attribute(name = "supannEntiteAffectationPrincipale") String supannEntiteAffectationPrincipale;
	private @Attribute(name = "supannEmpId") String supannEmpId;
	private @Attribute(name = "supannEmpCorps") String supannEmpCorps;
	private @Attribute(name = "supannActivite") String supannActivite;
	private @Attribute(name = "supannAutreTelephone") String supannAutreTelephone;
	private @Attribute(name = "supannCodeINE") String supannCodeINE;
	private @Attribute(name = "supannEtuId") String supannEtuId;
	private @Attribute(name = "supannEtuEtape") String supannEtuEtape;
	private @Attribute(name = "supannEtuAnnee") String supannEtuAnneeInscription;
	private @Attribute(name = "supannEtuSecteurDisciplinaire") String supannEtuSecteurDisciplinaire;
	private @Attribute(name = "supannEtuDiplome") String supannEtuDiplome;
	private @Attribute(name = "supannEtuTypeDiplome") String supannEtuTypeDiplome;
	private @Attribute(name = "supannEtuCursusAnnee") List<String> supannEtuCursusAnnee;
	private @Attribute(name = "supannParrainDN") String supannParrainDN;
	private @Attribute(name = "supannMailPerso") String supannMailPerso;
	private @Attribute(name = "supannAliasLogin") String supannAliasLogin;
	private @Attribute(name = "supannRefId") List<String> supannRefId;
	private @Attribute(name = "supannRoleGenerique") String supannRoleGenerique;
	private @Attribute(name = "supannAutreMail") String supannAutreMail;
	private @Attribute(name = "mailuserquota") Long mailuserquota;
	private @Attribute(name = "title") String title;

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getCn() {
		return cn;
	}

	public void setCn(String cn) {
		this.cn = cn;
	}

	public String getSn() {
		return sn;
	}

	public void setSn(String sn) {
		this.sn = sn;
	}

	public String getGivenName() {
		return givenName;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getSchacDateOfBirth() {
		return schacDateOfBirth;
	}

	public void setSchacDateOfBirth(String schacDateOfBirth) {
		this.schacDateOfBirth = schacDateOfBirth;
	}

	public String getSchacPlaceOfBirth() {
		return schacPlaceOfBirth;
	}

	public void setSchacPlaceOfBirth(String schacPlaceOfBirth) {
		this.schacPlaceOfBirth = schacPlaceOfBirth;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getMd5UserPassword() {
		return md5UserPassword;
	}

	public void setMd5UserPassword(String md5UserPassword) {
		this.md5UserPassword = md5UserPassword;
	}

	public String getCryptUserPassword() {
		return cryptUserPassword;
	}

	public void setCryptUserPassword(String cryptUserPassword) {
		this.cryptUserPassword = cryptUserPassword;
	}

	public String getShaUserPassword() {
		return shaUserPassword;
	}

	public void setShaUserPassword(String shaUserPassword) {
		this.shaUserPassword = shaUserPassword;
	}

	public List<String> getEduPersonAffiliation() {
		return eduPersonAffiliation;
	}

	public void setEduPersonAffiliation(List<String> eduPersonAffiliation) {
		this.eduPersonAffiliation = eduPersonAffiliation;
	}

	public String getEduPersonPrimaryAffiliation() {
		return eduPersonPrimaryAffiliation;
	}

	public void setEduPersonPrimaryAffiliation(String eduPersonPrimaryAffiliation) {
		this.eduPersonPrimaryAffiliation = eduPersonPrimaryAffiliation;
	}

	public String getEduPersonPrincipalName() {
		return eduPersonPrincipalName;
	}

	public void setEduPersonPrincipalName(String eduPersonPrincipalName) {
		this.eduPersonPrincipalName = eduPersonPrincipalName;
	}

	public String getMailDrop() {
		return mailDrop;
	}

	public void setMailDrop(String mailDrop) {
		this.mailDrop = mailDrop;
	}

	public String getMailHost() {
		return mailHost;
	}

	public void setMailHost(String mailHost) {
		this.mailHost = mailHost;
	}

	public String getSambaSID() {
		return sambaSID;
	}

	public void setSambaSID(String sambaSID) {
		this.sambaSID = sambaSID;
	}

	public String getSambaPrimaryGroupSID() {
		return sambaPrimaryGroupSID;
	}

	public void setSambaPrimaryGroupSID(String sambaPrimaryGroupSID) {
		this.sambaPrimaryGroupSID = sambaPrimaryGroupSID;
	}

	public String getSambaPwdLastSet() {
		return sambaPwdLastSet;
	}

	public void setSambaPwdLastSet(String sambaPwdLastSet) {
		this.sambaPwdLastSet = sambaPwdLastSet;
	}

	public String getSambaLMPassword() {
		return sambaLMPassword;
	}

	public void setSambaLMPassword(String sambaLMPassword) {
		this.sambaLMPassword = sambaLMPassword;
	}

	public String getSambaNTPassword() {
		return sambaNTPassword;
	}

	public void setSambaNTPassword(String sambaNTPassword) {
		this.sambaNTPassword = sambaNTPassword;
	}

	public String getSambaAcctFlags() {
		return sambaAcctFlags;
	}

	public void setSambaAcctFlags(String sambaAcctFlags) {
		this.sambaAcctFlags = sambaAcctFlags;
	}

	public String getHomeDirectory() {
		return homeDirectory;
	}

	public void setHomeDirectory(String homeDirectory) {
		this.homeDirectory = homeDirectory;
	}

	public String getUidNumber() {
		return uidNumber;
	}

	public void setUidNumber(String uidNumber) {
		this.uidNumber = uidNumber;
	}

	public String getGidNumber() {
		return gidNumber;
	}

	public void setGidNumber(String gidNumber) {
		this.gidNumber = gidNumber;
	}

	public String getPostalAddress() {
		return postalAddress;
	}

	public void setPostalAddress(String postalAddress) {
		this.postalAddress = postalAddress;
	}

	public String getFacsimileTelephoneNumber() {
		return facsimileTelephoneNumber;
	}

	public void setFacsimileTelephoneNumber(String facsimileTelephoneNumber) {
		this.facsimileTelephoneNumber = facsimileTelephoneNumber;
	}

	public String getTelephoneNumber() {
		return telephoneNumber;
	}

	public void setTelephoneNumber(String telephoneNumber) {
		this.telephoneNumber = telephoneNumber;
	}

	public String getSupannCivilite() {
		return supannCivilite;
	}

	public void setSupannCivilite(String supannCivilite) {
		this.supannCivilite = supannCivilite;
	}

	public String getSupannListeRouge() {
		return supannListeRouge;
	}

	public void setSupannListeRouge(String supannListeRouge) {
		this.supannListeRouge = supannListeRouge;
	}

	public String getSupannEtablissement() {
		return supannEtablissement;
	}

	public void setSupannEtablissement(String supannEtablissement) {
		this.supannEtablissement = supannEtablissement;
	}

	public String getSupannEntiteAffectation() {
		return supannEntiteAffectation;
	}

	public void setSupannEntiteAffectation(String supannEntiteAffectation) {
		this.supannEntiteAffectation = supannEntiteAffectation;
	}

	public String getSupannEntiteAffectationPrincipale() {
		return supannEntiteAffectationPrincipale;
	}

	public void setSupannEntiteAffectationPrincipale(String supannEntiteAffectationPrincipale) {
		this.supannEntiteAffectationPrincipale = supannEntiteAffectationPrincipale;
	}

	public String getSupannEmpId() {
		return supannEmpId;
	}

	public void setSupannEmpId(String supannEmpId) {
		this.supannEmpId = supannEmpId;
	}

	public String getSupannEmpCorps() {
		return supannEmpCorps;
	}

	public void setSupannEmpCorps(String supannEmpCorps) {
		this.supannEmpCorps = supannEmpCorps;
	}

	public String getSupannActivite() {
		return supannActivite;
	}

	public void setSupannActivite(String supannActivite) {
		this.supannActivite = supannActivite;
	}

	public String getSupannAutreTelephone() {
		return supannAutreTelephone;
	}

	public void setSupannAutreTelephone(String supannAutreTelephone) {
		this.supannAutreTelephone = supannAutreTelephone;
	}

	public String getSupannCodeINE() {
		return supannCodeINE;
	}

	public void setSupannCodeINE(String supannCodeINE) {
		this.supannCodeINE = supannCodeINE;
	}

	public String getSupannEtuId() {
		return supannEtuId;
	}

	public void setSupannEtuId(String supannEtuId) {
		this.supannEtuId = supannEtuId;
	}

	public String getSupannEtuEtape() {
		return supannEtuEtape;
	}

	public void setSupannEtuEtape(String supannEtuEtape) {
		this.supannEtuEtape = supannEtuEtape;
	}

	public String getSupannEtuAnneeInscription() {
		return supannEtuAnneeInscription;
	}

	public void setSupannEtuAnneeInscription(String supannEtuAnneeInscription) {
		this.supannEtuAnneeInscription = supannEtuAnneeInscription;
	}

	public String getSupannEtuSecteurDisciplinaire() {
		return supannEtuSecteurDisciplinaire;
	}

	public void setSupannEtuSecteurDisciplinaire(String supannEtuSecteurDisciplinaire) {
		this.supannEtuSecteurDisciplinaire = supannEtuSecteurDisciplinaire;
	}

	public String getSupannEtuDiplome() {
		return supannEtuDiplome;
	}

	public void setSupannEtuDiplome(String supannEtuDiplome) {
		this.supannEtuDiplome = supannEtuDiplome;
	}

	public String getSupannEtuTypeDiplome() {
		return supannEtuTypeDiplome;
	}

	public void setSupannEtuTypeDiplome(String supannEtuTypeDiplome) {
		this.supannEtuTypeDiplome = supannEtuTypeDiplome;
	}

	public List<String> getSupannEtuCursusAnnee() {
		return supannEtuCursusAnnee;
	}

	public void setSupannEtuCursusAnnee(List<String> supannEtuCursusAnnee) {
		this.supannEtuCursusAnnee = supannEtuCursusAnnee;
	}

	public String getSupannParrainDN() {
		return supannParrainDN;
	}

	public void setSupannParrainDN(String supannParrainDN) {
		this.supannParrainDN = supannParrainDN;
	}

	public String getSupannMailPerso() {
		return supannMailPerso;
	}

	public void setSupannMailPerso(String supannMailPerso) {
		this.supannMailPerso = supannMailPerso;
	}

	public String getSupannAliasLogin() {
		return supannAliasLogin;
	}

	public void setSupannAliasLogin(String supannAliasLogin) {
		this.supannAliasLogin = supannAliasLogin;
	}

	public List<String> getSupannRefId() {
		return supannRefId;
	}

	public void setSupannRefId(List<String> supannRefId) {
		this.supannRefId = supannRefId;
	}

	public String getSupannRoleGenerique() {
		return supannRoleGenerique;
	}

	public void setSupannRoleGenerique(String supannRoleGenerique) {
		this.supannRoleGenerique = supannRoleGenerique;
	}

	public String getSupannAutreMail() {
		return supannAutreMail;
	}

	public void setSupannAutreMail(String supannAutreMail) {
		this.supannAutreMail = supannAutreMail;
	}

	public Long getMailuserquota() {
		return mailuserquota;
	}

	public void setMailuserquota(Long mailuserquota) {
		this.mailuserquota = mailuserquota;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
	
	
