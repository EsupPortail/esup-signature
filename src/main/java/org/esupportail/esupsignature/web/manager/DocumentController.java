package org.esupportail.esupsignature.web.manager;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.Document.DocStatus;
import org.esupportail.esupsignature.domain.Content;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.PdfService;
import org.esupportail.esupsignature.service.UserKeystoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
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

@RequestMapping("/manager/documents")
@Controller
@RooWebScaffold(path = "manager/documents", formBackingObject = Document.class)
@Transactional
@Scope(value="session")
public class DocumentController {
	
	private static final Logger log = LoggerFactory.getLogger(DocumentController.class);
	
	@Resource
	FileService fileService;

	@Resource
	PdfService pdfService;
	
	@Resource
	UserKeystoreService userKeystoreService;
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return "documents";
	}
	
	@RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(@Valid Document document, @RequestParam("multipartFile") MultipartFile multipartFile, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, document);
            return "manager/documents/create";
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
        return "redirect:/manager/documents/" + encodeUrlPathSegment(document.getId().toString(), httpServletRequest);
    }
	
    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
        addDateTimeFormatPatterns(uiModel);
        Document document = Document.findDocument(id);
        uiModel.addAttribute("document", document);
        Content originalFile = document.getOriginalFile();
        uiModel.addAttribute("originalFilePath", originalFile.getUrl());
        uiModel.addAttribute("itemId", id);
        return "manager/documents/show";
    }
    
    @RequestMapping(value = "/get-original-file/{id}", method = RequestMethod.GET)
    public void getOriginalFile(@PathVariable("id") Long id, HttpServletResponse response, Model model) {
    	Document document = Document.findDocument(id);
        Content file = document.getOriginalFile();
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
    	Document document = Document.findDocument(id);
        Content file = document.getSignedFile();
        try {
            response.setHeader("Content-Disposition", "inline;filename=\"" + file.getFileName() + "\"");
            response.setContentType(file.getContentType());
            IOUtils.copy(file.getBigFile().getBinaryFile().getBinaryStream(), response.getOutputStream());
        } catch (Exception e) {
            log.error("get file error", e);
        }
    }    
    
    @RequestMapping(value = "/signdoc/{id}", method = RequestMethod.POST)
    public String signdoc(@PathVariable("id") Long id, @RequestParam("password") String password, RedirectAttributes redirectAttrs, HttpServletResponse response, Model model) throws Exception {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
		User user = User.findUsersByEppnEquals(eppn).getSingleResult();
    	Document document = Document.findDocument(id);
        Content file = document.getOriginalFile();
        Content signedFile;
        if(password != null && !password.isEmpty()) {
        	userKeystoreService.setPassword(password);
        }
        try {
            String pemCert = userKeystoreService.getPemCertificat(user.getKeystore().getBigFile().toJavaIoFile(), user.getEppn(), user.getEppn());
        	//signedFile = fileService.certSignPdf(file, userKeystoreService.pemToBase64String(pemCert), null, user.getSignImage(), 200, 200, true, -1);
            //signedFile = pdfService.addImage(file.getBigFile().toJavaIoFile(), user.getSignImage().getBigFile().toJavaIoFile(), 0, 200, 200);
            //document.setSignedFile(fileService.addFile(new FileInputStream(signedFile), "signed_" + file.getFileName(), signedFile.length(), file.getContentType()));
        	//document.setSignedFile(signedFile);
        	document.setStatus(DocStatus.signed);
        } catch (IOException e) {
        	redirectAttrs.addFlashAttribute("messageCustom", "bad password");
		}
        return "redirect:/manager/documents/" + id;
    }
}
