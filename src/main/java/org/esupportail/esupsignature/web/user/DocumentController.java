package org.esupportail.esupsignature.web.user;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

@RequestMapping("/user/documents")
@Controller
@RooWebScaffold(path = "user/documents", formBackingObject = Document.class)
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
	
	@Resource
	private PdfService pdfService;

	@Resource
	private FileService fileService;
	
	@Resource
	private SignRequestService signRequestService;

	@Resource
	private SignBookService signBookService;
	
	@RequestMapping(value = "/{id}/getimage", method = RequestMethod.GET)
	public void getImageAsByteArray(@PathVariable("id") Long id, HttpServletResponse response) throws IOException, SQLException {
		Document document = Document.findDocument(id);
		SignRequest signRequest = SignRequest.findSignRequest(document.getParentId());
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
		Document document = Document.findDocument(id);
		SignRequest signRequest = SignRequest.findSignRequest(document.getParentId());
		SignBook signBook = SignBook.findSignBook(document.getParentId());
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
		Document document = Document.findDocument(id);
		SignRequest signRequest = SignRequest.findSignRequest(document.getParentId());
		SignBook signBook = SignBook.findSignBook(document.getParentId());
		User user = userService.getUserFromAuthentication();
		//TODO les modèle ne sont pas protégés
		if(signRequestService.checkUserViewRights(user, signRequest) || signBook != null) {
			try {
				File fileToDownload = document.getJavaIoFile();
				if(signBook != null && document.getContentType().equals("application/pdf")) {
					//TODO remplissage pdf
					fileToDownload = pdfService.ldapFill(fileToDownload, user);
				}
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
    
}
