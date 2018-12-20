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
package org.esupportail.esupnfccarteculture.web.ws;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.esupportail.esupnfccarteculture.domain.Etudiant;
import org.esupportail.esupnfccarteculture.service.EtudiantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("/ws")
@Controller
public class RestWsController {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Resource
	EtudiantService etudiantService;

	@RequestMapping(value="/ent", produces = "text/html")
	public String entWebProxyPortletView(@RequestParam(required = false) String eppn, HttpServletRequest request, Model uiModel) {
		Etudiant etudiant = null;
		String coupons = null;
		if(Etudiant.countFindEtudiantsByEppnEquals(eppn) > 0) {
			log.info("consultation ent pour : " + eppn);
			etudiant = Etudiant.findEtudiantsByEppnEquals(eppn).getSingleResult();
			coupons = etudiantService.affichageCoupons(etudiant);
		}
		uiModel.addAttribute("coupons", coupons);
		return "ent/index";
	}
	
	
	@RequestMapping(value="/etudiant", produces = "application/json")
	@ResponseBody
	public Etudiant getEtudiantJson(@RequestParam(required = true) String eppn, HttpServletRequest request, Model uiModel) {
		if(Etudiant.countFindEtudiantsByEppnEquals(eppn) > 0) {
			return Etudiant.findEtudiantsByEppnEquals(eppn).getSingleResult();
		}
		return null;
	}

}
