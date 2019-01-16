package org.esupportail.esupsignature.web.user;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.esupportail.esupsignature.domain.Content;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequest.DocStatus;
import org.esupportail.esupsignature.domain.SignRequest.NewPageType;
import org.esupportail.esupsignature.domain.SignRequest.SignType;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.ldap.PersonLdapDao;
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.PdfService;
import org.esupportail.esupsignature.service.UserKeystoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
	
	@Autowired
	PersonLdapDao personDao;
	
	@Resource
	FileService fileService;

	@Resource
	UserKeystoreService userKeystoreService;

	
    @RequestMapping(params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
        populateEditForm(uiModel, new SignRequest());
        return "user/signrequests/create";
    }
    
    @RequestMapping(produces = "text/html")
    public String list(@RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "sortFieldName", required = false) String sortFieldName, @RequestParam(value = "sortOrder", required = false) String sortOrder, Model uiModel) {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
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
    public String show(@PathVariable("id") Long id, Model uiModel) {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
        addDateTimeFormatPatterns(uiModel);
        SignRequest signRequest = SignRequest.findSignRequest(id);
        if(signRequest.getCreateBy().equals(eppn)) {
        	User user = User.findUsersByEppnEquals(eppn).getSingleResult();
        	uiModel.addAttribute("keystore", user.getKeystore().getFileName());
	        uiModel.addAttribute("signRequest", signRequest);
	        Content originalFile = signRequest.getOriginalFile();
	        uiModel.addAttribute("originalFilePath", originalFile.getUrl());
	        uiModel.addAttribute("itemId", id);
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
        uiModel.asMap().clear();
        try {
        	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    		String eppn = auth.getName();
        	signRequest.setCreateBy(eppn);
        	signRequest.setCreateDate(new Date());
			signRequest.setOriginalFile(fileService.addFile(multipartFile));
			signRequest.setSignedFile(null);
			signRequest.setStatus(DocStatus.pending);
			Map<String, String> params = new HashMap<>();
			params.put("signType", signRequest.getSignType());
			params.put("newPageType", signRequest.getNewPageType());
			params.put("signPageNumber", signRequest.getSignPageNumber());
			params.put("xPos", signRequest.getXPos());
			params.put("yPos", signRequest.getYPos());
			signRequest.setParams(params);
	        signRequest.persist();
        } catch (IOException | SQLException e) {
        	log.error("Create file error", e);
		}
        return "redirect:/user/signrequests/";
    }

    @RequestMapping(value = "/signdoc/{id}", method = RequestMethod.POST)
    public String signdoc(@PathVariable("id") Long id, @RequestParam(value = "password", required=false) String password, RedirectAttributes redirectAttrs, HttpServletResponse response, Model model) throws Exception {
    	log.info("begin sign");
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
		User user = User.findUsersByEppnEquals(eppn).getSingleResult();
    	SignRequest signRequest = SignRequest.findSignRequest(id);
        Content toSignContent = signRequest.getOriginalFile();
        File toSignFile = toSignContent.getBigFile().toJavaIoFile();
        pdfService.signPageNumber = 1;
        if(signRequest.getParams().get("newPageType").equals(NewPageType.onBegin.toString())) {
        	log.info("add page on begin");
        	toSignFile = pdfService.addWhitePage(toSignContent.getBigFile().toJavaIoFile(), 0);
        } else 
        if(signRequest.getParams().get("newPageType").equals(NewPageType.onEnd.toString())) {
        	log.info("add page on end");
        	toSignFile = pdfService.addWhitePage(toSignContent.getBigFile().toJavaIoFile(), -1);
        } else
    	if(!signRequest.getParams().get("signPageNumber").isEmpty()) {
    		pdfService.signPageNumber = Integer.valueOf(signRequest.getParams().get("signPageNumber"));
        }
        int xPos = 0;
        int yPos = 0;
        if(!signRequest.getParams().get("xPos").isEmpty() && !signRequest.getParams().get("yPos").isEmpty()) {
        	xPos = Integer.valueOf(signRequest.getParams().get("xPos"));
        	yPos = Integer.valueOf(signRequest.getParams().get("yPos"));
        }
        if(signRequest.getParams().get("signType").equals(SignType.imageStamp.toString())) {
        	log.info("imageStamp signature");
        	File signedFile = pdfService.addImage(toSignFile, user.getSignImage().getBigFile().toJavaIoFile(), pdfService.signPageNumber, xPos, yPos);
            signRequest.setSignedFile(fileService.addFile(new FileInputStream(signedFile), "signed_" + toSignContent.getFileName(), signedFile.length(), toSignContent.getContentType()));

        } else 
        if(signRequest.getParams().get("signType").equals(SignType.certPAdES.toString())) {
        	log.info("cades signature");
        	if(password != null) {
            	userKeystoreService.setPassword(password);
            } else {
            	redirectAttrs.addFlashAttribute("messageCustom", "bad password");
            }
            try {
            	String pemCert = userKeystoreService.getPemCertificat(user.getKeystore().getBigFile().toJavaIoFile(), user.getEppn(), user.getEppn());
            	Content signedFile = fileService.certSignPdf(toSignFile, userKeystoreService.pemToBase64String(pemCert), null, user.getSignImage(), pdfService.signPageNumber, xPos, yPos);
            	signRequest.setSignedFile(signedFile);
            } catch (Exception e) {
            	redirectAttrs.addFlashAttribute("messageCustom", "keystore issue");
    		}
        	signRequest.setStatus(DocStatus.signed);
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
        uiModel.addAttribute("files", Content.findAllContents());
        uiModel.addAttribute("signTypes", Arrays.asList(SignType.values()));
        uiModel.addAttribute("newPageTypes", Arrays.asList(NewPageType.values()));
    }
    
    void addDateTimeFormatPatterns(Model uiModel) {
        uiModel.addAttribute("signRequest_createdate_date_format", "dd/MM/yyyy HH:mm");
        uiModel.addAttribute("signRequest_updatedate_date_format", "dd/MM/yyyy HH:mm");
    }
}
