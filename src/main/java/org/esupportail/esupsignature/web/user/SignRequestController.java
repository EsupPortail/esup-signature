package org.esupportail.esupsignature.web.user;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequest.NewPageType;
import org.esupportail.esupsignature.domain.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.domain.SignRequest.SignType;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.PdfService;
import org.esupportail.esupsignature.service.UserKeystoreService;
import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
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
@Scope(value="session")
public class SignRequestController {

	private static final Logger log = LoggerFactory.getLogger(SignRequestController.class);
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return "user/signrequests";
	}
	
	@Resource
	PdfService pdfService;
	
	@Resource
	DocumentService documentService;

	@Resource
	FileService fileService;
	
	@Resource
	UserKeystoreService userKeystoreService;

	@Resource
	UserService userService;
	
    @RequestMapping(params = "form", produces = "text/html")
    public String createForm(Model uiModel, @RequestParam(value = "recipientEmail", required = false) boolean recipientEmail) {
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
		populateEditForm(uiModel, new SignRequest());
		if(User.countFindUsersByEppnEquals(eppn) == 0) {
			return "redirect:/user/users";
		}
		if(sortOrder == null) {
			sortOrder = "asc";
			sortFieldName = "createDate";
		}
		User user = User.findUsersByEppnEquals(eppn).getSingleResult();
		List<SignRequest> signRequests = null;
		if(findBy != null && findBy.equals("recipientEmail")) {
			signRequests = SignRequest.findSignRequests("", user.getEmail(), "", page, size, sortFieldName, sortOrder).getResultList();
			uiModel.addAttribute("tosigndocs", "active");	
		} else {
			signRequests = SignRequest.findSignRequests(eppn, "", "", page, size, sortFieldName, sortOrder).getResultList();
			uiModel.addAttribute("mydocs", "active");
		}
		uiModel.addAttribute("signRequests", signRequests);
        uiModel.addAttribute("maxPages", (int) 1);

        return "user/signrequests/list";
    }
    
    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) throws SQLException, IOException, Exception {
		String eppn = userService.getEppnFromAuthentication();
    	User user = User.findUsersByEppnEquals(eppn).getSingleResult();
		addDateTimeFormatPatterns(uiModel);
        SignRequest signRequest = SignRequest.findSignRequest(id);
        if(signRequest.getCreateBy().equals(eppn) || signRequest.getRecipientEmail().equals(user.getEmail())) {
        	Document toConvertFile;
        	if(signRequest.getStatus().equals(SignRequestStatus.signed)) {
        		toConvertFile = signRequest.getSignedFile();
        	} else {
        		toConvertFile = signRequest.getOriginalFile();
        	}
        	uiModel.addAttribute("signFile", fileService.getBase64Image(user.getSignImage()));
        	uiModel.addAttribute("keystore", user.getKeystore().getFileName());
	        uiModel.addAttribute("signRequest", signRequest);
	        uiModel.addAttribute("itemId", id);
	        uiModel.addAttribute("imagePagesSize", pdfService.getTotalNumberOfPages(toConvertFile.getBigFile().toJavaIoFile()));
	        uiModel.addAttribute("documentId", toConvertFile.getId());
	    	if(signRequest.getCreateBy().equals(user.getEppn()) && signRequest.getRecipientEmail() != null) {
	    		uiModel.addAttribute("signable", "ko");
	    	} else {
	    		uiModel.addAttribute("signable", "ok");
	    	}
	        return "user/signrequests/show";
        } else {
        	return "redirect:/user/signrequests/";
        }
    }
    
	@RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(@Valid SignRequest signRequest, @RequestParam("multipartFile") MultipartFile multipartFile, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            uiModel.addAttribute("signRequest", signRequest);
            uiModel.addAttribute("recipientEmail", false);
            return "user/signrequests/create";
        }
		String eppn = userService.getEppnFromAuthentication();
        uiModel.asMap().clear();
        try {
        	signRequest.setCreateBy(eppn);
        	signRequest.setCreateDate(new Date());
			signRequest.setOriginalFile(documentService.addFile(multipartFile));
			signRequest.setSignedFile(null);
			signRequest.setStatus(SignRequestStatus.pending);
			Map<String, String> params = new HashMap<>();
			params.put("signType", signRequest.getSignType());
			params.put("newPageType", signRequest.getNewPageType());
			params.put("signPageNumber", "1");
			params.put("xPos", "0");
			params.put("yPos", "0");
			signRequest.setParams(params);
	        signRequest.persist();
        } catch (IOException e) {
        	log.error("Create file error", e);
		}
        return "redirect:/user/signrequests/" + signRequest.getId();
    }

    @RequestMapping(value = "/signdoc/{id}", method = RequestMethod.POST)
    public String signdoc(@PathVariable("id") Long id, 
    		@RequestParam(value = "xPos", required=true) int xPos,
    		@RequestParam(value = "yPos", required=true) int yPos,
    		@RequestParam(value = "signPageNumber", required=true) int signPageNumber,
    		@RequestParam(value = "password", required=false) String password, RedirectAttributes redirectAttrs, HttpServletResponse response, Model model) {
		String eppn = userService.getEppnFromAuthentication();
		User user = User.findUsersByEppnEquals(eppn).getSingleResult();
    	SignRequest signRequest = SignRequest.findSignRequest(id);
    	Map<String, String> params = signRequest.getParams();
    	if(signRequest.getCreateBy().equals(user.getEppn()) && signRequest.getRecipientEmail() != null) {
        	redirectAttrs.addFlashAttribute("messageCustom", "error");
    		return "redirect:/user/signrequests/" + id;
    	}
    	
    	File signImage = user.getSignImage().getBigFile().toJavaIoFile();
        File toSignFile = signRequest.getOriginalFile().getBigFile().toJavaIoFile();

    	NewPageType newPageType = NewPageType.valueOf(params.get("newPageType"));
    	SignType signType = SignType.valueOf(params.get("signType"));
    	String base64PemCert = null;
    	if(signType.equals(SignType.certPAdES)) {
    	if(password == null) password = "";
        	userKeystoreService.setPassword(password);
        	base64PemCert = userKeystoreService.getBase64PemCertificat(user.getKeystore().getBigFile().toJavaIoFile(), user.getEppn(), user.getEppn());
    	}
        try {
	        File signedFile = pdfService.signPdf(toSignFile, signImage, signType, base64PemCert, signPageNumber, xPos, yPos, newPageType);
	  	
	        if(signedFile != null) {
	        	params.put("signPageNumber", String.valueOf(signPageNumber));
				params.put("xPos", String.valueOf(xPos));
				params.put("yPos", String.valueOf(yPos));
				signRequest.setParams(params);
				signRequest.setSignedFile(documentService.addFile(signedFile, "application/pdf"));
			    signRequest.setStatus(SignRequestStatus.signed);
			    signRequest.setUpdateBy(eppn);
			    signRequest.setUpdateDate(new Date());
	        }
        } catch (IOException e) {
        	log.error("file to sign or sign image opening error", e);
		}
        
        return "redirect:/user/signrequests/" + id;
    }
	
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
    public String delete(@PathVariable("id") Long id, @RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, Model uiModel) {
    	SignRequest signRequest = SignRequest.findSignRequest(id);
        signRequest.remove();
        uiModel.asMap().clear();
        uiModel.addAttribute("page", (page == null) ? "1" : page.toString());
        uiModel.addAttribute("size", (size == null) ? "10" : size.toString());
        return "redirect:/user/signrequests/";
    }

    @RequestMapping(value = "/get-original-file/{id}", method = RequestMethod.GET)
    public void getOriginalFile(@PathVariable("id") Long id, HttpServletResponse response, Model model) {
    	SignRequest document = SignRequest.findSignRequest(id);
        Document file = document.getOriginalFile();
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
    	SignRequest document = SignRequest.findSignRequest(id);
        Document file = document.getSignedFile();
        try {
            response.setHeader("Content-Disposition", "inline;filename=\"" + file.getFileName() + "\"");
            response.setContentType(file.getContentType());
            IOUtils.copy(file.getBigFile().getBinaryFile().getBinaryStream(), response.getOutputStream());
        } catch (Exception e) {
            log.error("get file error", e);
        }
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
}
