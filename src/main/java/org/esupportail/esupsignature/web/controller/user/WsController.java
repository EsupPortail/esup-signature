package org.esupportail.esupsignature.web.controller.user;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.esupportail.esupsignature.web.JsonSignInfoMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@Transactional
@RequestMapping(value = "/ws/")
public class WsController {

	private static final Logger logger = LoggerFactory.getLogger(WsController.class);

	@Autowired
	private SignRequestRepository signRequestRepository;

	@Resource
	private SignRequestService signRequestService;

	@Autowired
	private SignBookRepository signBookRepository;

	@Resource
	private SignBookService signBookService;

	@Autowired
	private UserRepository userRepository;

	@Resource
	private UserService userService;
	
	@Resource
	private FileService fileService;
	
	@Resource
	private DocumentService documentService;

	@Autowired(required = false)
	private LdapPersonService ldapPersonService;

	// TODO creation / recupération de demandes par WS + declenchement
	// d'evenements + multidocs
	@ResponseBody
	@RequestMapping(value = "/create-sign-request", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String createSignRequest(@RequestParam("file") MultipartFile file, @RequestParam String signBookName, @RequestParam String creatorEmail, HttpServletRequest httpServletRequest) throws IOException, ParseException, EsupSignatureException {
		SignRequest signRequest = new SignRequest();
		SignBook signBook = signBookRepository.findByName(signBookName).get(0);
		User user = userRepository.findByEmail(creatorEmail).get(0);
		user.setIp(httpServletRequest.getRemoteAddr());
		if (file != null) {
			logger.info("adding new file into signbook" + signBookName);
			Document documentToAdd = documentService.createDocument(file, file.getOriginalFilename());
			signRequest = signRequestService.createSignRequest(new SignRequest(), user, documentToAdd, signBook.getSignRequestParams().get(0));
			signBookService.importSignRequestInSignBook(signRequest, signBook, user);
			signRequest.setTitle(file.getOriginalFilename());
			logger.info(file.getOriginalFilename() + " was added into signbook" + signBookName + " with id " + signRequest.getName());
			signRequestService.pendingSignRequest(signRequest, user);
			signRequestRepository.save(signRequest);
			return signRequest.getName();
		} else {
			logger.warn("no file to import");
		}
		return null;
	}

	@ResponseBody
	@RequestMapping(value = "/create-sign-book", method = RequestMethod.POST)
	public String createSignBook(@RequestParam String signBookString, @RequestParam String signBookType, HttpServletRequest httpServletRequest) throws IOException, ParseException, EsupSignatureException {
		User user = getSystemUser();
		user.setIp(httpServletRequest.getRemoteAddr());
		ObjectMapper mapper = new ObjectMapper();
		if (signBookType.equals("workflow")) {
			signBookService.createWorkflowSignBook(mapper.readValue(signBookString, SignBook.class), user, signRequestService.getEmptySignRequestParams(), null, true);
		} else {
			SignBook newSignBook = mapper.readValue(signBookString, SignBook.class);
			
			if(newSignBook.getRecipientEmails().size() > 1 ) {
				SignBook signBookCheck = null;
				for(String recipientEmail : newSignBook.getRecipientEmails()) {
					if(signBookRepository.countByRecipientEmailsContainAndSignBookType(newSignBook.getRecipientEmails().get(0), SignBookType.group) > 0 && signBookCheck == null) {
						signBookCheck = signBookRepository.findByRecipientEmailsContainAndSignBookType(newSignBook.getRecipientEmails().get(0), SignBookType.group).get(0);
					} else {
						if(signBookCheck != null && !signBookCheck.getRecipientEmails().contains(recipientEmail)) {
							break;
						}
					}
				}
				if(signBookCheck != null && signBookCheck.getRecipientEmails().size() == newSignBook.getRecipientEmails().size()) {
					return signBookCheck.getName();
				}
			} else {
				if(signBookRepository.countByRecipientEmailsAndSignBookType(newSignBook.getRecipientEmails(), SignBookType.user) > 0) {
					return signBookRepository.findByRecipientEmailsAndSignBookType(newSignBook.getRecipientEmails(), SignBookType.user).get(0).getName();	
				}
			}
			signBookService.createGroupSignBook(newSignBook, user, signRequestService.getEmptySignRequestParams(), null, true);
			return newSignBook.getName();

		}
		return "";

	}

	@ResponseBody
	@RequestMapping(value = "/delete-sign-book", method = RequestMethod.POST)
	public ResponseEntity<String> deleteSignBook(@RequestParam String signBookName, HttpServletRequest httpServletRequest) throws IOException, ParseException, EsupSignatureException {
		User user = getSystemUser();
		user.setIp(httpServletRequest.getRemoteAddr());
		if(signBookRepository.countByName(signBookName) > 0) {
			SignBook signBook = signBookRepository.findByName(signBookName).get(0);
			signBookService.deleteSignBook(signBook);
		}
		return new ResponseEntity<String>(HttpStatus.OK);
	}

	@Transactional
	@RequestMapping(value = "/get-signed-file", method = RequestMethod.GET)
	public ResponseEntity<Void> getSignedFile(@RequestParam String signBookName, @RequestParam String name, HttpServletResponse response, Model model) throws Exception {
		try {
			SignBook signBook = signBookRepository.findByName(signBookName).get(0);
			SignRequest signRequest = signRequestRepository.findByName(signBookName).get(0);
			if (signBook.getSignRequests().contains(signRequest) && signRequest.getStatus().equals(SignRequestStatus.signed)) {
				File file = signRequestService.getLastSignedFile(signRequest);
				try {
					response.setHeader("Content-Disposition", "inline;filename=\"" + file.getName() + "\"");
					response.setContentType(fileService.getContentType(file));
					IOUtils.copy(new FileInputStream(file), response.getOutputStream());
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
	@RequestMapping(value = "/get-last-file", method = RequestMethod.GET)
	public ResponseEntity<Void> getLastFile(@RequestParam String name, HttpServletResponse response, Model model) {
		try {
			//TODO add user to check right
			SignRequest signRequest = signRequestRepository.findByName(name).get(0);
			if(signRequest != null) {
				try {
					File file = signRequestService.getLastSignedFile(signRequest);
					if(file == null) {
						file = signRequest.getOriginalDocuments().get(0).getJavaIoFile();
					}
					response.setHeader("Content-Disposition", "inline;filename=\"" + file.getName() + "\"");
					response.setContentType(fileService.getContentType(file));
					IOUtils.copy(new FileInputStream(file), response.getOutputStream());
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
	@RequestMapping(value = "/check-sign-request", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public JsonSignInfoMessage checkSignRequest(@RequestParam String fileToken, HttpServletResponse response, Model model) throws JsonProcessingException {
		try {
			if (signRequestRepository.countByName(fileToken) > 0) {
				SignRequest signRequest = signRequestRepository.findByName(fileToken).get(0);
				JsonSignInfoMessage jsonSignInfoMessage = new JsonSignInfoMessage();
				jsonSignInfoMessage.setStatus(signRequest.getStatus().toString());
				List<String> recipientNames = new ArrayList<String>();
				List<String> recipientEmails = new ArrayList<String>();
				signRequestService.setSignBooksLabels(signRequest);
				for (String recipientName : signRequest.getSignBooksLabels().keySet()) {
					SignBook signBook = signBookRepository.findByName(recipientName).get(0);
					if(!signRequest.getSignBooks().get(signBook.getId())) {
						recipientNames.add(recipientName);
						recipientEmails.add(signBook.getRecipientEmails().get(0));
					}
					
				}
				jsonSignInfoMessage.setNextRecipientNames(recipientNames);
				jsonSignInfoMessage.setNextRecipientEmails(recipientEmails);
				
				return jsonSignInfoMessage;
			}
		} catch (NoResultException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	@ResponseBody
	@RequestMapping(value = "/delete-sign-request", method = RequestMethod.GET)
	public void deleteSignRequest(@RequestParam String signBookName, @RequestParam String fileToken, HttpServletResponse response, Model model) {
		if (signBookRepository.countByName(signBookName) > 0) {
			SignRequest signRequest = signRequestRepository.findByName(fileToken).get(0);
			SignBook signBook = signBookRepository.findByName(signBookName).get(0);
			signRequest.getOriginalSignBooks().remove(signBook);
			signBook.getSignRequests().remove(signRequest);
			signRequestRepository.delete(signRequest);
			signBookService.deleteSignBook(signBook);
		}
	}
	
	@RequestMapping(value = "/sign-by-token/{token}")
	public String signByToken(@PathVariable("token") String token, HttpServletRequest request) {
		return "redirect:/user/signrequests/sign-by-token/" + token;
	}
	
	@ResponseBody
	@RequestMapping(value = "/check-user-status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String checkUserStatus(@RequestParam String eppn, HttpServletResponse response, Model model) throws JsonProcessingException {
		if(userRepository.countByEppn(eppn) > 0) {
			User user = userRepository.findByEppn(eppn).get(0);
			return new ObjectMapper().writeValueAsString(userService.isUserReady(user));
		} else {
			return new ObjectMapper().writeValueAsString(false);
		}
	}

	@Transactional
	@RequestMapping(value = "/complete-sign-request", method = RequestMethod.POST)
	public ResponseEntity<Void> completeSignRequest(@RequestParam String signBookName, @RequestParam String name, HttpServletRequest httpServletRequest, HttpServletResponse response, Model model) {
		try {
			SignBook signBook = signBookRepository.findByName(signBookName).get(0);
			SignRequest signRequest = signRequestRepository.findByName(name).get(0);
			User user = getSystemUser();
			user.setIp(httpServletRequest.getRemoteAddr());
			if (signBook.getSignRequests().contains(signRequest) && signRequest.getStatus().equals(SignRequestStatus.signed)) {
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
		SignBook signBook = signBookRepository.findByName(signBookName).get(0);
		return String.valueOf(signBook.getSignRequests().stream().filter(s -> s.getStatus().equals(SignRequestStatus.signed)).count());
	}

	@Transactional
	@RequestMapping(value = "/move-sign-request", method = RequestMethod.POST)
	public ResponseEntity<Void> moveSignRequest(@RequestParam String signBookName, @RequestParam String name, HttpServletRequest httpServletRequest, HttpServletResponse response, Model model) {
		SignBook signBook = signBookRepository.findByName(signBookName).get(0);
		SignRequest signRequest = signRequestRepository.findByName(name).get(0);
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

	@RequestMapping(value = "/searchLdap")
	@ResponseBody
	public List<PersonLdap> searchLdap(@RequestParam(value = "searchString") String searchString, @RequestParam(required = false) String ldapTemplateName) {
		logger.debug("ldap search for : " + searchString);
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
		List<PersonLdap> ldapList = new ArrayList<PersonLdap>();
		if (ldapPersonService != null && !searchString.trim().isEmpty() && searchString.length() > 3) {
			List<PersonLdap> ldapSearchList = new ArrayList<PersonLdap>();
			ldapSearchList = ldapPersonService.search(searchString, ldapTemplateName);
			ldapList.addAll(ldapSearchList.stream().sorted(Comparator.comparing(PersonLdap::getDisplayName)).collect(Collectors.toList()));

		}
		return ldapList;
	}

	@RequestMapping(value = "/searchSignBook")
	@ResponseBody
	public List<PersonLdap> searchSignBook(@RequestParam(value = "searchString") String searchString, @RequestParam(required = false) String ldapTemplateName) {
		logger.debug("signBook search for : " + searchString);
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
		List<PersonLdap> ldapList = new ArrayList<PersonLdap>();
		if (ldapPersonService != null && !searchString.trim().isEmpty() && searchString.length() > 3) {
			List<PersonLdap> ldapSearchList = new ArrayList<PersonLdap>();
			ldapSearchList = ldapPersonService.search(searchString, ldapTemplateName);
			ldapList.addAll(ldapSearchList.stream().sorted(Comparator.comparing(PersonLdap::getDisplayName)).collect(Collectors.toList()));

		}

		List<SignBook> signBooks = new ArrayList<>();
		signBooks.addAll(signBookRepository.findBySignBookType(SignBookType.system));
		signBooks.addAll(signBookRepository.findBySignBookType(SignBookType.workflow));
		signBooks.addAll(signBookRepository.findBySignBookType(SignBookType.group));

		for (SignBook signBook : signBooks) {
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
