package org.esupportail.esupsignature.web.controller.admin;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.BigFileRepository;
import org.esupportail.esupsignature.repository.DocumentRepository;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

@RequestMapping("/admin/documents")
@Controller
@Transactional
@Scope(value="session")
public class AdminDocumentController {
	
	private static final Logger logger = LoggerFactory.getLogger(AdminDocumentController.class);
	
	@Resource
	private UserService userService;
	
	@ModelAttribute("user")
	public User getUser() {
		return userService.getUserFromAuthentication();
	}
	
	@Autowired
	private SignRequestRepository signRequestRepository;

	@Autowired
	private SignBookRepository signBookRepository;
	
	@Autowired
	private DocumentRepository documentRepository;
	
	@Autowired
	private BigFileRepository bigFileRepository;
	
	@Resource
	private PdfService pdfService;

	@Resource
	private FileService fileService;
	
	@Resource
	private DocumentService documentService;
	
	@Resource
	private SignRequestService signRequestService;

	@Resource
	private SignBookService signBookService;
	
	@RequestMapping(value = "/{id}/getimage", method = RequestMethod.GET)
	public void getImageAsByteArray(@PathVariable("id") Long id, HttpServletResponse response) throws IOException, SQLException {
		Document document = documentRepository.findById(id).get();
		SignRequest signRequest = signRequestRepository.findById(document.getParentId()).get();
		User user = userService.getUserFromAuthentication();
		InputStream in = null;
		if(signRequestService.checkUserViewRights(user, signRequest)) {
			in = document.getBigFile().getBinaryFile().getBinaryStream();
		    response.setContentType(MediaType.IMAGE_PNG_VALUE);
		    IOUtils.copy(in, response.getOutputStream());
		}else {
			in = new FileInputStream(fileService.notFoundImageToInputStream("png"));
			IOUtils.copy(in, response.getOutputStream());
		    in.close();
		}
	}
	
	@RequestMapping(value = "/{id}/getimagepdfpage/{page}", method = RequestMethod.GET)
	public void getImagePdfAsByteArray(@PathVariable("id") Long id, @PathVariable("page") int page, HttpServletResponse response) throws IOException {
		Document document = documentRepository.findById(id).get();
		SignRequest signRequest = null;
		if(signRequestRepository.countById(document.getParentId()) > 0) {
			signRequest = signRequestRepository.findById(document.getParentId()).get();
		}
		SignBook signBook = null;
		if(signBookRepository.countById(document.getParentId()) > 0) {
			signBook = signBookRepository.findById(document.getParentId()).get();
		}
		User user = userService.getUserFromAuthentication();
		InputStream in = null;
		if((signRequest != null && signRequestService.checkUserViewRights(user, signRequest)) 
		|| (signBook != null && signBookService.checkUserManageRights(user, signBook))) {
			try {
				in = pdfService.pageAsInputStream(document.getJavaIoFile(), page);
			} catch (Exception e) {
				logger.error("page " + page + " not found in this document");
			}
			if(in == null) {
				try {
					in = pdfService.pageAsInputStream(document.getJavaIoFile(), 0);
				} catch (Exception e) {
					logger.error("page " + page + " not found in this document");
				}
			}
		    response.setContentType(MediaType.IMAGE_PNG_VALUE);
		    IOUtils.copy(in, response.getOutputStream());
		    in.close();
		} else {
			in = new FileInputStream(fileService.notFoundImageToInputStream("png"));
			IOUtils.copy(in, response.getOutputStream());
		    in.close();
		}
	}

    
    @RequestMapping(value = "/getfile/{id}", method = RequestMethod.GET)
	public ResponseEntity<Void> getFile(@PathVariable("id") Long id, HttpServletResponse response, Model model) {
		Document document = documentRepository.findById(id).get();
		SignRequest signRequest = signRequestRepository.findById(document.getParentId()).get();
		User user = userService.getUserFromAuthentication();
		try {
			File fileToDownload = document.getJavaIoFile();
			response.setHeader("Content-Disposition", "inline;filename=\"" + document.getFileName() + "\"");
			response.setContentType(document.getContentType());
			IOUtils.copy(new FileInputStream(fileToDownload), response.getOutputStream());
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			logger.error("get file error", e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
    public String delete(@PathVariable("id") Long id, @RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, Model uiModel) {
        Document document = documentRepository.findById(id).get();
        documentService.deleteDocument(document);
        uiModel.asMap().clear();
        uiModel.addAttribute("page", (page == null) ? "1" : page.toString());
        uiModel.addAttribute("size", (size == null) ? "10" : size.toString());
        return "redirect:/user/documents";
    }

	void addDateTimeFormatPatterns(Model uiModel) {
        uiModel.addAttribute("document_createdate_date_format", "dd/MM/yyyy HH:mm");
    }

	void populateEditForm(Model uiModel, Document document) {
        uiModel.addAttribute("document", document);
        addDateTimeFormatPatterns(uiModel);
        uiModel.addAttribute("bigfiles", bigFileRepository.findAll());
    }
}
