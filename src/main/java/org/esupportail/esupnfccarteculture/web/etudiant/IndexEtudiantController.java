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
package org.esupportail.esupnfccarteculture.web.etudiant;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;

import org.esupportail.esupnfccarteculture.domain.Etudiant;
import org.esupportail.esupnfccarteculture.domain.Salle;
import org.esupportail.esupnfccarteculture.domain.TagLog;
import org.esupportail.esupnfccarteculture.domain.TypeSalleInscription;
import org.esupportail.esupnfccarteculture.ldap.PersonLdap;
import org.esupportail.esupnfccarteculture.ldap.PersonLdapDao;
import org.esupportail.esupnfccarteculture.service.EtudiantService;
import org.esupportail.esupnfccarteculture.service.TagService;
import org.esupportail.esupnfccarteculture.service.UtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RequestMapping("/etudiant")
@Controller
public class IndexEtudiantController {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	@ModelAttribute("active")
	public String getActiveMenu() {
		return "etudiant";
	}
	
	@Resource
	EtudiantService etudiantService;
	
	@Resource
	TagService tagService;

	@Autowired
	List<PersonLdapDao> personDaos;
	
	@Resource
	UtilsService utilsService;
	
	@RequestMapping(produces = "text/html")
	public String index(Model uiModel) throws ParseException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
		String csn = null;
		String statut = "KO";
		String msg = "";
		String coupons = "";
		Etudiant etudiant = null;
		List<TagLog> tagLogs = null;
		PersonLdap person = etudiantService.getPersonFromEppn(eppn);
		if(person != null) {
			statut = "OK";
			if(Etudiant.countFindEtudiantsByEppnEquals(eppn) > 0) {
				etudiant = Etudiant.findEtudiantsByEppnEquals(eppn).getSingleResult();
			}
			if(etudiant != null) {
				DateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
				Date dateBegin = format.parse("01/09/"+utilsService.getAnnee());
				if(etudiant.getDateInscription().after(dateBegin)){
					tagLogs = TagLog.findTagLogs(utilsService.getAnnee(), null, null, etudiant.getEppn(), null, null, "date", "desc").getResultList();
					String oldCsn = etudiant.getCsn();
					try {
						etudiantService.updateEtudiant(etudiant);
					} catch (Exception e) {
						log.error("erreur lors de la mise à jour de " + etudiant.getEppn(), e);
					}
					if(!oldCsn.equals(etudiant.getCsn())) {
						msg = "Votre nouvelle carte à bien été prise en compte";	
					}
					coupons = etudiantService.affichageCoupons(etudiant);
					log.info("Acces espace etudiant : " + eppn);
				} else {
					etudiant = null;
					log.info("Acces etudiant déja connu : " + eppn);
					if(etudiantService.isPreInscription()){
						msg = "Votre statut vous permet de vous pré-inscrire";
					} else {
						msg = "Votre status vous permet de recharger votre carte auprès de votre service carte culture";
					}
				}
			} else {
				log.warn("Etudiant non trouvé dans la base carte culture : " + eppn);
				csn = etudiantService.searchLdapCsnByEppn(eppn);
				if(csn != null) {
					if(etudiantService.isPreInscription()) {
						msg = "Votre statut vous permet de vous pré-inscrire";
					} else {
						msg = "Votre status vous permet de recharger votre carte auprès de votre service carte culture";
					}
				} else {
					log.info("Acces etudiant sans carte : " + eppn);
					msg ="Merci de vous procurer une carte etudiant pour pouvoir vous inscrire";
				}
			}
		} else {
			msg = "Accès refusé, merci de contacter le service carte culture";
			log.warn("essai de connexion de : " + eppn + " avec une mauvaise affiliation");
		}
		uiModel.addAttribute("taglogs", tagLogs);
		uiModel.addAttribute("etudiant", etudiant);
		uiModel.addAttribute("coupons", coupons);
		uiModel.addAttribute("msg", msg);	
		uiModel.addAttribute("statut", statut);
		uiModel.addAttribute("csn", csn);
		uiModel.addAttribute("isPreInscription", etudiantService.isPreInscription());
		return "etudiant/index";
	}
	
	@Transactional
	@RequestMapping(value = "/inscription", produces = "text/html")
	public String preInscription(RedirectAttributes redirectAttrs, Model uiModel) throws ParseException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
		if(etudiantService.isPreInscription()) {
			Salle salleInscription = null;
			if(Salle.countFindSallesByNomEquals(etudiantService.getPreInscriptionNomSalle()) > 0 ) {
				salleInscription = Salle.findSallesByNomEquals(etudiantService.getPreInscriptionNomSalle()).getSingleResult();
			} else {
				log.info("création salle Pré-inscription en base : " + etudiantService.getPreInscriptionNomSalle());
				salleInscription = new Salle();
				salleInscription.setNom(etudiantService.getPreInscriptionNomSalle());
				salleInscription.setLieu("web");
				salleInscription.setTypeSalle(TypeSalleInscription.getTypeSalleInscriptionSingleton().toString());
				salleInscription.persist();
			}
			
			PersonLdap person = etudiantService.getPersonFromEppn(eppn);
			if(person != null) {
				Etudiant etudiant = null;
				if(Etudiant.countFindEtudiantsByEppnEquals(eppn) > 0 ) {
					etudiant = Etudiant.findEtudiantsByEppnEquals(eppn).getSingleResult();
					DateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
					Date dateBegin = format.parse("31/08/"+utilsService.getAnnee());
					if(etudiant.getDateInscription().after(dateBegin)){
						return "redirect:/etudiant";
					}
				} else {
					etudiant = new Etudiant();
					etudiant.setEppn(eppn);
					etudiant.persist();
				}
				etudiant.setDateInscription(new Date());
				etudiant.setCoupons(null);
				etudiant.setNbRecharge(0);
				try {
					etudiantService.updateEtudiant(etudiant);
					tagService.recharge(etudiant);
					tagService.createNewTagLog(etudiant, salleInscription, eppn);
					redirectAttrs.addFlashAttribute("messageInfo", "message_info_preinscription");
					return "redirect:/etudiant";
				} catch (Exception e) {
					log.error("erreur lors de la mise à jour de " + eppn, e);
					redirectAttrs.addFlashAttribute("messageError", "message_error_inscription");
				}
			} else {
				log.warn("Problème lors de l'inscription  : " + eppn);
				redirectAttrs.addFlashAttribute("messageError", "message_error_inscription");
			}
		} 
		return "redirect:/etudiant";
	}
}
