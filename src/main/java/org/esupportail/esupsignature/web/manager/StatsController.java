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
package org.esupportail.esupsignature.web.manager;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.annotation.Resource;

import org.esupportail.esupsignature.service.StatsService;
import org.esupportail.esupsignature.service.TagService;
import org.esupportail.esupsignature.service.UtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("/manager/stats")
@Controller
public class StatsController {
	
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return "stats";
	}
	
	@Resource
	StatsService statsService;	

	@Resource
	UtilsService utilsService;	
	
	@Resource
	TagService tagService;
	
	@RequestMapping
	public String index(@RequestParam(required = false, value="annee") Integer annee, Model uiModel) throws ParseException {
		
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
		if(annee==null) {
			annee = utilsService.getAnnee();
		}
	
		return "manager/stats";
	}
	
	@RequestMapping(value="/chartJson", headers = "Accept=application/json; charset=utf-8")
	@ResponseBody 
	public String getStats(@RequestParam(required = false, value="model") StatsModel model, @RequestParam(required = false, value="annee") int annee) {
		String json = "Aucune statistique à récupérer";
		try {
			switch (model) {
			case numberTagByLocationInsc :
				json = statsService.getNumberTagByLocationInsc(annee);				
				break;
			case numberTagByLocationSalle :
				json = statsService.getNumberTagByLocationSalle(annee);				
				break;			
			default:
				break;
			}
			
		} catch (Exception e) {
			log.warn("Impossible de récupérer les statistiques", e);
		}
    	return json;
	}
	
	public enum StatsModel{
		numberTagByLocationInsc, numberTagByLocationSalle, numberTagByWeek, numberTagByWeekInsc, numberTagByWeekUsed
	}

}
