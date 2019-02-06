package org.esupportail.esupsignature.web.user;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
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
import org.esupportail.esupsignature.domain.SignBook.NewPageType;
import org.esupportail.esupsignature.domain.SignBook.SignType;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.PdfService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RequestMapping("/user/signrequests")
@Controller
@Transactional
@Scope(value = "session")
public class SignRequestController {

	private static final Logger log = LoggerFactory.getLogger(SignRequestController.class);

	@ModelAttribute("active")
	public String getActiveMenu() {
		return "user/signrequests";
	}

	private String password = "";
	long startTime;

	@Resource
	private SignRequestService signRequestService;

	@Resource
	private PdfService pdfService;

	@Resource
	private SignBookService signBookService;

	@Resource
	private DocumentService documentService;

	@Resource
	private FileService fileService;

	@Resource
	private UserService userService;

	@RequestMapping(params = "form", produces = "text/html")
	public String createForm(Model uiModel,
			@RequestParam(value = "recipientEmail", required = false) boolean recipientEmail) {
		populateEditForm(uiModel, new SignRequest());
		uiModel.addAttribute("recipientEmail", recipientEmail);
		return "user/signrequests/create";
	}

	@RequestMapping(produces = "text/html")
	public String list(@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "findBy", required = false) String findBy,
			@RequestParam(value = "size", required = false) Integer size,
			@RequestParam(value = "sortFieldName", required = false) String sortFieldName,
			@RequestParam(value = "sortOrder", required = false) String sortOrder, Model uiModel) {
		String eppn = userService.getEppnFromAuthentication();
		if (User.countFindUsersByEppnEquals(eppn) == 0) {
			return "redirect:/user/users/?form";
		}
		User user = User.findUsersByEppnEquals(eppn).getSingleResult();
		if (user.getSignImage().getBigFile().getBinaryFile() == null) {
			return "redirect:/user/users/?form";
		}
		populateEditForm(uiModel, new SignRequest());
		if (User.countFindUsersByEppnEquals(eppn) == 0) {
			return "redirect:/user/users";
		}
		if (sortOrder == null) {
			sortOrder = "desc";
			sortFieldName = "createDate";
		}
		List<SignRequest> signRequests = null;
		if (findBy != null && findBy.equals("recipientEmail")) {
			signRequests = SignRequest.findSignRequests("", user.getEmail(), SignRequestStatus.pending, "", page, size,
					sortFieldName, sortOrder).getResultList();
			uiModel.addAttribute("tosigndocs", "active");
		} else {
			signRequests = SignRequest.findSignRequests(eppn, "", null, "", page, size, sortFieldName, sortOrder)
					.getResultList();
			uiModel.addAttribute("mydocs", "active");
		}
		uiModel.addAttribute("nbToSignRequests",
				SignRequest.countFindSignRequests("", user.getEmail(), SignRequestStatus.pending, ""));
		uiModel.addAttribute("nbPedingSignRequests",
				SignRequest.countFindSignRequests(user.getEppn(), "", SignRequestStatus.pending, ""));
		uiModel.addAttribute("signRequests", signRequests);
		uiModel.addAttribute("maxPages", (int) 1);
		return "user/signrequests/list";
	}

	@RequestMapping(value = "/{id}", produces = "text/html")
	public String show(@PathVariable("id") Long id, Model uiModel, RedirectAttributes redirectAttrs) throws SQLException, IOException, Exception {
		String eppn = userService.getEppnFromAuthentication();
		User user = User.findUsersByEppnEquals(eppn).getSingleResult();
		addDateTimeFormatPatterns(uiModel);
		SignRequest signRequest = SignRequest.findSignRequest(id);
		if (signRequestService.checkUserViewRights(user, signRequest)) {
			Document toConvertFile;
			if (signRequest.getStatus().equals(SignRequestStatus.signed)) {
				toConvertFile = signRequest.getSignedFile();
			} else {
				toConvertFile = signRequest.getOriginalFile();
			}
			uiModel.addAttribute("signBooks", SignBook.findAllSignBooks());
			if (SignBook.findSignBook(signRequest.getSignBookId()) != null) {
				uiModel.addAttribute("inSignBookName", SignBook.findSignBook(signRequest.getSignBookId()).getName());
			}
			uiModel.addAttribute("logs", Log.findLogsBySignRequestIdEquals(signRequest.getId()).getResultList());
			uiModel.addAttribute("signFile", fileService.getBase64Image(user.getSignImage()));
			uiModel.addAttribute("keystore", user.getKeystore().getFileName());
			uiModel.addAttribute("signRequest", signRequest);
			uiModel.addAttribute("itemId", id);
			uiModel.addAttribute("imagePagesSize",
					PdfService.getTotalNumberOfPages(toConvertFile.getBigFile().toJavaIoFile()));
			uiModel.addAttribute("documentId", toConvertFile.getId());
			if (signRequestService.checkUserSignRights(user, signRequest)) {
				uiModel.addAttribute("signable", "ok");
			}
			return "user/signrequests/show";
		} else {
			log.warn(eppn + " attempted to access signRequest " + id + " without write access");
			redirectAttrs.addFlashAttribute("messageCustom", "not autorized");
			return "redirect:/user/signrequests/";
		}
	}

	@RequestMapping(method = RequestMethod.POST, produces = "text/html")
	public String create(@Valid SignRequest signRequest, @RequestParam("multipartFile") MultipartFile multipartFile,
			BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest,
			HttpServletRequest request) {
		if (bindingResult.hasErrors()) {
			uiModel.addAttribute("signRequest", signRequest);
			uiModel.addAttribute("recipientEmail", false);
			return "user/signrequests/create";
		}
		uiModel.asMap().clear();
		String eppn = userService.getEppnFromAuthentication();
		User user = User.findUsersByEppnEquals(eppn).getSingleResult();
		user.setIp(request.getRemoteAddr());
		Map<String, String> params = new HashMap<>();
		params.put("signType", signRequest.getSignType());
		params.put("newPageType", signRequest.getNewPageType());
		params.put("signPageNumber", "1");
		params.put("xPos", "0");
		params.put("yPos", "0");

		try {
			Document document = documentService.addFile(multipartFile, multipartFile.getOriginalFilename());
			signRequest = signRequestService.createSignRequest(user, document, params, signRequest.getRecipientEmail());

		} catch (IOException e) {
			log.error("error to add file : " + multipartFile.getOriginalFilename(), e);
		}

		return "redirect:/user/signrequests/" + signRequest.getId();
	}

	@RequestMapping(value = "/sign/{id}", method = RequestMethod.POST)
	public String sign(@PathVariable("id") Long id, @RequestParam(value = "xPos", required = true) int xPos,
			@RequestParam(value = "yPos", required = true) int yPos,
			@RequestParam(value = "signPageNumber", required = true) int signPageNumber,
			@RequestParam(value = "password", required = false) String password, RedirectAttributes redirectAttrs,
			HttpServletResponse response, Model model, HttpServletRequest request) {
		String eppn = userService.getEppnFromAuthentication();
		User user = User.findUsersByEppnEquals(eppn).getSingleResult();
		user.setIp(request.getRemoteAddr());
		SignRequest signRequest = SignRequest.findSignRequest(id);
		if (signRequestService.checkUserSignRights(user, signRequest)) {
			Map<String, String> params = signRequest.getParams();
			params.put("signPageNumber", String.valueOf(signPageNumber));
			params.put("xPos", String.valueOf(xPos));
			params.put("yPos", String.valueOf(yPos));
			signRequest.setParams(params);
			signRequest.merge();
			if (!"".equals(password))
				this.password = password;
			try {
				SignType signType = SignType.valueOf(signRequest.getParams().get("signType"));
				if(signType.equals(SignType.validate)) {
					signRequestService.updateInfo(signRequest, SignRequestStatus.checked, "validate", user, "SUCCESS");		
				} else 
				if(signType.equals(SignType.nexuPAdES)) {
					return "redirect:/user/nexu-sign/" + id;
				} else {
					signRequestService.sign(signRequest, user, this.password);
				}
			} catch (EsupSignatureKeystoreException e) {
				log.error("keystore error", e);
				redirectAttrs.addFlashAttribute("messageCustom", "bad password");
			} catch (EsupSignatureIOException e) {
				log.error(e.getMessage(), e);
			}
			return "redirect:/user/signrequests/" + id;
		} else {
			redirectAttrs.addFlashAttribute("messageCustom", "not autorized");
			return "redirect:/user/signrequests/";
		}
	}

	@RequestMapping(value = "/refuse/{id}")
	public String refuse(@PathVariable("id") Long id, RedirectAttributes redirectAttrs, HttpServletResponse response,
			Model model, HttpServletRequest request) throws SQLException {
		String eppn = userService.getEppnFromAuthentication();
		User user = User.findUsersByEppnEquals(eppn).getSingleResult();
		user.setIp(request.getRemoteAddr());
		SignRequest signRequest = SignRequest.findSignRequest(id);

		if ((signRequest.getCreateBy().equals(user.getEppn()) && signRequest.getRecipientEmail() != null)
				|| (signRequest.getRecipientEmail() != null
						&& !signRequest.getRecipientEmail().equals(user.getEmail()))) {
			redirectAttrs.addFlashAttribute("messageCustom", "not autorized");
			return "redirect:/user/signrequests/" + id;
		}
		if (signRequest.getSignBookId() != 0) {
			SignBook signBook = SignBook.findSignBook(signRequest.getSignBookId());
			try {
				signBookService.removeSignRequestFromSignBook(signRequest, signBook, user);
			} catch (EsupSignatureException e) {
				log.warn(e.getMessage(), e);
			}
		}
		signRequestService.updateInfo(signRequest, SignRequestStatus.refused, "documentRefused", user, "SUCCESS");
		return "redirect:/user/signrequests/";
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
	public String delete(@PathVariable("id") Long id, @RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size, Model uiModel) {
		SignRequest signRequest = SignRequest.findSignRequest(id);
		SignBook signBook = SignBook.findSignBook(signRequest.getSignBookId());
		if (signBook != null) {
			signBook.getSignRequests().remove(signRequest);
			signBook.merge();
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
		Document file = signRequest.getOriginalFile();
		try {
			response.setHeader("Content-Disposition", "inline;filename=\"" + file.getFileName() + "\"");
			response.setContentType(file.getContentType());
			IOUtils.copy(file.getBigFile().getBinaryFile().getBinaryStream(), response.getOutputStream());
		} catch (Exception e) {
			log.error("get file error", e);
		}
	}

	@RequestMapping(value = "/get-signed-file/{id}", method = RequestMethod.GET)
	public void getSignedFile(@PathVariable("id") Long id, HttpServletResponse response, Model model) {
		SignRequest signRequest = SignRequest.findSignRequest(id);
		Document file = signRequest.getSignedFile();
		try {
			response.setHeader("Content-Disposition", "inline;filename=\"" + file.getFileName() + "\"");
			response.setContentType(file.getContentType());
			IOUtils.copy(file.getBigFile().getBinaryFile().getBinaryStream(), response.getOutputStream());
		} catch (Exception e) {
			log.error("get file error", e);
		}
	}

	@RequestMapping(value = "/send-to-signbook/{id}", method = RequestMethod.GET)
	public String sendToSignBook(@PathVariable("id") Long id,
			@RequestParam(value = "signBookId", required = false) long signBookId, HttpServletResponse response,
			RedirectAttributes redirectAttrs, Model model, HttpServletRequest request) {
		String eppn = userService.getEppnFromAuthentication();
		User user = User.findUsersByEppnEquals(eppn).getSingleResult();
		user.setIp(request.getRemoteAddr());
		SignRequest signRequest = SignRequest.findSignRequest(id);
		SignBook signBook = SignBook.findSignBook(signBookId);
		try {
			signBookService.importSignRequestInSignBook(signRequest, signBook, user);
		} catch (EsupSignatureException e) {
			log.warn(e.getMessage());
			redirectAttrs.addFlashAttribute("messageCustom", e.getMessage());

		}
		return "redirect:/user/signrequests/" + id;
	}

	void populateEditForm(Model uiModel, SignRequest signRequest) {
		uiModel.addAttribute("signRequest", signRequest);
		addDateTimeFormatPatterns(uiModel);
		uiModel.addAttribute("files", Document.findAllDocuments());
		uiModel.addAttribute("signTypes", Arrays.asList(SignType.values()));
		uiModel.addAttribute("newPageTypes", Arrays.asList(NewPageType.values()));
	}

	void addDateTimeFormatPatterns(Model uiModel) {
		uiModel.addAttribute("signRequest_createdate_date_format", "dd/MM/yyyy");
		uiModel.addAttribute("signRequest_updatedate_date_format", "dd/MM/yyyy");
	}

	@Scheduled(fixedDelay = 5000)
	public void clearPassword() {
		if (startTime > 0) {
			if (System.currentTimeMillis() - startTime > 300000) {
				password = "";
				startTime = 0;
			}
		}
	}
}
