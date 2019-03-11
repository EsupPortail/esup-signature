package org.esupportail.esupsignature.web.user;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
	private PdfService pdfService;

	@Resource
	private FileService fileService;
	
	@RequestMapping(value = "/{id}/getimage", method = RequestMethod.GET)
	public void getImageAsByteArray(@PathVariable("id") Long id, HttpServletResponse response) throws IOException, SQLException {
		Document document = Document.findDocument(id);
		InputStream in = document.getBigFile().getBinaryFile().getBinaryStream();
	    response.setContentType(MediaType.IMAGE_PNG_VALUE);
	    IOUtils.copy(in, response.getOutputStream());
	}
	
	@RequestMapping(value = "/{id}/getimagepdfpage/{page}", method = RequestMethod.GET)
	public void getImagePdfAsByteArray(@PathVariable("id") Long id, @PathVariable("page") int page, HttpServletResponse response) throws IOException {
		Document document = Document.findDocument(id);
		InputStream in = null;
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
		//in = fileService.notFoundImageToInputStream("png");
	    response.setContentType(MediaType.IMAGE_PNG_VALUE);
	    IOUtils.copy(in, response.getOutputStream());
	    in.close();
	}
	
    String encodeUrlPathSegment(String pathSegment, HttpServletRequest httpServletRequest) {
        String enc = httpServletRequest.getCharacterEncoding();
        if (enc == null) {
            enc = WebUtils.DEFAULT_CHARACTER_ENCODING;
        }
        pathSegment = UriUtils.encodePathSegment(pathSegment, enc);
        return pathSegment;
    }
    
}
