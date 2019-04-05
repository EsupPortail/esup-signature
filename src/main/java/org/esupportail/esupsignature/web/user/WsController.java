package org.esupportail.esupsignature.web.user;

import java.io.IOException;
import java.text.ParseException;

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
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping(value = "/ws/")
public class WsController {

	private static final Logger logger = LoggerFactory.getLogger(WsController.class);

	@Resource
	SignRequestService signRequestService;
	
	@Resource
	SignBookService signBookService;

	@Resource
	DocumentService documentService;
	
	//TODO creation / recupération de demandes par WS + declenchement d'evenements + multidocs
	@Transactional
	@ResponseBody
	@RequestMapping(value = "/create-sign-request", method = RequestMethod.POST)
	public String createSignRequest(@RequestParam("file") MultipartFile file, @RequestParam String signBookName, HttpServletRequest httpServletRequest) throws IOException, ParseException {
		System.err.println(signBookName);
		SignRequest signRequest= new SignRequest();
		SignBook signBook = SignBook.findSignBooksByNameEquals(signBookName).getSingleResult();
		User user = getSystemUser();
		user.setIp(httpServletRequest.getRemoteAddr());
		if(file != null) {
			Document document = documentService.createDocument(file, file.getOriginalFilename());
			signRequest = signRequestService.createSignRequest(new SignRequest(), user, document, signBook.getSignRequestParams(), signBook.getRecipientEmails());
			logger.info(file.getOriginalFilename() + "was added into signbook" + signBookName);
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
	
	//TODO : move signRequest to signBook
	
	public User getSystemUser() {
		User user = new User();
		user.setEppn("System");
		return user;
	}
}
