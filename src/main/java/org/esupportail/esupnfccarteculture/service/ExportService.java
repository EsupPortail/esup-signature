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

import java.util.List;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.esupportail.esupnfccarteculture.domain.ExportAll;
import org.esupportail.esupnfccarteculture.domain.ExportEmails;
import org.hibernate.transform.Transformers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExportService {

	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	
	@PersistenceContext
	EntityManager entityManager;

	@Resource
	EtudiantService etudiantService;

	public List<ExportAll> getEtudiantsExport(int annee) {
		
		String queryString = "select "
				+ "tl.date as date, "
				+ "e.email as email, "
				+ "e.eppn as eppn, "
				+ "e.etablissement as etablissement, "
				+ "(select sum(coupons) from etudiant_coupons as ec where ec.etudiant = e.id group by ec.etudiant) as nb_coupon, "
				+ "e.nb_recharge as nb_recharge, "
				+ "e.nom as nom, "
				+ "e.prenom as prenom, "
				+ "e.date_recharge as date_recharge, "
				+ "tl.eppn_init as eppn_init, "
				+ "s.lieu as lieu, "
				+ "s.nom as nom_salle, "
				+ "s.tarif as tarif, "
				+ "s.type_salle as type_salle, "
				+ "e.filiere as filiere, "
				+ "e.civilite as civilite, "
				+ "date_part('year',e.date_naissance) as annee_naissance, "
				+ "e.niveau_etudes as niveau_etudes "
				+ "from tag_log as tl, etudiant as e, salle as s "
				+ "where tl.salle=s.id and tl.etudiant=e.id and tl.date between '" + annee + "-09-01' and '"+ (annee + 1) +"-08-31' order by tl.date";
	
		List<ExportAll> exports = entityManager.createNativeQuery(queryString).unwrap(org.hibernate.Query.class).setResultTransformer(Transformers.aliasToBean(ExportAll.class)).list();
	
		return exports;

	}
	
	public List<ExportEmails> getEmailsExport(int annee) {
		
		String queryString = "select "
				+ "e.nom as nom, "
				+ "e.prenom as prenom, "
				+ "e.email as email, "
				+ "e.etablissement as etablissement "
				+ "from etudiant as e "
				+ "where e.date_inscription between '" + annee + "-09-01' and '"+ (annee + 1) +"-08-31' order by e.nom";
	
		System.err.println(queryString);
		List<ExportEmails> exports = entityManager.createNativeQuery(queryString).unwrap(org.hibernate.Query.class).setResultTransformer(Transformers.aliasToBean(ExportEmails.class)).list();
		return exports;

	}

}
