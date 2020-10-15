package org.esupportail.esupsignature.web.controller.user;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.DocumentRepository;
import org.esupportail.esupsignature.repository.FormRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.SignRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RequestMapping("/user/documents")
@Controller
@Transactional
public class DocumentController {
	
	private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

	@Resource
	private SignRequestRepository signRequestRepository;

	@Resource
	private DocumentRepository documentRepository;

	@Resource
	private FormRepository formRepository;

	@Resource
	private SignRequestService signRequestService;

    @GetMapping(value = "/getfile/{id}")
	public ResponseEntity<Void> getFile(@ModelAttribute("user") User user, @ModelAttribute("authUser") User authUser, @PathVariable("id") Long id, HttpServletResponse response) throws IOException {

		Document document = documentRepository.findById(id).get();
		if(document.equals(user.getKeystore())) {
			return getDocumentResponseEntity(response, document);
		}
		Long nbSignRequest = signRequestRepository.countById(document.getParentId());
		if(nbSignRequest > 0 && signRequestService.checkUserViewRights(user, authUser, signRequestRepository.findById(document.getParentId()).get())) {
			return getDocumentResponseEntity(response, document);
		}
		Long nbForm = formRepository.countById(document.getParentId());
		if(nbForm > 0) {
			return getDocumentResponseEntity(response, document);
		}
		logger.warn(user.getEppn() + " try to access " + id + " without view rights");
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	private ResponseEntity<Void> getDocumentResponseEntity(HttpServletResponse response, Document document) throws IOException {
		response.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(document.getFileName(), StandardCharsets.UTF_8.toString()));
		response.setContentType(document.getContentType());
		IOUtils.copy(document.getInputStream(), response.getOutputStream());
		return new ResponseEntity<>(HttpStatus.OK);
	}


}
