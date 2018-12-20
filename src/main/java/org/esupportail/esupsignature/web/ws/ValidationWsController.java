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
package org.esupportail.esupsignature.web.ws;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import eu.europa.esig.dss.RemoteDocument;
import eu.europa.esig.dss.validation.RemoteDocumentValidationService;
import eu.europa.esig.dss.validation.reports.dto.ReportsDTO;

@RequestMapping("/service/rest/validation")
@Controller
public class ValidationWsController {

	private final Logger log = LoggerFactory.getLogger(getClass());

	RemoteDocumentValidationService service = new RemoteDocumentValidationService();

	@RequestMapping(value = "/validateSignature", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ReportsDTO validateDocument(@ModelAttribute("signedFile") RemoteDocument signedFile, 
			@ModelAttribute("originalFiles") List<RemoteDocument> originalFiles, 
			@ModelAttribute("policy") RemoteDocument policy) {
		return service.validateDocument(signedFile, originalFiles, policy);
	}
	
}
