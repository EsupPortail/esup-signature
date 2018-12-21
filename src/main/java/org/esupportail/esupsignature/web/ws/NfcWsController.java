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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.esupportail.esupsignature.domain.EsupNfcTagLog;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.ldap.PersonLdapDao;
import org.esupportail.esupsignature.service.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("/nfc-ws")
@Controller
public class NfcWsController {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Resource
	TagService tagService;
	
	@Resource(name="personDaoComue")
	PersonLdapDao personLdapDao;
	
	@RequestMapping(value = "/tagIdCheck", method = RequestMethod.GET)
	@ResponseBody
	public EsupNfcTagLog tagIdCheck(@RequestParam(required = false) String desfireId,
			@RequestParam(required = false) String csn) {
		EsupNfcTagLog esupNfcTagLog = null;
		PersonLdap person = personLdapDao.getPersonLdaps("csn", csn).get(0);
		if(person != null) {
			esupNfcTagLog = new EsupNfcTagLog();
			esupNfcTagLog.setCsn(csn.toUpperCase());
			esupNfcTagLog.setEppn(person.getEduPersonPrincipalName());
			esupNfcTagLog.setFirstname(person.getGivenName());
			esupNfcTagLog.setLastname(person.getSn());
		}
		return esupNfcTagLog;
	}
	
	@RequestMapping(value = "/getLocations", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public List<String> getLocations(@RequestParam String eppn) {
		List<String> listSalles = new ArrayList<String>();
		listSalles.add("Signature");
		return listSalles;
	}

	/* curl -H "Content-Type: application/json" -X POST -d '{"eppn": "test@univ-ville.fr", "location": "Olympia"}' https://carte-culture.univ-ville.fr/nfc-ws/isTagable */
	@RequestMapping(value = "/isTagable", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<String> isTagable(@RequestBody EsupNfcTagLog esupNfcTagLog) {
		HttpHeaders responseHeaders = new HttpHeaders();
		PersonLdap person = new PersonLdap();
		if(person.getSambaPrimaryGroupSID() != null) {
			return new ResponseEntity<String>("OK", responseHeaders, HttpStatus.OK);
		} else {
			return new ResponseEntity<String>("Non autorisé", responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	/* curl -H "Content-Type: application/json" -X POST -d '{"eppn": "test@univ-ville.fr", "location": "Olympia", "eppnInit": "gest@univ-ville.fr"}' http://carte-culture.univ-ville.fr/nfc-ws/validateTag */
	@Transactional
	@RequestMapping(value = "/validateTag", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<String> validateTag(@RequestBody EsupNfcTagLog esupNfcTagLog, HttpServletRequest httpServletRequest) {
		HttpHeaders responseHeaders = new HttpHeaders();
		return new ResponseEntity<String>("OK", responseHeaders, HttpStatus.OK);
		//return new ResponseEntity<String>("KO", responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@RequestMapping(value="/display",  method=RequestMethod.POST)
	@ResponseBody
	public String verso(@RequestBody EsupNfcTagLog taglog) {
		log.info("get verso from : " + taglog);
		return "Signature OK";
	}
	
}