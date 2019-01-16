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
import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.Document.DocStatus;
import org.esupportail.esupsignature.domain.Document.NewPageType;
import org.esupportail.esupsignature.domain.Document.SignType;
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

@RequestMapping("/user/documents")
@Controller
@Transactional
@Scope(value="session")
public class DocumentControler {

	private static final Logger log = LoggerFactory.getLogger(DocumentControler.class);
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return "user/documents";
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
        populateEditForm(uiModel, new Document());
        return "user/documents/create";
    }
    
    @RequestMapping(produces = "text/html")
    public String list(@RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "sortFieldName", required = false) String sortFieldName, @RequestParam(value = "sortOrder", required = false) String sortOrder, Model uiModel) {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
		if(User.countFindUsersByEppnEquals(eppn) == 0) {
			return "redirect:/user/users";
		}
    	if(Document.countFindDocumentsByCreateByEquals(eppn) == 0) {
    		return "redirect:/user/documents/?form"; 
    	}
        if (page != null || size != null) {
            int sizeNo = size == null ? 10 : size.intValue();
            //final int firstResult = page == null ? 0 : (page.intValue() - 1) * sizeNo;
            uiModel.addAttribute("documents", Document.findDocumentsByCreateByEquals(eppn, sortFieldName, sortOrder).getResultList());
            float nrOfPages = (float) Document.countDocuments() / sizeNo;
            uiModel.addAttribute("maxPages", (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages));
        } else {
            uiModel.addAttribute("documents", Document.findDocumentsByCreateByEquals(eppn, sortFieldName, sortOrder).getResultList());
        }
        return "user/documents/list";
    }
	
    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
        addDateTimeFormatPatterns(uiModel);
        Document document = Document.findDocument(id);
        if(document.getCreateBy().equals(eppn)) {
        	User user = User.findUsersByEppnEquals(eppn).getSingleResult();
        	uiModel.addAttribute("keystore", user.getKeystore().getFileName());
	        uiModel.addAttribute("document", document);
	        Content originalFile = document.getOriginalFile();
	        uiModel.addAttribute("originalFilePath", originalFile.getUrl());
	        uiModel.addAttribute("itemId", id);
	        return "user/documents/show";
        } else {
        	return "redirect:/user/documents/";
        }
    }
    
	@RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(@Valid Document document, @RequestParam("multipartFile") MultipartFile multipartFile, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            uiModel.addAttribute("document", document);
            return "user/documents/create";
        }
        uiModel.asMap().clear();
        try {
        	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    		String eppn = auth.getName();
        	document.setCreateBy(eppn);
        	document.setCreateDate(new Date());
			document.setOriginalFile(fileService.addFile(multipartFile));
			document.setSignedFile(null);
			document.setStatus(DocStatus.pending);
			Map<String, String> params = new HashMap<>();
			params.put("signType", document.getSignType());
			params.put("newPageType", document.getNewPageType());
			params.put("signPageNumber", document.getSignPageNumber());
			params.put("xPos", document.getXPos());
			params.put("yPos", document.getYPos());
			document.setParams(params);
	        document.persist();
        } catch (IOException | SQLException e) {
        	log.error("Create file error", e);
		}
        return "redirect:/user/documents/";
    }

    @RequestMapping(value = "/signdoc/{id}", method = RequestMethod.POST)
    public String signdoc(@PathVariable("id") Long id, @RequestParam(value = "password", required=false) String password, RedirectAttributes redirectAttrs, HttpServletResponse response, Model model) throws Exception {
    	log.info("begin sign");
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
		User user = User.findUsersByEppnEquals(eppn).getSingleResult();
    	Document document = Document.findDocument(id);
        Content toSignContent = document.getOriginalFile();
        File toSignFile = toSignContent.getBigFile().toJavaIoFile();
        int signPageNumber = 1;
        if(document.getParams().get("newPageType").equals(NewPageType.onBegin.toString())) {
        	log.info("add page on begin");
        	toSignFile = pdfService.addWhitePage(toSignContent.getBigFile().toJavaIoFile(), 0, signPageNumber);
        } else 
        if(document.getParams().get("newPageType").equals(NewPageType.onEnd.toString())) {
        	signPageNumber = -1;
        	toSignFile = pdfService.addWhitePage(toSignContent.getBigFile().toJavaIoFile(), -1, signPageNumber);
        } else
    	if(!document.getParams().get("signPageNumber").isEmpty()) {
        	signPageNumber = Integer.valueOf(document.getParams().get("signPageNumber"));
        }
        int xPos = 0;
        int yPos = 0;
        if(!document.getParams().get("xPos").isEmpty() && !document.getParams().get("yPos").isEmpty()) {
        	xPos = Integer.valueOf(document.getParams().get("xPos"));
        	yPos = Integer.valueOf(document.getParams().get("yPos"));
        }
        if(document.getParams().get("signType").equals(SignType.imageStamp.toString())) {
        	log.info("imageStamp signature");
        	File signedFile = pdfService.addImage(toSignFile, user.getSignImage().getBigFile().toJavaIoFile(), signPageNumber, xPos, yPos);
            document.setSignedFile(fileService.addFile(new FileInputStream(signedFile), "signed_" + toSignContent.getFileName(), signedFile.length(), toSignContent.getContentType()));

        } else 
        if(document.getParams().get("signType").equals(SignType.certPAdES.toString())) {
        	log.info("cades signature");
        	if(password != null) {
            	userKeystoreService.setPassword(password);
            } else {
            	redirectAttrs.addFlashAttribute("messageCustom", "bad password");
            }
            try {
        		String pemCert = userKeystoreService.getPemCertificat(user.getKeystore().getBigFile().toJavaIoFile(), user.getEppn(), user.getEppn());
            	Content signedFile = fileService.certSignPdf(toSignFile, userKeystoreService.pemToBase64String(pemCert), null, user.getSignImage(), signPageNumber, xPos, yPos);
            	document.setSignedFile(signedFile);
            } catch (Exception e) {
            	redirectAttrs.addFlashAttribute("messageCustom", "keystore issue");
    		}
        	document.setStatus(DocStatus.signed);
        }
        return "redirect:/user/documents/" + id;
    }
	
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
    public String delete(@PathVariable("id") Long id, @RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, Model uiModel) {
    	Document document = Document.findDocument(id);
        document.remove();
        uiModel.asMap().clear();
        uiModel.addAttribute("page", (page == null) ? "1" : page.toString());
        uiModel.addAttribute("size", (size == null) ? "10" : size.toString());
        return "redirect:/user/documents/";
    }
    
    void populateEditForm(Model uiModel, Document document) {
        uiModel.addAttribute("document", document);
        addDateTimeFormatPatterns(uiModel);
        uiModel.addAttribute("files", Content.findAllContents());
        uiModel.addAttribute("signTypes", Arrays.asList(SignType.values()));
        uiModel.addAttribute("newPageTypes", Arrays.asList(NewPageType.values()));
    }
    
    void addDateTimeFormatPatterns(Model uiModel) {
        uiModel.addAttribute("document_createdate_date_format", "dd/MM/yyyy HH:mm");
        uiModel.addAttribute("document_updatedate_date_format", "dd/MM/yyyy HH:mm");
    }
}
