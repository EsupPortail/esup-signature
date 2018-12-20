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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.esupnfccarteculture.domain.Etudiant;
import org.esupportail.esupnfccarteculture.domain.TagLog;
import org.esupportail.esupnfccarteculture.domain.TypeSalle;
import org.esupportail.esupnfccarteculture.domain.TypeSalleInscription;
import org.esupportail.esupnfccarteculture.domain.TypeSalleJoker;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@Service
public class StatsService {

	@Resource
	TagService tagService;

	private Map<String, Integer>  nbTags;
	
	List<String> month = Arrays.asList( "9", "10", "11", "12", "1", "2", "3", "4", "5", "6", "7", "8");
	
	String[] backgroundColor = { "#FF5C4D", "#FF9933", "#FFD134",
			"#FFFF4F", "#FFFF67", "#E3FF3C", "#51FF62",
			"#5ADBFF", "#5A9FFF", "#D759FF",
			"#FF5AC1", "#A880FF", "#F7F7F7", "#CCCCCC",
			"#B2B2B2", "#4D4D4D", "#A45077", "#FDCA59",
			"#E64D4D", "#985972" };

	String[] hoverBackgroundColor = { "#FF291A", "#FF6600", "#FF9E01",
				"#FCD202", "#F8FF01", "#B0DE09", "#04D215",
				"#0D8ECF", "#0D52D1", "#2A0CD0", "#8A0CCF",
				"#CD0D74", "#754DEB", "#DDDDDD", "#999999",
				"#333333", "#000000", "#57032A", "#CA9726",
				"#990000", "#4B0C25" };
	
	public List<Object[]> countNumberTagByLocationInsc(int annee) {
		EntityManager em = TagLog.entityManager();
		Query q = em.createNativeQuery(
				"SELECT s.lieu as labels, count(t.id) as value FROM tag_log as t, salle as s WHERE s.id=t.salle AND s.type_salle = 'inscription' AND date BETWEEN '" + annee + "-09-01' AND '" + (annee + 1) + "-08-31' GROUP BY s.id ORDER BY value DESC");
		return q.getResultList();
	}
	

	public List<Object[]> countNumberTagByLocationSalle(int annee) {
		EntityManager em = TagLog.entityManager();
		Query q = em.createNativeQuery(
				"SELECT s.nom as labels, count(t.id) as value FROM tag_log as t, salle as s WHERE s.id=t.salle AND s.type_salle != 'inscription' AND date BETWEEN '" + annee + "-09-01' AND '" + (annee + 1) + "-08-31' GROUP BY s.id ORDER BY value DESC");
		return q.getResultList();
	}

	public List<Object[]> countNumberTagByWeekInsc(int annee) {
		Object[] saut = {null, null, null};
		List<Object[]> result = new ArrayList<Object[]>();
		EntityManager em = TagLog.entityManager();
		Query qInsc = em.createNativeQuery(
				"SELECT  trim(to_char(date_part('month', tg.date), '99')) AS labels, COALESCE('coupons distribués') AS label, COUNT(sl.id) AS value, date_part('month', tg.date) AS groupby FROM tag_log AS tg LEFT JOIN (SELECT id FROM salle WHERE type_salle = '" + TypeSalleInscription.getTypeSalleInscriptionSingleton().getNom() + "') AS sl ON sl.id=tg.salle AND date BETWEEN '" + annee + "-09-01' AND '" + (annee + 1) + "-08-31' GROUP BY groupby ORDER BY max(tg.date);");
		List<Object[]> qResultInsc = qInsc.getResultList();
		for (int i = 0; i< month.size(); i++){
			Object[] objectToAdd = {month.get(i), "coupons distribués", 0};
			for (Object[] object : qResultInsc) {
				if(object[0].toString().equals(month.get(i))){
					objectToAdd = object;
				}				}
			result.add(objectToAdd);
		}
		result.add(saut);
		return result;
	}
	
	public List<Object[]> countNumberTagByWeekUsed(int annee) {
		Object[] saut = {null, null, null};
    	List<Object[]> result = new ArrayList<Object[]>();
		EntityManager em = TagLog.entityManager();

		for(TypeSalle typeSalle : tagService.getTypeSallesDebitables()) {
			if(!typeSalle.getNom().equals(TypeSalleJoker.JOKER_NAME)) {
				String queryString = "SELECT trim(to_char(date_part('month', tg.date), '99')) AS labels, COALESCE('coupons " + typeSalle.getNom() + " utilisés') as label, COUNT(sl.id) AS value, date_part('month', tg.date) AS groupby FROM tag_log AS tg LEFT JOIN (SELECT id FROM salle WHERE type_salle = '" + typeSalle.getNom() + "') AS sl ON sl.id=tg.salle AND date BETWEEN '" + annee + "-09-01' AND '" + (annee + 1) + "-08-31' GROUP BY groupby ORDER BY max(tg.date);";
				Query query = em.createNativeQuery(queryString);
				List<Object[]> qResult = query.getResultList();
				for (int i = 0; i< month.size(); i++){
					Object[] objectToAdd = {month.get(i), "coupons " + typeSalle.getNom() + " utilisés", 0};
					for (Object[] object : qResult) {
						if(object[0].toString().equals(month.get(i))){
							objectToAdd = object;
						}	
					}		
					result.add(objectToAdd);
				}
				result.add(saut);
			}
		}
		
		return result;
	}	
	
	
	public String getNumberTagByLocationInsc(int annee) throws JsonProcessingException {
		return toChartJson(countNumberTagByLocationInsc(annee), TypeChart.pie, backgroundColor, hoverBackgroundColor);
	}

	public String getNumberTagByLocationSalle(int annee) throws JsonProcessingException {
		
		return toChartJson(countNumberTagByLocationSalle(annee), TypeChart.pie, backgroundColor, hoverBackgroundColor);
	}
	
	public String getNumberTagByWeekUsed(int annee) throws JsonProcessingException {
		return toChartJson(countNumberTagByWeekUsed(annee), TypeChart.bar, backgroundColor, hoverBackgroundColor);
	}

	public String getNumberTagByWeekInsc(int annee) throws JsonProcessingException {
		return toChartJson(countNumberTagByWeekInsc(annee), TypeChart.bar, backgroundColor, hoverBackgroundColor);
	}
	
	public String toChartJson(List<Object[]> results, TypeChart type, String[] backgroundColor, String[] hoverBackgroundColor) throws JsonProcessingException{
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String jsonOut = null;
		ChartJson chartJson = new ChartJson();
		ChartData chartData = null;
		boolean sautOk=false;
		boolean next=true;
		int colorCount=0;
		switch (type) {
		case bar:
			chartData = new ChartDataLine();
			sautOk=false;
			next=true;
			colorCount=0;
			for (Object[] object : results) {
				if(object[0]==null){
					chartJson.datasets.add(chartData);
					sautOk=true;
					next=true;
					continue;
				}
				if(!sautOk){
					boolean isDate = false;
		            DateFormat df = new SimpleDateFormat("MM");
		            df.setLenient(false);
		            try {
						df.parse(object[0].toString());
						isDate=true;
						
					} catch (ParseException e) {
					}
		            if(isDate){
	                	chartJson.labels.add(StringUtils.capitalize(new SimpleDateFormat("MMMM").format(df.getCalendar().getTime())));
		            }else{
						chartJson.labels.add(object[0].toString());		            	
		            }

				}
				if(next) {
					chartData = new ChartDataLine();
					chartData.backgroundColor = new String[results.size()];
					chartData.hoverBackgroundColor = new String[results.size()];
					for(int i=0; i<results.size();i++){
						chartData.backgroundColor[i]=backgroundColor[colorCount];
						chartData.hoverBackgroundColor[i]=hoverBackgroundColor[colorCount];
					}
					chartData.label.add(object[1].toString());
					next=false;
					colorCount++;
				}
				chartData.data.add(object[2].toString());
			}
			break;
		case line:
			chartData = new ChartDataLine();
			sautOk=false;
			next=true;
			colorCount=0;
			for (Object[] object : results) {
				if(object[0]==null){
					chartJson.datasets.add(chartData);
					sautOk=true;
					next=true;
					continue;
				}
				if(!sautOk) chartJson.labels.add(object[0].toString());
				if(next) {
					chartData = new ChartDataLine();
					chartData.backgroundColor = new String[1];
					chartData.backgroundColor[0]=backgroundColor[colorCount];
					chartData.label.add(object[1].toString());
					colorCount++;	
					next=false;
				}
				chartData.data.add(object[2].toString());
			}
			break;
		case pie:
			chartData = new ChartDataPie();
			for (Object[] object : results) {
				chartJson.labels.add(object[0].toString());
				chartData.data.add(object[1].toString());
			}
			chartJson.datasets.add(chartData);		
			chartData.backgroundColor=backgroundColor;
			chartData.hoverBackgroundColor=hoverBackgroundColor;
			break;

		default:
			break;
		}
	
		jsonOut = ow.writeValueAsString(chartJson);
		return jsonOut;
	}

	class ChartJson {
		public List<String> labels = new ArrayList<String>();
		public List<ChartData> datasets = new ArrayList<ChartData>();
	}
	
	class ChartData {
		public List<String> label = new ArrayList<String>();
		public List<String> data = new ArrayList<String>();
		public String[] backgroundColor;
		public String[] hoverBackgroundColor;
		
	}
	
	class ChartDataPie extends ChartData {
	}	
	
	public class ChartDataLine extends ChartData {
   		public Boolean fill = true;
		public String borderColor= "rgba(100,100,100,1)";
		public String pointBorderColor= "rgba(0,0,0,1)";
		public String pointBackgroundColor= "#fff";
		public int pointBorderWidth= 1;
		public int pointHoverRadius= 10;
		public String pointHoverBackgroundColor= "rgba(220,0,0,1)";
		public String pointHoverBorderColor= "rgba(100,100,100,1)";
		public int pointHoverBorderWidth= 2;
	}
	
	public class ChartDataBar extends ChartData {
   		public Boolean fill = true;
		public String borderColor= "rgba(100,100,100,1)";
		public String pointBorderColor= "rgba(0,0,0,1)";
		public String pointBackgroundColor= "#fff";
		public int pointBorderWidth= 1;
		public int pointHoverRadius= 10;
		public String pointHoverBackgroundColor= "rgba(220,0,0,1)";
		public String pointHoverBorderColor= "rgba(100,100,100,1)";
		public int pointHoverBorderWidth= 2;
	}
	
	
	public enum TypeChart{
		line, pie, bar
	}
	
    public Map<String, Integer> countCoupon(int annee) {
    	nbTags = null;
    	List<Etudiant> etudiants = Etudiant.findEtudiants(annee, null, null, null, null, 10000, null, null).getResultList();
    	for(Etudiant etudiant : etudiants) {
    		if(nbTags == null) {
    			nbTags = new HashMap<String, Integer>(etudiant.getCoupons());
    		} else {
    			etudiant.getCoupons().forEach((k, v) -> nbTags.merge(k, v, Integer::sum));
    		}
    		System.err.println(nbTags.get("joker"));
    	}
    	return nbTags;
    }
    
}
