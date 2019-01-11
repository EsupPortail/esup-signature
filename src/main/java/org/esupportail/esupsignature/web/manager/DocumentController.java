package org.esupportail.esupsignature.web.manager;
import java.io.IOException;
import java.sql.SQLException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.File;
import org.esupportail.esupsignature.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
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

@RequestMapping("/manager/documents")
@Controller
@RooWebScaffold(path = "manager/documents", formBackingObject = Document.class)
@Transactional
public class DocumentController {
	
	private static final Logger log = LoggerFactory.getLogger(DocumentController.class);
	
	@Resource
	FileService fileService;
	
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
			document.setOriginalFile(fileService.addFile(multipartFile));
			document.setSignedFile(null);
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
        File originalFile = document.getOriginalFile();
        uiModel.addAttribute("originalFilePath", originalFile.getUrl());
        uiModel.addAttribute("itemId", id);
        return "manager/documents/show";
    }
    
    @RequestMapping(value = "/get-original-file/{id}", method = RequestMethod.GET)
    public void getOriginalFile(@PathVariable("id") Long id, HttpServletResponse response, Model model) {
    	Document document = Document.findDocument(id);
        File file = document.getOriginalFile();
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
        File file = document.getSignedFile();
        try {
            response.setHeader("Content-Disposition", "inline;filename=\"" + file.getFileName() + "\"");
            response.setContentType(file.getContentType());
            IOUtils.copy(file.getBigFile().getBinaryFile().getBinaryStream(), response.getOutputStream());
        } catch (Exception e) {
            log.error("get file error", e);
        }
    }    
    
    @RequestMapping(value = "/signdoc/{id}", method = RequestMethod.GET)
    public String signdoc(@PathVariable("id") Long id, HttpServletResponse response, Model model) throws IOException, SQLException {
    	Document document = Document.findDocument(id);
        File file = document.getOriginalFile();
        String dummyCertif = "MIIFNTCCBB2gAwIBAgIQBqzQs3sFF+zIvlGgIT4ejzANBgkqhkiG9w0BAQsFADBp"
        		+ "MQswCQYDVQQGEwJOTDEWMBQGA1UECBMNTm9vcmQtSG9sbGFuZDESMBAGA1UEBxMJ"
        		+ "QW1zdGVyZGFtMQ8wDQYDVQQKEwZURVJFTkExHTAbBgNVBAMTFFRFUkVOQSBQZXJz"
        		+ "b25hbCBDQSAzMB4XDTE5MDEwNzAwMDAwMFoXDTIyMDEwNzEyMDAwMFowYzELMAkG"
        		+ "A1UEBhMCRlIxGjAYBgNVBAcTEU1vbnQtU2FpbnQtQWlnbmFuMR0wGwYDVQQKDBRV"
        		+ "bml2ZXJzaXTDqSBkZSBSb3VlbjEZMBcGA1UEAxMQRGF2aWQgTGVtYWlnbmVudDCC"
        		+ "ASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAO8jqX+obuSKsy4a+o0V1AXO"
        		+ "j30li/AJ0OzVDbWKE+9h4WYYPPutsK94Plp725bkSMCM7UEylHq9huNFhMn9SsEi"
        		+ "DwRkehlaASgrkM5AXycQaaHcpJj4cmtdrq/lVamapq5TPrqS+DOKIqUjT56EfIaT"
        		+ "EzhLzd00rpMOmoHZSYZgcdh56xvGcKy/XJt4JOqA1D4pbC/O2t8G+WL5cmjgobXL"
        		+ "wKyBtb6tPNnPBSnGDV+7Ey0Hxg8dA85zojD8v8z2ak8/uiJth7xX9ZcCbEhaFGlD"
        		+ "2ri2Yc7IKeYdoSbS50X36+f9BzQEvXhgAMfGkTYVXTCDOUtn3lg/9TPPbBoWvlkC"
        		+ "AwEAAaOCAd0wggHZMB8GA1UdIwQYMBaAFPAh6Ul3c5+Frhg76FJwFAbtQu7KMB0G"
        		+ "A1UdDgQWBBSIyoNX6ACsp2Kq4NoZUi4/qD/CCjAMBgNVHRMBAf8EAjAAMCkGA1Ud"
        		+ "EQQiMCCBHmRhdmlkLmxlbWFpZ25lbnRAdW5pdi1yb3Vlbi5mcjAOBgNVHQ8BAf8E"
        		+ "BAMCBaAwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMEMEMGA1UdIAQ8MDow"
        		+ "OAYKYIZIAYb9bAQBAjAqMCgGCCsGAQUFBwIBFhxodHRwczovL3d3dy5kaWdpY2Vy"
        		+ "dC5jb20vQ1BTMHUGA1UdHwRuMGwwNKAyoDCGLmh0dHA6Ly9jcmwzLmRpZ2ljZXJ0"
        		+ "LmNvbS9URVJFTkFQZXJzb25hbENBMy5jcmwwNKAyoDCGLmh0dHA6Ly9jcmw0LmRp"
        		+ "Z2ljZXJ0LmNvbS9URVJFTkFQZXJzb25hbENBMy5jcmwwcwYIKwYBBQUHAQEEZzBl"
        		+ "MCQGCCsGAQUFBzABhhhodHRwOi8vb2NzcC5kaWdpY2VydC5jb20wPQYIKwYBBQUH"
        		+ "MAKGMWh0dHA6Ly9jYWNlcnRzLmRpZ2ljZXJ0LmNvbS9URVJFTkFQZXJzb25hbENB"
        		+ "My5jcnQwDQYJKoZIhvcNAQELBQADggEBAAzEOiuLxAHe25gngoHLSY+EUdLuvvKJ"
        		+ "cVrgedFq3fIXhEH5W1d4fzKJERmiBnEB//GWOsbj9ypoXZjXodPS1l+FyyJflIFy"
        		+ "lI/+ohZxqrIYn2ENLoD0OPieeQcmfy0voYWeaGuq9fFcjd7rJa2uHjADkJ2i243x"
        		+ "yIFnP3wH5BGhhW/kiTtHn3630vHrmacKjEd/G++tAUH1EQMPiMX9IR1UqqM75Mv7"
        		+ "jq12xsdGf0t+VWdITqq73oEWFoFkhLnjPujkm7vKhys0mraPpgiWo1+vhAjvxgW3"
        		+ "r+THyPkXCDolvZaKLK1KURd9v8UH7d1CCXZ5J751wdN03h9LEwyeQt4=";
        
        
        document.setSignedFile(
        		fileService.signPdf(file, dummyCertif, null)
        		);
        
        
        return "redirect:/manager/documents/" + id;
    }
}
