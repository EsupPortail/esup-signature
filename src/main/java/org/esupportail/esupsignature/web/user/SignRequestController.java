	package org.esupportail.esupsignature.web.user;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.Log;
import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignBook.SignBookType;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.domain.SignRequestParams;
import org.esupportail.esupsignature.domain.SignRequestParams.NewPageType;
import org.esupportail.esupsignature.domain.SignRequestParams.SignType;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.esupportail.esupsignature.exception.EsupSignatureNexuException;
import org.esupportail.esupsignature.exception.EsupSignatureSignException;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.pdf.PdfParameters;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

@CrossOrigin(origins = "*")
@RequestMapping("/user/signrequests")
@Controller
@Transactional
@Scope(value = "session")
public class SignRequestController {

	private static final Logger logger = LoggerFactory.getLogger(SignRequestController.class);

	@ModelAttribute("active")
	public String getActiveMenu() {
		return "user/signrequests";
	}

	@Value("${sign.passwordTimeout}")
	private long passwordTimeout;

	@Resource
	private UserService userService;
	
	@Resource
	private ReloadableResourceBundleMessageSource messageSource;
	
	@ModelAttribute("user")
	public User getUser() {
		return userService.getUserFromAuthentication();
	}
	
	private String progress = "0";
	
	private String password;
	
	long startTime;

	public void setPassword(String password) {
		startTime = System.currentTimeMillis();
		this.password = password;
	}

	@Resource
	private SignRequestService signRequestService;

	@Resource
	private SignBookService signBookService;

	@Resource
	private DocumentService documentService;

	@Resource
	private PdfService pdfService;
	
	@Resource
	private FileService fileService;

	@RequestMapping(produces = "text/html")
	public String list(
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "toSign", required = false) Boolean toSign,
			@RequestParam(value = "statusFilter", required = false) String statusFilter,
			@RequestParam(value = "signBookId", required = false) Long signBookId,
			@RequestParam(value = "size", required = false) Integer size,
			@RequestParam(value = "sortFieldName", required = false) String sortFieldName,
			@RequestParam(value = "sortOrder", required = false) String sortOrder, Model uiModel) {
		SignRequestStatus statusFilterEnum = null;

		if(page == null) {
			page = 1;
		}
		if(size == null) {
			size = 10;
		}
		if(statusFilter != null) {
			if(!statusFilter.isEmpty()) {
				statusFilterEnum = SignRequestStatus.valueOf(statusFilter);
			}
		} 
		User user = userService.getUserFromAuthentication();
		if (user == null || !user.isReady()) {
			return "redirect:/user/users/?form";
		}
		populateEditForm(uiModel, new SignRequest());

		if (sortOrder == null) {
			sortOrder = "desc";
			sortFieldName = "createDate";
		}
		List<SignRequest> signRequests = new ArrayList<>();
		float nrOfPages = 1;
		int sizeNo = size == null ? 10 : size.intValue();
		List<String> recipientEmails = new ArrayList<>();
		recipientEmails.add(user.getEmail());
		
		List<SignBook> signBooks = SignBook.findSignBooksBySignBookTypeEquals(SignBookType.group).getResultList();
		if(signBookId != null) {	
			signRequests.addAll(SignBook.findSignBook(signBookId).getSignRequests());
		} else {
		if(toSign != null) {
			if(toSign) {
				signRequests = signRequestService.findSignRequestByUserAndStatusEquals(user, toSign, SignRequestStatus.pending, page, size);
				signRequests = signRequests.stream().sorted(Comparator.comparing(SignRequest::getCreateDate).reversed()).collect(Collectors.toList());
				nrOfPages = (float) signRequestService.findSignRequestByUserAndStatusEquals(user, statusFilterEnum).size() / sizeNo;
			} else {
				signRequests = SignRequest.findSignRequests(user.getEppn(), statusFilterEnum, "", page, size, sortFieldName, sortOrder)
						.getResultList();
				nrOfPages = (float) SignRequest.countFindSignRequests(user.getEppn(), statusFilterEnum, "") / sizeNo;
				
			}
		} else {
			signRequests = signRequestService.findSignRequestByUserAndStatusEquals(user, false, null, page, size);
		}
		}
		uiModel.addAttribute("mydocs", "active");
		uiModel.addAttribute("maxPages", (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages));
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);
		uiModel.addAttribute("toSign", toSign);
		uiModel.addAttribute("signBookId", signBookId);
		uiModel.addAttribute("nbToSignRequests",signRequestService.findSignRequestByUserAndStatusEquals(user, SignRequestStatus.pending).size());
		uiModel.addAttribute("nbPedingSignRequests", SignRequest.countFindSignRequestsByCreateByAndStatusEquals(user.getEppn(), SignRequestStatus.pending));
		uiModel.addAttribute("signRequests", signRequests);
		uiModel.addAttribute("signBooks", signBooks);
		uiModel.addAttribute("statusFilter", statusFilterEnum);
		uiModel.addAttribute("statuses", SignRequest.SignRequestStatus.values());
		uiModel.addAttribute("queryUrl", "?toSign=" + toSign);
		return "user/signrequests/list";
	}

	@RequestMapping(value = "/{id}", produces = "text/html")
	public String show(@PathVariable("id") Long id, Model uiModel, RedirectAttributes redirectAttrs) throws SQLException, IOException, Exception {
		//TODO fill model if pdf and pdfform
		User user = userService.getUserFromAuthentication();
		if(!user.isReady()) {
			return "redirect:/user/users/?form";
		}
		
		addDateTimeFormatPatterns(uiModel);
		SignRequest signRequest = SignRequest.findSignRequest(id);
		if (signRequestService.checkUserViewRights(user, signRequest) || signRequestService.checkUserSignRights(user, signRequest)) {
			uiModel.addAttribute("signBooks", SignBook.findAllSignBooks());
			SignBook userSignBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
			if(userSignBook != null) {
				if(!signRequest.isOverloadSignBookParams()) {
					signRequest.setSignRequestParams(userSignBook.getSignRequestParams());
				}
			}

			Document toDisplayDocument = null;
			File toDisplayFile = null;
			
			if(signRequestService.getToSignDocuments(signRequest).size() == 1) {
				toDisplayDocument = signRequestService.getToSignDocuments(signRequest).get(0);
				toDisplayFile = toDisplayDocument.getJavaIoFile();
				if(toDisplayDocument.getContentType().equals("application/pdf")) {
					PdfParameters pdfParameters = pdfService.getPdfParameters(toDisplayFile);
					uiModel.addAttribute("pdfWidth", pdfParameters.getWidth());
					uiModel.addAttribute("pdfHeight", pdfParameters.getHeight());
					uiModel.addAttribute("imagePagesSize", pdfParameters.getTotalNumberOfPages());
					int[] pos = pdfService.getSignFieldCoord(toDisplayFile);
					if(pos != null) {
						signRequest.getSignRequestParams().setXPos(pos[0]);
						signRequest.getSignRequestParams().setYPos(pos[1]);
					}
					if(user.getSignImage() != null) {
						uiModel.addAttribute("signFile", fileService.getBase64Image(user.getSignImage()));
						int[] size = pdfService.getSignSize(user.getSignImage().getJavaIoFile());
						uiModel.addAttribute("signWidth", size[0]);
						uiModel.addAttribute("signHeight", size[1]);
					}
				}
				uiModel.addAttribute("documentType", fileService.getExtension(toDisplayFile));		
				uiModel.addAttribute("documentId", toDisplayDocument.getId());
			}
			List<Log> logs = Log.findLogsBySignRequestIdEquals(signRequest.getId()).getResultList();
			uiModel.addAttribute("logs", logs);
			uiModel.addAttribute("comments", logs.stream().filter(log -> log.getComment() != null && !log.getComment().isEmpty()).collect(Collectors.toList()));
			if(user.getSignImage() != null) {
				uiModel.addAttribute("signFile", fileService.getBase64Image(user.getSignImage()));
			}
			if(user.getKeystore() != null) {
				uiModel.addAttribute("keystore", user.getKeystore().getFileName());
			}
			uiModel.addAttribute("signRequest", signRequest);
			uiModel.addAttribute("itemId", id);
			if (signRequest.getStatus().equals(SignRequestStatus.pending) && signRequestService.checkUserSignRights(user, signRequest) && signRequest.getOriginalDocuments().size() > 0) {
				uiModel.addAttribute("signable", "ok");
			}
			SignBook firstOriginalSignBook = SignBook.findSignBooksByNameEquals(signRequest.getOriginalSignBookNames().get(0)).getSingleResult();
			if(firstOriginalSignBook.getSignBookType().equals(SignBookType.workflow)) {
				uiModel.addAttribute("originalSignBooks", firstOriginalSignBook);
			}
			if(firstOriginalSignBook.getModelFile() != null) {
				uiModel.addAttribute("modelId", firstOriginalSignBook.getModelFile().getUrl());
			}
			uiModel.addAttribute("allSignBooks", SignBook.findSignBooksBySignBookTypeEquals(SignBookType.group).getResultList());
			uiModel.addAttribute("nbSignOk", signRequest.countSignOk());
			return "user/signrequests/show";
		} else {
			logger.warn(user.getEppn() + " attempted to access signRequest " + id + " without write access");
			redirectAttrs.addFlashAttribute("messageCustom", "not autorized");
			return "redirect:/user/signrequests/";
		}
	}
	
	@RequestMapping(params = "form", produces = "text/html")
	public String createForm(Model uiModel) {
		populateEditForm(uiModel, new SignRequest());
		User user = userService.getUserFromAuthentication();
		uiModel.addAttribute("mySignBook", signBookService.getUserSignBook(user));
		uiModel.addAttribute("allSignBooks", SignBook.findAllSignBooks());
		return "user/signrequests/create";
	}

	@RequestMapping(method = RequestMethod.POST, produces = "text/html")
	public String create(@Valid SignRequest signRequest, BindingResult bindingResult, 
			@RequestParam(value = "signType", required = false) String signType, 
			@RequestParam(value = "signBookNames", required = false) String[] signBookNames,
			@RequestParam(value = "newPageType", required = false) String newPageType, 
			Model uiModel, HttpServletRequest httpServletRequest,
			HttpServletRequest request, RedirectAttributes redirectAttrs) throws EsupSignatureException {
		if (bindingResult.hasErrors()) {
			uiModel.addAttribute("signRequest", signRequest);
			return "user/signrequests/create";
		}

		uiModel.asMap().clear();
		User user = userService.getUserFromAuthentication();
		user.setIp(request.getRemoteAddr());
		SignRequestParams signRequestParams = null;
		if(signRequest.isOverloadSignBookParams()) {
			signRequestParams = new SignRequestParams();
			signRequestParams.setSignType(SignType.valueOf(signType));
			signRequestParams.setNewPageType(NewPageType.valueOf(newPageType));
			signRequestParams.setSignPageNumber(1);
			signRequestParams.persist();
		}
		//TODO si signbook type workflow == 1 seul
		signRequest = signRequestService.createSignRequest(signRequest, user, signRequestParams);
		List<SignBook> signBooks = new ArrayList<>();
		if(signBookNames != null && signBookNames.length > 0) {
			for(String signBookName : signBookNames) {
				if(SignBook.countFindSignBooksByNameEquals(signBookName) > 0) {
					signBooks.add(SignBook.findSignBooksByNameEquals(signBookName).getSingleResult());
				} else {
					signBooks.add(signBookService.getUserSignBookByRecipientEmail(signBookName));
				}
			}
		}
		//on traite les groups en premier
		signBooks = signBooks.stream().sorted(Comparator.comparing(SignBook::getSignBookType)).collect(Collectors.toList());

		for(SignBook signBook : signBooks) {
			signBookService.importSignRequestInSignBook(signRequest, signBook, user);			
		}
		
		return "redirect:/user/signrequests/" + signRequest.getId();
	}

	@RequestMapping(value = "/add-doc/{id}", method = RequestMethod.POST, produces = "text/html")
	@ResponseBody
	public String addDocument(@PathVariable("id") Long id,
			@RequestParam("multipartFiles") MultipartFile[] multipartFiles, HttpServletRequest request) {
		logger.info("start add documents");
		User user = userService.getUserFromAuthentication();
		user.setIp(request.getRemoteAddr());
		SignRequest signRequest = SignRequest.findSignRequest(id);
		if (signRequestService.checkUserViewRights(user, signRequest)) {
			try {
				if(signRequest.getOriginalDocuments().size() > 0 && 
				(signRequest.getSignRequestParams().getSignType().equals(SignType.pdfImageStamp) || signRequest.getSignRequestParams().getSignType().equals(SignType.visa))
				) {
					signRequest.getOriginalDocuments().remove(signRequest.getOriginalDocuments().get(0));
				}
				List<Document> documents =  documentService.createDocuments(multipartFiles);
				signRequestService.addOriginalDocuments(signRequest, documents);
				signRequest.merge();
			} catch (IOException e) {
				logger.error("error to add file : " + multipartFiles[0].getOriginalFilename(), e);
			}
		}
        return "{}";

	}

	@RequestMapping(value = "/remove-doc/{id}", method = RequestMethod.POST, produces = "text/html")
	@ResponseBody
	public String removeDocument(@PathVariable("id") Long id, HttpServletRequest request) {
		User user = userService.getUserFromAuthentication();
		user.setIp(request.getRemoteAddr());
		Document document = Document.findDocument(id);
		SignRequest signRequest = SignRequest.findSignRequest(document.getParentId());
		if (signRequestService.checkUserSignRights(user, signRequest)) {
			signRequest.getOriginalDocuments().remove(document);
		}
		return "{}";
	}
	
	@RequestMapping(value = "/sign/{id}", method = RequestMethod.POST)
	public String sign(@PathVariable("id") Long id, 
			@RequestParam(value = "xPos", required = false) Integer xPos,
			@RequestParam(value = "yPos", required = false) Integer yPos,
			@RequestParam(value = "comment", required = false) String comment,
			@RequestParam(value = "addDate", required = false) Boolean addDate,
			@RequestParam(value = "signPageNumber", required = false) Integer signPageNumber,
			@RequestParam(value = "password", required = false) String password, RedirectAttributes redirectAttrs,
			HttpServletResponse response, Model model, HttpServletRequest request) {
		//TODO : choose xades cades
		if(addDate == null) {
			addDate = false;
		}
		User user = userService.getUserFromAuthentication();
		user.setIp(request.getRemoteAddr());
		SignRequest signRequest = SignRequest.findSignRequest(id);
		if (signRequestService.checkUserSignRights(user, signRequest)) {
			if(!signRequest.isOverloadSignBookParams()) {
				SignBook signBook = SignBook.findSignBooksByRecipientEmailsAndSignBookTypeEquals(Arrays.asList(user.getEmail()), SignBookType.user).getSingleResult();
				signRequest.setSignRequestParams(signBook.getSignRequestParams());
				signRequest.merge();
			}
			if(signPageNumber != null) {
				signRequest.getSignRequestParams().setSignPageNumber(signPageNumber);
				signRequest.getSignRequestParams().setXPos(xPos);
				signRequest.getSignRequestParams().setYPos(yPos);
				signRequest.merge();
			}
			if (!"".equals(password)) {
	        	setPassword(password);
			}
			try {
				signRequest.setComment(comment);
				signRequestService.sign(signRequest, user, this.password, addDate);
				signRequest.merge();
			} catch (EsupSignatureKeystoreException e) {
				logger.error("keystore error", e);
				redirectAttrs.addFlashAttribute("messageError", "security_bad_password");
				progress = "security_bad_password";
			} catch (EsupSignatureIOException e) {
				logger.error(e.getMessage(), e);
			} catch (EsupSignatureSignException e) {
				logger.error(e.getMessage(), e);
			} catch (EsupSignatureNexuException e) {
				logger.info(e.getMessage());
				return "redirect:/user/nexu-sign/" + id;
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
			return "redirect:/user/signrequests/" + id;
		} else {
			redirectAttrs.addFlashAttribute("messageCustom", "not autorized");
			progress = "not_autorized";
			return "redirect:/user/signrequests/";
		}
	}
	
	@RequestMapping(value = "/sign-multiple", method = RequestMethod.POST)
	public void signMultiple(
			@RequestParam(value = "ids", required = true) Long[] ids,
			@RequestParam(value = "comment", required = false) String comment,
			@RequestParam(value = "password", required = false) String password, RedirectAttributes redirectAttrs,
			HttpServletResponse response, Model model, HttpServletRequest request) throws JsonParseException, JsonMappingException, IOException {
		User user = userService.getUserFromAuthentication();
		user.setIp(request.getRemoteAddr());
		float totalToSign = ids.length;
		float nbSigned = 0;
		progress = "0";
		for(Long id : ids){
			SignRequest signRequest = SignRequest.findSignRequest(id);
			SignBook currentSignBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
			if (signRequestService.checkUserSignRights(user, signRequest)) {
				if (!"".equals(password)) {
		        	setPassword(password);
				}
				try {
					if(!signRequest.isOverloadSignBookParams()) {
						signRequest.getSignRequestParams().setSignType(currentSignBook.getSignRequestParams().getSignType());
					}
					if(signRequest.getSignRequestParams().getSignType().equals(SignRequestParams.SignType.visa)) {
						signRequestService.updateStatus(signRequest, SignRequestStatus.checked, messageSource.getMessage("updateinfo_visa", null, Locale.FRENCH), user, "SUCCESS", comment);		
					} else 
					if(signRequest.getSignRequestParams().getSignType().equals(SignRequestParams.SignType.nexuSign)) {
						logger.error("no multiple nexu sign");
						progress = "not_autorized";
					} else {
						signRequestService.sign(signRequest, user, this.password, false);
					}
				} catch (EsupSignatureKeystoreException e) {
					logger.error("keystore error", e);
					progress = "security_bad_password";
					break;
				} catch (EsupSignatureIOException e) {
					logger.error(e.getMessage(), e);
				} catch (EsupSignatureException e) {
					logger.error(e.getMessage(), e);
				}
			} else {
				logger.error("not autorized to sign");
				progress = "not_autorized";
			}
			nbSigned++;
			float percent = (nbSigned / totalToSign) * 100;
			progress = String.valueOf((int) percent);
		}
	}
	
	@ResponseBody
	@RequestMapping(value = "/get-step")
	public String getStep(RedirectAttributes redirectAttrs, HttpServletResponse response,
			Model model, HttpServletRequest request) {
		logger.debug("getStep : " + signRequestService.getStep());
		return signRequestService.getStep();
	}
	
	@ResponseBody
	@RequestMapping(value = "/get-progress")
	public String getProgress(RedirectAttributes redirectAttrs, HttpServletResponse response,
			Model model, HttpServletRequest request) {
		logger.debug("getProgress : " + progress);
		return progress;
	}
	
	@RequestMapping(value = "/refuse/{id}")
	public String refuse(@PathVariable("id") Long id, @RequestParam(value = "comment", required = true) String comment, RedirectAttributes redirectAttrs, HttpServletResponse response,
			Model model, HttpServletRequest request) throws SQLException {
		User user = userService.getUserFromAuthentication();
		user.setIp(request.getRemoteAddr());
		SignRequest signRequest = SignRequest.findSignRequest(id);
		if (!signRequestService.checkUserSignRights(user, signRequest)) {
			redirectAttrs.addFlashAttribute("messageCustom", "not autorized");
			return "redirect:/user/signrequests/" + id;
		}
		signRequest.setComment(comment);
		signRequestService.refuse(signRequest, user);
		return "redirect:/user/signrequests/";
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
	public String delete(@PathVariable("id") Long id, @RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size, Model uiModel) {
		SignRequest signRequest = SignRequest.findSignRequest(id);
		for(Map.Entry<Long, Boolean> signBookId : signRequest.getSignBooks().entrySet()) {
			SignBook signBook = SignBook.findSignBook(signBookId.getKey());
			if (signBook != null) {
				signBook.getSignRequests().remove(signRequest);
				signBook.merge();
			}
		}
		List<Log> logs = Log.findLogsBySignRequestIdEquals(id).getResultList();
		for(Log log : logs) {
			log.remove();
		}
		signRequest.remove();
		uiModel.asMap().clear();
		uiModel.addAttribute("page", (page == null) ? "1" : page.toString());
		uiModel.addAttribute("size", (size == null) ? "10" : size.toString());
		return "redirect:/user/signrequests/";
	}

	@RequestMapping(value = "/get-last-file/{id}", method = RequestMethod.GET)
	public void getLastFile(@PathVariable("id") Long id, HttpServletResponse response, Model model) {
		SignRequest signRequest = SignRequest.findSignRequest(id);
		User user = userService.getUserFromAuthentication();
		if(signRequestService.checkUserViewRights(user, signRequest)) {
			Document document = signRequestService.getLastSignedDocument(signRequest);
			try {
				response.setHeader("Content-Disposition", "inline;filename=\"" + document.getFileName() + "\"");
				response.setContentType(document.getContentType());
				IOUtils.copy(document.getBigFile().getBinaryFile().getBinaryStream(), response.getOutputStream());
			} catch (Exception e) {
				logger.error("get file error", e);
			}
		} else {
			logger.warn(user.getEppn() + " try to access " + signRequest.getId() + " without view rights");
		}
	}

	@RequestMapping(value = "/toggle-need-all-sign/{id}", method = RequestMethod.GET)
	public String toggleNeedAllSign(@PathVariable("id") Long id, HttpServletResponse response, Model model) {
		SignRequest signRequest = SignRequest.findSignRequest(id);
		signRequestService.toggleNeedAllSign(signRequest);
		return "redirect:/user/signrequests/" + id;
	}
	
	@RequestMapping(value = "/send-to-signbook/{id}", method = RequestMethod.GET)
	public String sendToSignBook(@PathVariable("id") Long id,
			@RequestParam(value = "signBookNames", required = true) String[] signBookNames, 
			@RequestParam(value = "comment", required = false) String comment,
			HttpServletResponse response,
			RedirectAttributes redirectAttrs, Model model, HttpServletRequest request) {
		User user = userService.getUserFromAuthentication();
		user.setIp(request.getRemoteAddr());
		SignRequest signRequest = SignRequest.findSignRequest(id);
		if(signRequest.getCreateBy().equals(user.getEppn())) {
			if(signBookNames != null && signBookNames.length > 0) {
				for(String signBookName : signBookNames) {
					SignBook signBook;
					if(SignBook.countFindSignBooksByNameEquals(signBookName) == 0 && SignBook.countFindSignBooksByRecipientEmailsAndSignBookTypeEquals(Arrays.asList(signBookName), SignBookType.user) == 0) {
						signBook = userService.createUser(signBookName);
						//recipientEmails.add(signBookName);
					} else {
						if(SignBook.countFindSignBooksByNameEquals(signBookName) > 0) {
							signBook = SignBook.findSignBooksByNameEquals(signBookName).getSingleResult();
						} else {
							signBook = signBookService.getUserSignBookByRecipientEmail(signBookName);
						}
					}
					try {
						signBookService.importSignRequestInSignBook(signRequest, signBook, user);
						signRequestService.updateStatus(signRequest, SignRequestStatus.draft, messageSource.getMessage("updateinfo_sendtosignbook", null, Locale.FRENCH) + " " + signBook.getName(), user, "SUCCESS", comment);
					} catch (EsupSignatureException e) {
						logger.warn(e.getMessage());
						redirectAttrs.addFlashAttribute("messageCustom", e.getMessage());
			
					}
					
				}
			}
		} else {
			logger.warn(user.getEppn() + " try to move " + signRequest.getId() + " without rights");
		}
		return "redirect:/user/signrequests/" + id;
	}
	

	@RequestMapping(value = "/complete/{id}", method = RequestMethod.GET)
	public String complete(@PathVariable("id") Long id, 
			@RequestParam(value = "comment", required = false) String comment,
			HttpServletResponse response, RedirectAttributes redirectAttrs, Model model, HttpServletRequest request) throws EsupSignatureException {
		User user = userService.getUserFromAuthentication();
		user.setIp(request.getRemoteAddr());
		SignRequest signRequest = SignRequest.findSignRequest(id);
		if(signRequest.getCreateBy().equals(user.getEppn()) && (signRequest.getStatus().equals(SignRequestStatus.signed) || signRequest.getStatus().equals(SignRequestStatus.checked))) {
			SignBook originalSignBook = SignBook.findSignBooksByNameEquals(signRequest.getOriginalSignBookNames().get(0)).getSingleResult();
			signRequestService.completeSignRequest(signRequest, originalSignBook, user);
		} else {
			logger.warn(user.getEppn() + " try to complete " + signRequest.getId() + " without rights");
		}
		return "redirect:/user/signrequests/" + id;
	}

	@RequestMapping(value = "/pending/{id}", method = RequestMethod.GET)
	public String pending(@PathVariable("id") Long id, 
			@RequestParam(value = "comment", required = false) String comment,
			HttpServletResponse response, RedirectAttributes redirectAttrs, Model model, HttpServletRequest request) {
		User user = userService.getUserFromAuthentication();
		user.setIp(request.getRemoteAddr());
		SignRequest signRequest = SignRequest.findSignRequest(id);
		signRequest.setComment(comment);
		if(signRequestService.checkUserViewRights(user, signRequest) && signRequest.getStatus().equals(SignRequestStatus.draft)) {
			signRequestService.pendingSignRequest(signRequest, user);
		} else {
			logger.warn(user.getEppn() + " try to send for sign " + signRequest.getId() + " without rights");
		}
		return "redirect:/user/signrequests/" + id;
	}
	
	@RequestMapping(value = "/comment/{id}", method = RequestMethod.GET)
	public String comment(@PathVariable("id") Long id, 
			@RequestParam(value = "comment", required = false) String comment,
			HttpServletResponse response, RedirectAttributes redirectAttrs, Model model, HttpServletRequest request) {
		User user = userService.getUserFromAuthentication();
		user.setIp(request.getRemoteAddr());
		SignRequest signRequest = SignRequest.findSignRequest(id);
		if(signRequestService.checkUserViewRights(user, signRequest)) {
			signRequestService.updateStatus(signRequest, null, messageSource.getMessage("updateinfo_addcomment", null, Locale.FRENCH), user, "SUCCESS", comment);
		} else {
			logger.warn(user.getEppn() + " try to add comment" + signRequest.getId() + " without rights");
		}
		return "redirect:/user/signrequests/" + id;
	}
	
	void populateEditForm(Model uiModel, SignRequest signRequest) {
		uiModel.addAttribute("signRequest", signRequest);
		addDateTimeFormatPatterns(uiModel);
		uiModel.addAttribute("files", Document.findAllDocuments());
		uiModel.addAttribute("signTypes", Arrays.asList(SignRequestParams.SignType.values()));
		uiModel.addAttribute("newPageTypes", Arrays.asList(SignRequestParams.NewPageType.values()));
	}

	void addDateTimeFormatPatterns(Model uiModel) {
		uiModel.addAttribute("signRequest_createdate_date_format", "dd/MM/yyyy HH:mm");
		uiModel.addAttribute("signRequest_updatedate_date_format", "dd/MM/yyyy HH:mm");
	}

	@Scheduled(fixedDelay = 5000)
	public void clearPassword() {
		password = "";
		if (startTime > 0) {
			if (System.currentTimeMillis() - startTime > passwordTimeout) {
				password = "";
				startTime = 0;
			}
		}
	}
}
