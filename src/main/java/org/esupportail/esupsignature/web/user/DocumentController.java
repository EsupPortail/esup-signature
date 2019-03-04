package org.esupportail.esupsignature.web.user;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.service.pdf.PdfService;
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
	
	@Resource
	private PdfService pdfService;
	
	@RequestMapping(value = "/{id}/getimage", method = RequestMethod.GET)
	public void getImageAsByteArray(@PathVariable("id") Long id, HttpServletResponse response) throws IOException, SQLException {
		Document document = Document.findDocument(id);
		InputStream in = document.getBigFile().getBinaryFile().getBinaryStream();
	    response.setContentType(MediaType.IMAGE_PNG_VALUE);
	    IOUtils.copy(in, response.getOutputStream());
	}
	
	@RequestMapping(value = "/{id}/getimagepdfpage/{page}", method = RequestMethod.GET)
	public void getImagePdfAsByteArray(@PathVariable("id") Long id, @PathVariable("page") int page, HttpServletResponse response) throws Exception {
		//TODO pb	 fermeture pdfdoc
		Document document = Document.findDocument(id);
		InputStream in = pdfService.pageAsInputStream(document.getJavaIoFile(), page);
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
