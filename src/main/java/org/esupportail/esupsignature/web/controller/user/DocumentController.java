package org.esupportail.esupsignature.web.controller.user;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.BigFileRepository;
import org.esupportail.esupsignature.repository.DocumentRepository;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

@RequestMapping("/user/documents")
@Controller
@Transactional
@Scope(value="session")
public class DocumentController {
	
	private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
	
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
	
    String encodeUrlPathSegment(String pathSegment, HttpServletRequest httpServletRequest) {
        String enc = httpServletRequest.getCharacterEncoding();
        if (enc == null) {
            enc = WebUtils.DEFAULT_CHARACTER_ENCODING;
        }
        pathSegment = UriUtils.encodePathSegment(pathSegment, enc);
        return pathSegment;
    }
    
    @RequestMapping(value = "/getfile/{id}", method = RequestMethod.GET)
	public ResponseEntity<Void> getFile(@PathVariable("id") Long id, HttpServletResponse response, Model model) {
		Document document = documentRepository.findById(id).get();
		SignRequest signRequest = signRequestRepository.findById(document.getParentId()).get();
		User user = userService.getUserFromAuthentication();
		//TODO les modèle ne sont pas protégés
		if(signRequestService.checkUserViewRights(user, signRequest)) {
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
		} else {
			logger.warn(user.getEppn() + " try to access " + id + " without view rights");
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
	}
    
    @RequestMapping(value = "/test", method = RequestMethod.GET)
	public ResponseEntity<Void> test(HttpServletResponse response, Model model) throws IOException {
		File file = fileService.stringToImageFile("test\nok", "png");
		try {
			response.setHeader("Content-Disposition", "inline;filename=\"test.png\"");
			response.setContentType("image/png");
			IOUtils.copy(new FileInputStream(file), response.getOutputStream());
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			logger.error("get file error", e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
    

	@RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(@Valid Document document, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, document);
            return "user/documents/create";
        }
        uiModel.asMap().clear();
        documentRepository.save(document);
        return "redirect:/user/documents/" + encodeUrlPathSegment(document.getId().toString(), httpServletRequest);
    }

	@RequestMapping(params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
        populateEditForm(uiModel, new Document());
        return "user/documents/create";
    }

	@RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
        addDateTimeFormatPatterns(uiModel);
        uiModel.addAttribute("document", documentRepository.findById(id).get());
        uiModel.addAttribute("itemId", id);
        return "user/documents/show";
    }

	@RequestMapping(produces = "text/html")
    public String list(@RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "sortFieldName", required = false) String sortFieldName, @RequestParam(value = "sortOrder", required = false) String sortOrder, Model uiModel) {
        if (page != null || size != null) {
            int sizeNo = size == null ? 10 : size.intValue();
            uiModel.addAttribute("documents", documentRepository.findAll());
            float nrOfPages = (float) documentRepository.count() / sizeNo;
            uiModel.addAttribute("maxPages", (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages));
        } else {
            uiModel.addAttribute("documents", documentRepository.findAll());
        }
        addDateTimeFormatPatterns(uiModel);
        return "user/documents/list";
    }

	@RequestMapping(method = RequestMethod.PUT, produces = "text/html")
    public String update(@Valid Document document, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, document);
            return "user/documents/update";
        }
        uiModel.asMap().clear();
        documentRepository.save(document);
        return "redirect:/user/documents/" + encodeUrlPathSegment(document.getId().toString(), httpServletRequest);
    }

	@RequestMapping(value = "/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel) {
        populateEditForm(uiModel, documentRepository.findById(id).get());
        return "user/documents/update";
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
