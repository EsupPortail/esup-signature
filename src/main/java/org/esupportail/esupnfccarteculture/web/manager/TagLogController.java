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
package org.esupportail.esupnfccarteculture.web.manager;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;

import org.esupportail.esupnfccarteculture.domain.Salle;
import org.esupportail.esupnfccarteculture.domain.TagLog;
import org.esupportail.esupnfccarteculture.service.UtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/manager/taglogs")
@Controller
@RooWebScaffold(path = "manager/taglogs", formBackingObject = TagLog.class, create=false, update=false, delete=false)
public class TagLogController {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    
	@ModelAttribute("active")
	public String getActiveMenu() {
		return "taglogs";
	}
    
	List<String> listSearchBy = Arrays.asList("authDate", "salle", "etudiant");
	
	@Resource
	UtilsService utilsService;
	
    @RequestMapping(produces = "text/html")
    public String list(
    		@RequestParam(value = "annee", required = false) Integer annee,
    		@RequestParam(value = "salleFilter", required = false) String salleFilter,
    		@RequestParam(value = "dateFilter", required = false) String dateFilter,
    		@RequestParam(value = "searchString", required = false) String searchString,
    		@RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "sortFieldName", required = false) String sortFieldName, @RequestParam(value = "sortOrder", required = false) String sortOrder, Model uiModel) {
    	
    	if(annee == null) {
    		annee = utilsService.getAnnee();
    	}
    	
    	List<Salle> salles = Salle.findAllSalles();
    	    	
    	if(sortFieldName == null) {
    		sortFieldName="date";
    		sortOrder="DESC";
    	}
    	if(salleFilter == null) {
    		salleFilter = "";
    	}
    	if(searchString == null) {
    		searchString = "";
    	}
        int sizeNo = size == null ? 10 : size.intValue();
       
        List<TagLog> taglogs = TagLog.findTagLogs(annee, salleFilter, dateFilter, searchString, page, size, sortFieldName, sortOrder).getResultList();

        float nrOfPages = (float) TagLog.countFindTagLogs(annee, salleFilter, dateFilter, searchString) / sizeNo;
        
        addDateTimeFormatPatterns(uiModel);
        uiModel.addAttribute("annee", annee);
    	uiModel.addAttribute("annees", TagLog.findAnnees());        
        uiModel.addAttribute("page", page);
        uiModel.addAttribute("size", size);
        uiModel.addAttribute("taglogs", taglogs);
        uiModel.addAttribute("maxPages", (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages));
        uiModel.addAttribute("searchString", searchString);
        uiModel.addAttribute("salleFilter", salleFilter);
        uiModel.addAttribute("dateFilter", dateFilter);
        uiModel.addAttribute("listSearchBy", listSearchBy);
        uiModel.addAttribute("salles", salles);
        uiModel.addAttribute("queryUrl", "?salleFilter=" + salleFilter + "&searchString=" + searchString);
        return "manager/taglogs/list";
    }
	
  }
