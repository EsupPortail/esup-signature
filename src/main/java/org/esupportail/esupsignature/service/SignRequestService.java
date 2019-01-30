package org.esupportail.esupsignature.service;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequest.SignRequestStatus;
import org.springframework.stereotype.Service;

@Service
public class SignRequestService {
	
	public SignRequest createSignRequest(String eppn, Document document, Map<String, String> params, String recipientEmail) throws IOException {
		
		SignRequest signRequest = new SignRequest();
		signRequest.setName(eppn + "-" + document.getFileName());
    	signRequest.setCreateBy(eppn);
    	signRequest.setCreateDate(new Date());
		signRequest.setOriginalFile(document);
		signRequest.setSignedFile(null);
		signRequest.setStatus(SignRequestStatus.pending);
		signRequest.setParams(params);
		signRequest.setRecipientEmail(recipientEmail);
        signRequest.persist();
        return signRequest;
	}
	
}
