package org.esupportail.esupsignature.web.user;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.Document.DocStatus;
import org.esupportail.esupsignature.domain.Document.SignType;
import org.esupportail.esupsignature.domain.File;
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
	        uiModel.addAttribute("document", document);
	        File originalFile = document.getOriginalFile();
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
	        document.persist();
        } catch (IOException | SQLException e) {
        	log.error("Create file error", e);
		}
        return "redirect:/user/documents/";
    }

    @RequestMapping(value = "/signdoc/{id}", method = RequestMethod.POST)
    public String signdoc(@PathVariable("id") Long id, @RequestParam(value = "password", required=false) String password, RedirectAttributes redirectAttrs, HttpServletResponse response, Model model) throws Exception {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
		User user = User.findUsersByEppnEquals(eppn).getSingleResult();
    	Document document = Document.findDocument(id);
        File file = document.getOriginalFile();
//TODO : if ajout de page
        if(document.getSignType().equals(SignType.imageStamp)) {
        	//pdfService.addWhitePageOnTop(file.getBigFile().toJavaIoFile(), 0)
        	java.io.File signedFile = pdfService.addImage(file.getBigFile().toJavaIoFile(), user.getSignImage().getBigFile().toJavaIoFile(), 1, 200, 200);
            document.setSignedFile(fileService.addFile(new FileInputStream(signedFile), "signed_" + file.getFileName(), signedFile.length(), file.getContentType()));

        } else 
        if(document.getSignType().equals(SignType.certPAdES)) {
            if(password != null) {
            	userKeystoreService.setPassword(password);
            } else {
            	redirectAttrs.addFlashAttribute("messageCustom", "bad password");
            }
            try {
                String pemCert = userKeystoreService.getPemCertificat(user.getKeystore().getBigFile().toJavaIoFile(), user.getEppn(), user.getEppn());
            	File signedFile = fileService.certSignPdf(file, userKeystoreService.pemToBase64String(pemCert), null, user.getSignImage(), 200, 200, true, -1);
            	document.setSignedFile(signedFile);
            } catch (IOException e) {
            	redirectAttrs.addFlashAttribute("messageCustom", "bad password");
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
        uiModel.addAttribute("files", File.findAllFiles());
        uiModel.addAttribute("signTypes", Arrays.asList(SignType.values()));
    }
    
    void addDateTimeFormatPatterns(Model uiModel) {
        uiModel.addAttribute("document_createdate_date_format", "dd/MM/yyyy HH:mm");
        uiModel.addAttribute("document_updatedate_date_format", "dd/MM/yyyy HH:mm");
    }
}
