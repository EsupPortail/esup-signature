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

import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.domain.SignRequest.NewPageType;
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
public class SignRequestControler {

	private static final Logger log = LoggerFactory.getLogger(SignRequestControler.class);
	
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
    public String createForm(Model uiModel) {
        populateEditForm(uiModel, new SignRequest());
        return "user/signrequests/create";
    }
    
    @RequestMapping(value="/tosign", produces = "text/html")
    public String listToSign(@RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "sortFieldName", required = false) String sortFieldName, @RequestParam(value = "sortOrder", required = false) String sortOrder, Model uiModel) {
		String eppn = userService.getEppnFromAuthentication();
		if(User.countFindUsersByEppnEquals(eppn) == 0) {
			return "redirect:/user/users";
		}
		User user = User.findUsersByEppnEquals(eppn).getSingleResult();
        if (page != null || size != null) {
            int sizeNo = size == null ? 10 : size.intValue();
            //final int firstResult = page == null ? 0 : (page.intValue() - 1) * sizeNo;
            uiModel.addAttribute("signRequests", SignRequest.findSignRequestsByRecipientEmailEquals(user.getEmail(), sortFieldName, sortOrder).getResultList());
            float nrOfPages = (float) SignRequest.countSignRequests() / sizeNo;
            uiModel.addAttribute("maxPages", (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages));
        } else {
            uiModel.addAttribute("signRequests", SignRequest.findSignRequestsByRecipientEmailEquals(user.getEmail(), sortFieldName, sortOrder).getResultList());
        }
        return "user/signrequests/list";
    }

    @RequestMapping(produces = "text/html")
    public String listMyDocs(@RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "sortFieldName", required = false) String sortFieldName, @RequestParam(value = "sortOrder", required = false) String sortOrder, Model uiModel) {
		String eppn = userService.getEppnFromAuthentication();
		if(User.countFindUsersByEppnEquals(eppn) == 0) {
			return "redirect:/user/users";
		}
    	if(SignRequest.countFindSignRequestsByCreateByEquals(eppn) == 0) {
    		return "redirect:/user/signrequests/?form"; 
    	}
        if (page != null || size != null) {
            int sizeNo = size == null ? 10 : size.intValue();
            //final int firstResult = page == null ? 0 : (page.intValue() - 1) * sizeNo;
            uiModel.addAttribute("signRequests", SignRequest.findSignRequestsByCreateByEquals(eppn, sortFieldName, sortOrder).getResultList());
            float nrOfPages = (float) SignRequest.countSignRequests() / sizeNo;
            uiModel.addAttribute("maxPages", (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages));
        } else {
            uiModel.addAttribute("signRequests", SignRequest.findSignRequestsByCreateByEquals(eppn, sortFieldName, sortOrder).getResultList());
        }
        return "user/signrequests/list";
    }
    
    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) throws SQLException, IOException, Exception {
		String eppn = userService.getEppnFromAuthentication();
        addDateTimeFormatPatterns(uiModel);
        SignRequest signRequest = SignRequest.findSignRequest(id);
        if(signRequest.getCreateBy().equals(eppn)) {
        	File toConvertFile;
        	if(signRequest.getStatus().equals(SignRequestStatus.signed)) {
        		toConvertFile = signRequest.getSignedFile().getBigFile().toJavaIoFile();
        	} else {
        		toConvertFile = signRequest.getOriginalFile().getBigFile().toJavaIoFile();
        	}
        	List<String> imagePages = pdfService.pageAsImage(toConvertFile);
        	User user = User.findUsersByEppnEquals(eppn).getSingleResult();
        	uiModel.addAttribute("keystore", user.getKeystore().getFileName());
	        uiModel.addAttribute("signRequest", signRequest);
	        uiModel.addAttribute("itemId", id);
	        uiModel.addAttribute("imagePagesSize", imagePages.size());
	        uiModel.addAttribute("imagePages", imagePages);
	        return "user/signrequests/show";
        } else {
        	return "redirect:/user/signrequests/";
        }
    }
    
	@RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(@Valid SignRequest signRequest, @RequestParam("multipartFile") MultipartFile multipartFile, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            uiModel.addAttribute("signRequest", signRequest);
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
			params.put("signPageNumber", signRequest.getSignPageNumber());
			params.put("xPos", signRequest.getXPos());
			params.put("yPos", signRequest.getYPos());
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
        
    	File signImage = user.getSignImage().getBigFile().toJavaIoFile();
        File toSignFile = signRequest.getOriginalFile().getBigFile().toJavaIoFile();

    	if(signRequest.getParams().containsKey("signPageNumber")) {
    		signPageNumber = Integer.valueOf(signRequest.getParams().get("signPageNumber"));
        }
    	NewPageType newPageType = NewPageType.valueOf(signRequest.getParams().get("newPageType"));
    	SignType signType = SignType.valueOf(signRequest.getParams().get("signType"));
    	String base64PemCert = null;
    	if(signType.equals(SignType.certPAdES)) {
    	if(password == null) password = "";
        	userKeystoreService.setPassword(password);
        	base64PemCert = userKeystoreService.getBase64PemCertificat(user.getKeystore().getBigFile().toJavaIoFile(), user.getEppn(), user.getEppn());
    	}
        try {
	        File signedFile = pdfService.signPdf(toSignFile, signImage, signType, base64PemCert, signPageNumber, xPos, yPos, newPageType);
	  	
	        if(signedFile != null) {
				signRequest.setSignedFile(documentService.addFile(signedFile, "application/pdf"));
			    signRequest.setStatus(SignRequestStatus.signed);
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
    
    void populateEditForm(Model uiModel, SignRequest signRequest) {
        uiModel.addAttribute("signRequest", signRequest);
        addDateTimeFormatPatterns(uiModel);
        uiModel.addAttribute("files", Document.findAllDocuments());
        uiModel.addAttribute("signTypes", Arrays.asList(SignType.values()));
        uiModel.addAttribute("newPageTypes", Arrays.asList(NewPageType.values()));
    }
    
    void addDateTimeFormatPatterns(Model uiModel) {
        uiModel.addAttribute("signRequest_createdate_date_format", "dd/MM/yyyy HH:mm");
        uiModel.addAttribute("signRequest_updatedate_date_format", "dd/MM/yyyy HH:mm");
    }
}
