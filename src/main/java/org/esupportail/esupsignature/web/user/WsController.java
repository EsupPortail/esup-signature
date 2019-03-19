package org.esupportail.esupsignature.web.user;

import java.io.IOException;
import java.text.ParseException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping(value = "/ws/")
public class WsController {

	private static final Logger logger = LoggerFactory.getLogger(WsController.class);

	@Resource
	SignRequestService signRequestService;
	
	@Resource
	SignBookService signBookService;

	@Resource
	DocumentService documentService;
	
	//TODO creation / recupération de demandes par WS + declenchement d'evenements
	
	@RequestMapping(value = "/create-sign-request", method = RequestMethod.POST)
	public void createSignRequest(MultipartFile file, @RequestParam String signBookName, HttpServletRequest httpServletRequest) throws IOException, ParseException {
		SignBook signBook = SignBook.findSignBooksByNameEquals(signBookName).getSingleResult();
		long[] signBookIds = {signBook.getId()};
		User user = getSystemUser();
		user.setIp(httpServletRequest.getRemoteAddr());
		if(file != null) {
			Document document = documentService.addFile(file, file.getOriginalFilename());
			signRequestService.createSignRequest(new SignRequest(), user, document, signBook.getSignRequestParams(), signBookIds);
			logger.info(file.getOriginalFilename() + "was added into signbook" + signBookName);
			
		}

	}
	
	@ResponseBody
	@RequestMapping(value = "/count-signed-in-signbook", produces = "text/html")
	public String countSignedInSignBook(@RequestParam String signBookName, HttpServletRequest httpServletRequest) throws IOException, ParseException {
		SignBook signBook = SignBook.findSignBooksByNameEquals(signBookName).getSingleResult();
		return String.valueOf(signBook.getSignRequests().stream().filter(s -> s.getStatus().equals(SignRequestStatus.signed)).count());
	}
	
	//TODO : move signRequest to signBook
	
	public User getSystemUser() {
		User user = new User();
		user.setEppn("System");
		return user;
	}
}
