package org.esupportail.esupsignature.web.user;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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
	
	private String progress = "0";
	private String password = "";
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

	@Resource
	private UserService userService;

	@RequestMapping(produces = "text/html")
	public String list(@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "findBy", required = false) String findBy,
			@RequestParam(value = "statusFilter", required = false) String statusFilter,
			@RequestParam(value = "signBookId", required = false) Long signBookId,
			@RequestParam(value = "size", required = false) Integer size,
			@RequestParam(value = "sortFieldName", required = false) String sortFieldName,
			@RequestParam(value = "sortOrder", required = false) String sortOrder, Model uiModel) {
		SignRequestStatus statusFilterEnum = null;
		if(statusFilter != null && !statusFilter.isEmpty()) {
			statusFilterEnum = SignRequestStatus.valueOf(statusFilter);
		}
		User user = userService.getEppnFromAuthentication();
		if (user == null) {
			return "redirect:/user/users/?form";
		}
		if (user.getSignImage().getBigFile().getBinaryFile() == null) {
			return "redirect:/user/users/?form";
		}
		populateEditForm(uiModel, new SignRequest());

		if (sortOrder == null) {
			sortOrder = "desc";
			sortFieldName = "createDate";
		}
		List<SignRequest> signRequests = null;
		float nrOfPages = 1;
		int sizeNo = size == null ? 10 : size.intValue();
		signBookId = null;
		if(findBy != null && findBy.equals("recipientEmail")) {
		signRequests = SignRequest.findSignRequests("", user.getEmail(), statusFilterEnum, "", page, size, sortFieldName, sortOrder)
				.getResultList();
		nrOfPages = (float) SignRequest.countFindSignRequests(user.getEppn(), statusFilterEnum, "") / sizeNo;
		} else {
			signRequests = SignRequest.findSignRequests(user.getEppn(), user.getEmail(), statusFilterEnum, "", page, size, sortFieldName, sortOrder)
					.getResultList();
			nrOfPages = (float) SignRequest.countFindSignRequests(user.getEppn(), statusFilterEnum, "") / sizeNo;
			
		}
		uiModel.addAttribute("mydocs", "active");

		uiModel.addAttribute("maxPages", (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages));
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);
		uiModel.addAttribute("findBy", findBy);
		uiModel.addAttribute("signBookId", signBookId);
		//TODO repair dectect nb to sign
		uiModel.addAttribute("nbToSignRequests",SignRequest.countFindSignRequests(user.getEppn(), SignRequestStatus.pending, ""));
		uiModel.addAttribute("nbPedingSignRequests",SignRequest.countFindSignRequests(user.getEppn(), SignRequestStatus.pending, ""));
		uiModel.addAttribute("signRequests", signRequests);
		uiModel.addAttribute("signBooks", SignBook.findSignBooksByRecipientEmailEquals(user.getEmail()).getResultList());
		uiModel.addAttribute("statusFilter", statusFilter);
		uiModel.addAttribute("statuses", SignRequest.SignRequestStatus.values());
		uiModel.addAttribute("queryUrl", "?findBy="+findBy);
		return "user/signrequests/list";
	}

	@RequestMapping(value = "/{id}", produces = "text/html")
	public String show(@PathVariable("id") Long id, Model uiModel, RedirectAttributes redirectAttrs) throws SQLException, IOException, Exception {
		User user = userService.getEppnFromAuthentication();
		addDateTimeFormatPatterns(uiModel);
		SignRequest signRequest = SignRequest.findSignRequest(id);
		if (signRequestService.checkUserViewRights(user, signRequest)) {
			uiModel.addAttribute("signBooks", SignBook.findAllSignBooks());
			SignBook signBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
			uiModel.addAttribute("curentSignBook", signBook);
			if(signBook == null) {
				signBook = SignBook.findSignBook(signRequest.getSignBooks().keySet().iterator().next());
			}
			if(!signRequest.isOverloadSignParams()) {
				signRequest.setSignRequestParams(signBook.getSignRequestParams());
			}
			uiModel.addAttribute("documents", signRequest.getDocuments());
			Document toDisplayDocument = signRequestService.getLastDocument(signRequest);
			File toDisplayFile = toDisplayDocument.getJavaIoFile();
			if(toDisplayDocument.getContentType().equals("application/pdf")) {
				PdfParameters pdfParameters = pdfService.getPdfParameters(toDisplayFile);
				uiModel.addAttribute("pdfWidth", pdfParameters.getWidth());
				uiModel.addAttribute("pdfHeight", pdfParameters.getHeight());
				uiModel.addAttribute("imagePagesSize", pdfParameters.getTotalNumberOfPages());
			}
			uiModel.addAttribute("logs", Log.findLogsBySignRequestIdEquals(signRequest.getId()).getResultList());
			uiModel.addAttribute("signFile", fileService.getBase64Image(user.getSignImage()));
			uiModel.addAttribute("keystore", user.getKeystore().getFileName());
			uiModel.addAttribute("signRequest", signRequest);
			uiModel.addAttribute("documentType", fileService.getExtension(toDisplayFile));
			uiModel.addAttribute("itemId", id);
			uiModel.addAttribute("documentId", toDisplayDocument.getId());
			if (signRequestService.checkUserSignRights(user, signRequest)) {
				uiModel.addAttribute("signable", "ok");
			}
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
		User user = userService.getEppnFromAuthentication();
		uiModel.addAttribute("mySignBook", SignBook.findSignBooksByRecipientEmailAndSignBookTypeEquals(user.getEmail(), SignBookType.user).getSingleResult());
		uiModel.addAttribute("allSignBooks", SignBook.findAllSignBooks("name", "ASC"));
		//TODO autocompletion signbooks
		return "user/signrequests/create";
	}

	@RequestMapping(method = RequestMethod.POST, produces = "text/html")
	public String create(@Valid SignRequest signRequest, @RequestParam("multipartFile") MultipartFile multipartFile,
			BindingResult bindingResult, @RequestParam(value = "signType", required = false) String signType, @RequestParam(value = "signBookIds", required = true) long[] signBookIds, @RequestParam(value="newPageType", required = false) String newPageType, Model uiModel, HttpServletRequest httpServletRequest,
			HttpServletRequest request) {
		if (bindingResult.hasErrors()) {
			uiModel.addAttribute("signRequest", signRequest);
			uiModel.addAttribute("recipientEmail", false);
			return "user/signrequests/create";
		}
		uiModel.asMap().clear();
		User user = userService.getEppnFromAuthentication();
		user.setIp(request.getRemoteAddr());
		SignRequestParams signRequestParams = new SignRequestParams();
		if(signRequest.isOverloadSignParams()) {
			signRequestParams.setSignType(SignType.valueOf(signType));
			signRequestParams.setNewPageType(NewPageType.valueOf(newPageType));
		}
		signRequestParams.setSignPageNumber(1);
		signRequestParams.setXPos(0);
		signRequestParams.setYPos(0);
		signRequestParams.persist();
		try {
			Document document = documentService.addFile(multipartFile, multipartFile.getOriginalFilename());
			signRequest = signRequestService.createSignRequest(signRequest, user, document, signRequestParams, signBookIds);

		} catch (IOException e) {
			logger.error("error to add file : " + multipartFile.getOriginalFilename(), e);
		}

		return "redirect:/user/signrequests/" + signRequest.getId();
	}

	@RequestMapping(value = "/sign/{id}", method = RequestMethod.POST)
	public String sign(@PathVariable("id") Long id, 
			@RequestParam(value = "xPos", required = true) int xPos,
			@RequestParam(value = "yPos", required = true) int yPos,
			@RequestParam(value = "signPageNumber", required = true) int signPageNumber,
			@RequestParam(value = "password", required = false) String password, RedirectAttributes redirectAttrs,
			HttpServletResponse response, Model model, HttpServletRequest request) {
		User user = userService.getEppnFromAuthentication();
		user.setIp(request.getRemoteAddr());
		SignRequest signRequest = SignRequest.findSignRequest(id);
		if (signRequestService.checkUserSignRights(user, signRequest)) {
			signRequest.getSignRequestParams().setSignPageNumber(signPageNumber);
			signRequest.getSignRequestParams().setXPos(xPos);
			signRequest.getSignRequestParams().setYPos(yPos);
			signRequest.merge();
			if (!"".equals(password)) {
	        	setPassword(password);
			}
			try {
				SignType signType = signRequest.getSignRequestParams().getSignType();
				if(signType.equals(SignType.validate)) {
					signRequestService.validate(signRequest, user);
				} else 
				if(signType.equals(SignType.nexuSign)) {
					return "redirect:/user/nexu-sign/" + id;
				} else {
					signRequestService.sign(signRequest, user, this.password);
				}
			} catch (EsupSignatureKeystoreException e) {
				logger.error("keystore error", e);
				redirectAttrs.addFlashAttribute("messageError", "security_bad_password");
			} catch (EsupSignatureIOException e) {
				logger.error(e.getMessage(), e);
			} catch (EsupSignatureException e) {
				logger.error(e.getMessage(), e);
			}
			return "redirect:/user/signrequests/" + id;
		} else {
			redirectAttrs.addFlashAttribute("messageCustom", "not autorized");
			return "redirect:/user/signrequests/";
		}
	}
	
	@RequestMapping(value = "/sign-multiple", method = RequestMethod.POST)
	public void signMultiple(
			@RequestParam(value = "ids", required = true) Long[] ids,
			@RequestParam(value = "password", required = false) String password, RedirectAttributes redirectAttrs,
			HttpServletResponse response, Model model, HttpServletRequest request) throws JsonParseException, JsonMappingException, IOException {
		User user = userService.getEppnFromAuthentication();
		user.setIp(request.getRemoteAddr());
		float totalToSign = ids.length;
		float nbSigned = 0;
		progress = "0";
		for(Long id : ids){
			SignRequest signRequest = SignRequest.findSignRequest(id);
			if (signRequestService.checkUserSignRights(user, signRequest)) {
				if (!"".equals(password)) {
		        	setPassword(password);
				}
				try {
					SignRequestParams.SignType signType = signRequest.getSignRequestParams().getSignType();
					if(signType.equals(SignRequestParams.SignType.validate)) {
						signRequestService.updateInfo(signRequest, SignRequestStatus.checked, "validate", user, "SUCCESS");		
					} else 
					if(signType.equals(SignRequestParams.SignType.nexuSign)) {
						logger.error("no multiple nexu sign");
						progress = "not_autorized";
					} else {
						signRequestService.sign(signRequest, user, this.password);
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
			System.err.println(progress);
		}
	}
	
	@ResponseBody
	@RequestMapping(value = "/get-progress")
	public String getProgress(RedirectAttributes redirectAttrs, HttpServletResponse response,
			Model model, HttpServletRequest request) {
		logger.debug("getProgress : " + progress);
		return progress;
	}
	
	@RequestMapping(value = "/refuse/{id}")
	public String refuse(@PathVariable("id") Long id, RedirectAttributes redirectAttrs, HttpServletResponse response,
			Model model, HttpServletRequest request) throws SQLException {
		User user = userService.getEppnFromAuthentication();
		user.setIp(request.getRemoteAddr());
		SignRequest signRequest = SignRequest.findSignRequest(id);
		if (!signRequestService.checkUserSignRights(user, signRequest)) {
			redirectAttrs.addFlashAttribute("messageCustom", "not autorized");
			return "redirect:/user/signrequests/" + id;
		}
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
		signRequest.remove();
		uiModel.asMap().clear();
		uiModel.addAttribute("page", (page == null) ? "1" : page.toString());
		uiModel.addAttribute("size", (size == null) ? "10" : size.toString());
		return "redirect:/user/signrequests/";
	}

	@RequestMapping(value = "/get-original-file/{id}", method = RequestMethod.GET)
	public void getOriginalFile(@PathVariable("id") Long id, HttpServletResponse response, Model model) {
		SignRequest signRequest = SignRequest.findSignRequest(id);
		Document document = signRequestService.getPreviousDocument(signRequest);
		try {
			response.setHeader("Content-Disposition", "inline;filename=\"" + document.getFileName() + "\"");
			response.setContentType(document.getContentType());
			IOUtils.copy(document.getBigFile().getBinaryFile().getBinaryStream(), response.getOutputStream());
		} catch (Exception e) {
			logger.error("get file error", e);
		}
	}

	@RequestMapping(value = "/get-signed-file/{id}", method = RequestMethod.GET)
	public void getSignedFile(@PathVariable("id") Long id, HttpServletResponse response, Model model) {
		SignRequest signRequest = SignRequest.findSignRequest(id);
		if(signRequest.getStatus().equals(SignRequestStatus.signed) || signRequest.getStatus().equals(SignRequestStatus.completed)) {
			Document document = signRequestService.getLastDocument(signRequest);
			try {
				response.setHeader("Content-Disposition", "inline;filename=\"" + document.getFileName() + "\"");
				response.setContentType(document.getContentType());				
				IOUtils.copy(document.getBigFile().getBinaryFile().getBinaryStream(), response.getOutputStream());
			} catch (Exception e) {
				logger.error("get file error", e);
			}
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
			@RequestParam(value = "signBookId", required = false) long signBookId, HttpServletResponse response,
			RedirectAttributes redirectAttrs, Model model, HttpServletRequest request) {
		User user = userService.getEppnFromAuthentication();
		user.setIp(request.getRemoteAddr());
		SignRequest signRequest = SignRequest.findSignRequest(id);
		SignBook signBook = SignBook.findSignBook(signBookId);
		try {
			signBookService.importSignRequestInSignBook(signRequest, signBook, user);
		} catch (EsupSignatureException e) {
			logger.warn(e.getMessage());
			redirectAttrs.addFlashAttribute("messageCustom", e.getMessage());

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
		uiModel.addAttribute("signRequest_createdate_date_format", "dd/MM/yyyy");
		uiModel.addAttribute("signRequest_updatedate_date_format", "dd/MM/yyyy");
	}

	@Scheduled(fixedDelay = 5000)
	public void clearPassword() {
		if (startTime > 0) {
			if (System.currentTimeMillis() - startTime > passwordTimeout) {
				password = "";
				startTime = 0;
			}
		}
	}
}
