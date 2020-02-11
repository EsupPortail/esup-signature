package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.mail.MailService;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.esupportail.esupsignature.service.sign.SignService;
import org.esupportail.esupsignature.web.JsonSignInfoMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
	private RecipientService recipientService;

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
	
	public List<Data> getAllDatas(){
		List<Data> list = new ArrayList<Data>();
		dataRepository.findAll().forEach(e -> list.add(e));
		return list;
	}

	public void updateData(Data data) {
		dataRepository.save(data);
	}
	
	public void deleteData(Long dataId) {
		dataRepository.delete(getDataById(dataId));
	}

	public void reset(Data data) {
		data.setStatus(SignRequestStatus.draft);
		data.setSignRequestToken(null);
		data.setSignBookName(null);
		dataRepository.save(data);
	}

	public void nextStep(Data data, List<String> recipientEmails, User user) {
		List<WorkflowStep> workflowSteps = getWorkflowSteps(data, recipientEmails, user);
		WorkflowStep workflowStep = workflowSteps.get(data.getStep() - 1);
		SignRequest signRequest = signRequestRepository.findByToken(data.getSignRequestToken()).get(0);
		signRequestService.pendingSignRequest(signRequest, workflowStep.getSignType(), workflowStep.getAllSignToComplete(), user);
		data.setStatus(SignRequestStatus.pending);
		dataRepository.save(data);
	}

	public void sendForSign(Data data, List<String> recipientEmails, List<String> targetEmails, User user) throws EsupSignatureException, EsupSignatureIOException {
		Form form = data.getForm();
		if(form.getTargetType().equals(DocumentIOType.mail)) {
			if(targetEmails == null || targetEmails.size() == 0) {
				throw new EsupSignatureException("Target email empty");
			}
			String targetUrl = String.join(",", targetEmails);
			userPropertieService.createTargetPropertie(user, targetUrl, form);
		}
		String name = data.getName().replaceAll("[\\\\/:*?\"<>|]", "-");
		List<WorkflowStep> workflowSteps = getWorkflowSteps(data, recipientEmails, user);
		Workflow workflow = new Workflow();
		workflow.setName(form.getName());
		workflow.getWorkflowSteps().addAll(workflowSteps);
		data.setStep(1);
		SignBook signBook = signBookService.createSignBook(name, user, false);
		SignRequest signRequest = signRequestService.createSignRequest(name, user);
		signRequestService.addDocsToSignRequest(signRequest, fileService.toMultipartFile(generateFile(data), name, "application/pdf"));
		signBookService.importWorkflow(signBook, workflow);
		signBookService.addSignRequest(signBook, signRequest);
		String token = signRequest.getToken();
		logger.info(token);
		data.setSignRequestToken(token);
		data.setStatus(SignRequestStatus.pending);
		dataRepository.save(data);
	}

	public List<WorkflowStep> getWorkflowSteps(Data data, List<String> recipientEmails, User user) {
		Workflow workflow = workflowService.getWorkflowByClassName(data.getForm().getWorkflowType());
		List<WorkflowStep> workflowSteps = new ArrayList<>();
		if(workflow.getWorkflowSteps(data, recipientEmails).size() > 0 ) {
			workflowSteps = workflow.getWorkflowSteps(data, recipientEmails);
			workflowSteps = workflowSteps.stream().sorted(Comparator.comparingInt(WorkflowStep::getStepNumber)).collect(Collectors.toList());
			int step = 1;
			for(WorkflowStep workflowStep: workflowSteps) {
				userPropertieService.createRecipientPropertie(user, step, workflowStep, data.getForm());
				//signService.addWorkflowStep(name, workflowStep);
				step++;
			}
		} else {
			WorkflowStep workflowStep = new WorkflowStep();
			workflowStep.getRecipients().add(recipientService.createRecipient(data.getId(), user));
			workflowSteps.add(workflowStep);
			//signService.addWorkflowStep(name, workflowStep);

		}
		return workflowSteps;
	}

	public InputStream generateFile(Data data) {
		Form form = data.getForm();
		return pdfService.fill(form.getDocument().getInputStream(), data.getDatas());
	}
}
