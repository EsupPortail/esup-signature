package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.esupportail.esupsignature.service.workflow.DefaultWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataService {
	
	private static final Logger logger = LoggerFactory.getLogger(DataService.class);

	@Resource
	private DataRepository dataRepository;

	@Resource
	private UserPropertieService userPropertieService;

	@Resource
	private SignRequestService signRequestService;

	@Resource
	private SignBookService signBookService;

	@Resource
	private SignRequestRepository signRequestRepository;

	@Resource
	private SignBookRepository signBookRepository;

	@Resource
	private WorkflowStepRepository workflowStepRepository;

	@Resource
	private RecipientRepository recipientRepository;

	@Resource
	private FileService fileService;

	@Resource
	private PdfService pdfService;

	@Resource
	private WorkflowService workflowService;

	public Data getDataById(Long dataId) {
		Data obj = dataRepository.findById(dataId).get();
		return obj;
	}	

	public void delete(Data data) {
		if(data.getSignBook() != null) {
			signBookService.delete(data.getSignBook());
		}
		dataRepository.delete(data);
	}

	public void reset(Data data) {
		data.setStatus(SignRequestStatus.draft);
		data.setSignBook(null);
		data.setSignBookName(null);
		dataRepository.save(data);
	}

	public SignBook sendForSign(Data data, List<String> recipientEmails, List<String> targetEmails, User user) throws EsupSignatureException, EsupSignatureIOException {
		Form form = data.getForm();
		if(form.getTargetType().equals(DocumentIOType.mail)) {
			if(targetEmails == null || targetEmails.size() == 0) {
				throw new EsupSignatureException("Target email empty");
			}
			String targetUrl = String.join(",", targetEmails);
			userPropertieService.createTargetPropertie(user, targetUrl, form);
		}
		String name = form.getName().replaceAll("[\\\\/:*?\"<>|]", "-") + "_" + data.getName().replaceAll("[\\\\/:*?\"<>|]", "-");
		String signBookName = signBookService.generateName(name, user);
		SignBook signBook = signBookService.createSignBook(signBookName, user, false);
		SignRequest signRequest = signRequestService.createSignRequest(name, user);
		signRequestService.addDocsToSignRequest(signRequest, fileService.toMultipartFile(generateFile(data), name + ".pdf", "application/pdf"));
		signRequestRepository.save(signRequest);
		signBookService.addSignRequest(signBook, signRequest);
//		List<WorkflowStep> workflowSteps = getWorkflowSteps(data, recipientEmails, user);
//		for(WorkflowStep workflowStep: workflowSteps)  {
//			for(Recipient recipient : workflowStep.getRecipients()) {
//				recipient.setParentId(signRequest.getId());
//				recipientRepository.save(recipient);
//			}
//		}
		Workflow workflow = getWorkflowSteps(data, recipientEmails, user);
		workflow.setName(workflow.getName() + "_" + form.getName());
//		workflow.getWorkflowSteps().addAll(workflowSteps);
		signBookService.importWorkflow(signBook, workflow);
		signBookService.nextWorkFlowStep(signBook);
		signBookRepository.save(signBook);
		signBookService.pendingSignBook(signBook, user);
		data.setSignBook(signBook);
		data.setStatus(SignRequestStatus.pending);
		return signBook;
	}

	public Workflow getWorkflowSteps(Data data, List<String> recipientEmails, User user) {
		DefaultWorkflow workflow = workflowService.getWorkflowByClassName(data.getForm().getWorkflowType());
		workflow.generateWorkflowSteps(user, data, recipientEmails);
		int step = 1;
		for(WorkflowStep workflowStep: workflow.getWorkflowSteps())  {
			for(Recipient recipient : workflowStep.getRecipients()) {
				recipientRepository.save(recipient);
			}
			workflowStepRepository.save(workflowStep);
			//userPropertieService.createUserPropertie(user, step, workflowStep, data.getForm());
			step++;
		}
		return workflow;
	}

	public InputStream generateFile(Data data) {
		Form form = data.getForm();
		return pdfService.fill(form.getDocument().getInputStream(), data.getDatas());
	}

}
