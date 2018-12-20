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

import java.math.BigInteger;
import java.util.Date;

public class ExportAll {
	
	private Date date; 
	private String email; 
	private String eppn;
	private String login;
	private String etablissement;
	private BigInteger nb_coupon; 
	private Integer nb_recharge; 
	private String nom; 
	private String prenom; 
	private Date date_recharge; 
	private String eppn_init; 
	private String lieu; 
	private String nom_salle; 
	private Integer tarif; 
	private String tarif_string; 
	private String type_salle;
	private String filiere; 
	private String civilite;
	private Double annee_naissance;
	private String niveau_etudes;
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getEppn() {
		return eppn;
	}
	public void setEppn(String eppn) {
		this.eppn = eppn;
	}
	public String getLogin() {
		return login;
	}
	public void setLogin(String login) {
		this.login = login;
	}
	public String getEtablissement() {
		return etablissement;
	}
	public void setEtablissement(String etablissement) {
		this.etablissement = etablissement;
	}
	public BigInteger getNb_coupon() {
		return nb_coupon;
	}
	public void setNb_coupon(BigInteger nb_coupon) {
		this.nb_coupon = nb_coupon;
	}
	public Integer getNb_recharge() {
		return nb_recharge;
	}
	public void setNb_recharge(Integer nb_recharge) {
		this.nb_recharge = nb_recharge;
	}
	public String getNom() {
		return nom;
	}
	public void setNom(String nom) {
		this.nom = nom;
	}
	public String getPrenom() {
		return prenom;
	}
	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}
	public Date getDate_recharge() {
		return date_recharge;
	}
	public void setDate_recharge(Date date_recharge) {
		this.date_recharge = date_recharge;
	}
	public String getEppn_init() {
		return eppn_init;
	}
	public void setEppn_init(String eppn_init) {
		this.eppn_init = eppn_init;
	}
	public String getLieu() {
		return lieu;
	}
	public void setLieu(String lieu) {
		this.lieu = lieu;
	}
	public String getNom_salle() {
		return nom_salle;
	}
	public void setNom_salle(String nom_salle) {
		this.nom_salle = nom_salle;
	}
	public Integer getTarif() {
		return tarif;
	}
	public void setTarif(Integer tarif) {
		this.tarif = tarif;
	}
	public String getTarif_string() {
		return tarif_string;
	}
	public void setTarif_string(String tarif_string) {
		this.tarif_string = tarif_string;
	}
	public String getType_salle() {
		return type_salle;
	}
	public void setType_salle(String type_salle) {
		this.type_salle = type_salle;
	}
	public String getFiliere() {
		return filiere;
	}
	public void setFiliere(String filiere) {
		this.filiere = filiere;
	}
	public String getCivilite() {
		return civilite;
	}
	public void setCivilite(String civilite) {
		this.civilite = civilite;
	}
	public String getAnnee_naissance() {
		if(annee_naissance != null) {
			return String.valueOf(annee_naissance.intValue());
		} else {
			return "";
		}
	}
	public void setAnnee_naissance(Double annee_naissance) {
		this.annee_naissance = annee_naissance;
	}
	public String getNiveau_etudes() {
		return niveau_etudes;
	}
	public void setNiveau_etudes(String niveau_etudes) {
		this.niveau_etudes = niveau_etudes;
	}
	
	
}
