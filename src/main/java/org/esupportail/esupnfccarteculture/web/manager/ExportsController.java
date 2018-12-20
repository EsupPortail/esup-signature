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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.esupportail.esupnfccarteculture.domain.ExportAll;
import org.esupportail.esupnfccarteculture.domain.ExportEmails;
import org.esupportail.esupnfccarteculture.domain.TagLog;
import org.esupportail.esupnfccarteculture.service.CsvService;
import org.esupportail.esupnfccarteculture.service.EtudiantService;
import org.esupportail.esupnfccarteculture.service.ExportService;
import org.esupportail.esupnfccarteculture.service.UtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

@RequestMapping("/manager/exports")
@Controller
public class ExportsController {

	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return "exports";
	}
	
	private final static byte[] EXCEL_UTF8_HACK = new byte[] { (byte)0xEF, (byte)0xBB, (byte)0xBF};

	@Resource
	ExportService exportService;

	@Resource
	UtilsService utilsService;	
	
	@Resource
	EtudiantService etudiantService;
	
	@Resource
	CsvService csvService;

	
	@ModelAttribute("active")
	String getCurrentMenu() {
		return "exports";
	}

	@RequestMapping(produces = "text/html")
	public String list(@RequestParam(value = "annee", required = false) Integer annee, Model uiModel) throws ParseException {
		if(annee==null) {
			annee = utilsService.getAnnee();
		}
		uiModel.addAttribute("chooseAnnee", annee);
		uiModel.addAttribute("annees", TagLog.findAnnees());
		uiModel.addAttribute("annee", annee);
		return "manager/exports";
	}

	@RequestMapping(value = "/emails/{annee}", method = RequestMethod.GET)
	public void exportEmails(@ModelAttribute(value = "annee") Integer annee, HttpServletResponse response, Model uiModel) throws IOException {
		if(annee==null) {
			annee = utilsService.getAnnee();
		}
		String reportName = "CSV_all_export.csv";
		response.setHeader("Content-disposition", "attachment;filename=" + reportName);
		response.setHeader("Set-Cookie", "fileDownload=true; path=/");
		response.setContentType("text/csv");
		response.setCharacterEncoding("UTF-8");
		response.getOutputStream().write(EXCEL_UTF8_HACK);
		Writer writer = new OutputStreamWriter(response.getOutputStream(), "UTF8");
		Field[] attributes = ExportEmails.class.getDeclaredFields();
		final String[] header = new String[attributes.length];
		int i = 0;
		for (Field field : attributes) {
			header[i]=field.getName();
			i++;
		}
		ICsvBeanWriter beanWriter =  new CsvBeanWriter(writer, CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE);
		beanWriter.writeHeader(header);
		List<ExportEmails> exports=null;
		try{
			exports = exportService.getEmailsExport(annee);
			for (ExportEmails export : exports) {
				beanWriter.write(export, header);
			}
			beanWriter.flush();
			writer.close();
			beanWriter.close();
		}catch(Exception e){
			log.error("interuption de l'export email !", e);
		}
	}
	
	@RequestMapping(value = "/all/{annee}", method = RequestMethod.GET)
	public void exportComplet(@ModelAttribute(value = "annee") Integer annee, HttpServletResponse response, Model uiModel) throws IOException, ParseException {
		
		if(annee==null) {
			annee = utilsService.getAnnee();
		}
		String reportName = "CSV_all_export.csv";
		response.setHeader("Content-disposition", "attachment;filename=" + reportName);
		response.setHeader("Set-Cookie", "fileDownload=true; path=/");
		response.setContentType("text/csv");
		response.setCharacterEncoding("UTF-8");
		response.getOutputStream().write(EXCEL_UTF8_HACK);
		Writer writer = new OutputStreamWriter(response.getOutputStream(), "UTF8");
		Field[] attributes = ExportAll.class.getDeclaredFields();
		final String[] header = new String[attributes.length];
		int i = 0;
		for (Field field : attributes) {
			header[i]=field.getName();
			i++;
		}
		ICsvBeanWriter beanWriter =  new CsvBeanWriter(writer, CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE);
		beanWriter.writeHeader(header);
		List<ExportAll> exports=null;
		try{
			exports = exportService.getEtudiantsExport(annee);
			for (ExportAll export : exports) {
				beanWriter.write(export, header);
			}
			beanWriter.flush();
			writer.close();
			beanWriter.close();
		}catch(Exception e){
			log.error("interuption de l'export !", e);
		}

	}
	
}
