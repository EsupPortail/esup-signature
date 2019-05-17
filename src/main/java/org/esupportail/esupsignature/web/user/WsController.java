package org.esupportail.esupsignature.web.user;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.domain.SignBook.SignBookType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@Transactional
@RequestMapping(value = "/ws/")
public class WsController {

	private static final Logger logger = LoggerFactory.getLogger(WsController.class);

	@Resource
	SignRequestService signRequestService;
	
	@Resource
	SignBookService signBookService;

	@Resource
	DocumentService documentService;
	
	@Autowired(required = false)
	private LdapPersonService ldapPersonService;
	
	@Resource
	private ReloadableResourceBundleMessageSource messageSource;
	
	//TODO creation / recupération de demandes par WS + declenchement d'evenements + multidocs
	@ResponseBody
	@RequestMapping(value = "/create-sign-request", method = RequestMethod.POST)
	public String createSignRequest(@RequestParam("file") MultipartFile file, @RequestParam String signBookName, HttpServletRequest httpServletRequest) throws IOException, ParseException, EsupSignatureException {
		SignRequest signRequest= new SignRequest();
		SignBook signBook = SignBook.findSignBooksByNameEquals(signBookName).getSingleResult();
		User user = getSystemUser();
		user.setIp(httpServletRequest.getRemoteAddr());
		if(file != null) {
			logger.info("adding new file into signbook" + signBookName);
			Document documentToAdd = documentService.createDocument(file, file.getOriginalFilename());
			signRequest = signRequestService.createSignRequest(new SignRequest(), user, documentToAdd, signBook.getSignRequestParams().get(0));
			signBookService.importSignRequestInSignBook(signRequest, signBook, user);
			signRequest.setTitle(signBookName);
			logger.info(file.getOriginalFilename() + " was added into signbook" + signBookName + " with id " + signRequest.getName());
			signRequestService.updateStatus(signRequest, SignRequestStatus.pending, messageSource.getMessage("updateinfo_wsupload", null, Locale.FRENCH), user, "SUCCESS", "");
			signRequest.merge();
			return signRequest.getName();
		} else {
			logger.warn("no file to import");
		}
		return null;
	}
	
	@Transactional
	@RequestMapping(value = "/get-signed-file", method = RequestMethod.GET)
	public ResponseEntity<Void> getSignedFile(@RequestParam String signBookName, @RequestParam String name, HttpServletResponse response, Model model) {
		try {
		SignBook signBook = SignBook.findSignBooksByNameEquals(signBookName).getSingleResult();
		SignRequest signRequest = SignRequest.findSignRequestsByNameEquals(name).getSingleResult();
		if(signBook.getSignRequests().contains(signRequest) && signRequest.getStatus().equals(SignRequestStatus.signed)) {
			Document document = signRequestService.getLastSignedDocument(signRequest);
			try {
				response.setHeader("Content-Disposition", "inline;filename=\"" + document.getFileName() + "\"");
				response.setContentType(document.getContentType());
				IOUtils.copy(document.getBigFile().getBinaryFile().getBinaryStream(), response.getOutputStream());
				return new ResponseEntity<>(HttpStatus.OK);
			} catch (Exception e) {
				logger.error("get file error", e);
			}
		} else {
			logger.warn("no signed version of " + name);
	        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		} catch (NoResultException e) {
			logger.error(e.getMessage(), e);
		}
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	}
	
	@Transactional
	@RequestMapping(value = "/complete-sign-request", method = RequestMethod.POST)
	public ResponseEntity<Void> completeSignRequest(@RequestParam String signBookName, @RequestParam String name, HttpServletRequest httpServletRequest, HttpServletResponse response, Model model) {
		try {
		SignBook signBook = SignBook.findSignBooksByNameEquals(signBookName).getSingleResult();
		SignRequest signRequest = SignRequest.findSignRequestsByNameEquals(name).getSingleResult();
		User user = getSystemUser();
		user.setIp(httpServletRequest.getRemoteAddr());
		if(signBook.getSignRequests().contains(signRequest) && signRequest.getStatus().equals(SignRequestStatus.signed)) {
			try {
				signBookService.removeSignRequestFromSignBook(signRequest, signBook, user);
				return new ResponseEntity<>(HttpStatus.OK);
			} catch (Exception e) {
				logger.error("get file error", e);
			}
		} else {
			logger.warn("no signed version of " + name);
	        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		} catch (NoResultException e) {
			logger.error(e.getMessage(), e);
		}
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	}
	
	@ResponseBody
	@RequestMapping(value = "/count-signed-in-signbook", produces = "text/html")
	public String countSignedInSignBook(@RequestParam String signBookName, HttpServletRequest httpServletRequest) throws IOException, ParseException {
		SignBook signBook = SignBook.findSignBooksByNameEquals(signBookName).getSingleResult();
		return String.valueOf(signBook.getSignRequests().stream().filter(s -> s.getStatus().equals(SignRequestStatus.signed)).count());
	}
	
	@Transactional
	@RequestMapping(value = "/move-sign-request", method = RequestMethod.POST)
	public ResponseEntity<Void> moveSignRequest(@RequestParam String signBookName, @RequestParam String name, HttpServletRequest httpServletRequest, HttpServletResponse response, Model model) {
		SignBook signBook = SignBook.findSignBooksByNameEquals(signBookName).getSingleResult();
		SignRequest signRequest = SignRequest.findSignRequestsByNameEquals(name).getSingleResult();
		User user = getSystemUser();
		user.setIp(httpServletRequest.getRemoteAddr());
		try {
			signBookService.removeSignRequestFromAllSignBooks(signRequest);
			signBookService.importSignRequestInSignBook(signRequest, signBook, user);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			logger.error("get file error", e);
		}
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	}
	
	@RequestMapping(value="/searchLdap")
	@ResponseBody
	public List<PersonLdap> searchLdap(@RequestParam(value="searchString") String searchString, @RequestParam(required=false) String ldapTemplateName) {
		logger.debug("ldap search for : " + searchString);
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
		List<PersonLdap> ldapList = new ArrayList<PersonLdap>();
		if(ldapPersonService != null && !searchString.trim().isEmpty() && searchString.length() > 3) {
			List<PersonLdap> ldapSearchList = new ArrayList<PersonLdap>();
			ldapSearchList = ldapPersonService.search(searchString, ldapTemplateName);
			ldapList.addAll(ldapSearchList.stream().sorted(Comparator.comparing(PersonLdap::getDisplayName)).collect(Collectors.toList()));

		}
		return ldapList;
   }
	
	@RequestMapping(value="/searchSignBook")
	@ResponseBody
	public List<PersonLdap> searchSignBook(@RequestParam(value="searchString") String searchString, @RequestParam(required=false) String ldapTemplateName) {
		logger.debug("signBook search for : " + searchString);
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
		List<PersonLdap> ldapList = new ArrayList<PersonLdap>();
		if(ldapPersonService != null && !searchString.trim().isEmpty() && searchString.length() > 3) {
			List<PersonLdap> ldapSearchList = new ArrayList<PersonLdap>();
			ldapSearchList = ldapPersonService.search(searchString, ldapTemplateName);
			ldapList.addAll(ldapSearchList.stream().sorted(Comparator.comparing(PersonLdap::getDisplayName)).collect(Collectors.toList()));

		}

		List<SignBook> signBooks = new ArrayList<>() ;
		signBooks.addAll(SignBook.findSignBooksBySignBookTypeEquals(SignBookType.system).getResultList());
		signBooks.addAll(SignBook.findSignBooksBySignBookTypeEquals(SignBookType.workflow).getResultList());
		signBooks.addAll(SignBook.findSignBooksBySignBookTypeEquals(SignBookType.group).getResultList());

		for(SignBook signBook : signBooks) {
			PersonLdap personLdap = new PersonLdap();
			personLdap.setUid(signBook.getSignBookType().toString());
			personLdap.setMail(signBook.getName());
			personLdap.setDisplayName(signBook.getName());
			ldapList.add(personLdap);
		}
		return ldapList;
   }
	
	public User getSystemUser() {
		User user = new User();
		user.setEppn("System");
		return user;
	}
}
