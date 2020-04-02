package org.esupportail.esupsignature.service;

import org.apache.commons.beanutils.BeanUtils;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class DataService {
	
	private static final Logger logger = LoggerFactory.getLogger(DataService.class);

	@Resource
	private DataRepository dataRepository;

	@Resource
	private FormRepository formRepository;

	@Resource
	private UserPropertieService userPropertieService;

	@Resource
	private SignRequestService signRequestService;

	@Resource
	private UserService userService;

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

	public boolean preAuthorizeUpdate(Long id) {
		Data data = dataRepository.findById(id).get();
		if (data.getCreateBy().equals(userService.getCurrentUser().getEppn()) || data.getOwner().equals(userService.getCurrentUser().getEppn())) {
			return true;
		}
		return false;
	}

	public void delete(Data data) {
		if(data.getSignBook() != null) {
			signBookService.delete(data.getSignBook());
		}
		data.setForm(null);
		dataRepository.delete(data);
	}

	public void reset(Data data) {
		data.setStatus(SignRequestStatus.draft);
		data.setSignBook(null);
		dataRepository.save(data);
	}

	public SignBook sendForSign(Data data, List<String> recipientEmails, List<String> targetEmails, User user) throws EsupSignatureException, EsupSignatureIOException {
		if(recipientEmails == null) {
			recipientEmails = new ArrayList<>();
		}
		Form form = data.getForm();
		if(form.getTargetType().equals(DocumentIOType.mail)) {
			if(targetEmails == null || targetEmails.size() == 0) {
				throw new EsupSignatureException("Target email empty");
			}
			String targetUrl = String.join(",", targetEmails);
			userPropertieService.createTargetPropertie(user, targetUrl, form);
		}
		String name = form.getTitle().replaceAll("[\\\\/:*?\"<>|]", "-");
		if(!data.getName().isEmpty()) {
			name += "_" + data.getName().replaceAll("[\\\\/:*?\"<>|]", "-");
		}
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
		Workflow workflow = getWorkflowByDataAndUser(data, recipientEmails, user);
		workflow.setName(workflow.getName() + "_" + form.getName());
//		workflow.getWorkflowSteps().addAll(workflowSteps);
		signBookService.importWorkflow(signBook, workflow);
		signBookService.nextWorkFlowStep(signBook);
		if(form.getTargetType() != null && !form.getTargetType().equals(DocumentIOType.none)) {
			signBook.setTargetType(form.getTargetType());
			signBook.setDocumentsTargetUri(targetEmails.get(0));
		}
		signBookRepository.save(signBook);
		signBookService.pendingSignBook(signBook, user);
		data.setSignBook(signBook);
		data.setStatus(SignRequestStatus.pending);
		return signBook;
	}

	public Workflow getWorkflowByDataAndUser(Data data, List<String> recipientEmails, User user) {
		DefaultWorkflow workflow = workflowService.getWorkflowByClassName(data.getForm().getWorkflowType());
		try {
			DefaultWorkflow defaultWorkflow = (DefaultWorkflow) BeanUtils.cloneBean(workflow);
			List<WorkflowStep> workflowSteps = workflow.generateWorkflowSteps(user, data, recipientEmails);
			defaultWorkflow.initWorkflowSteps();
			defaultWorkflow.getWorkflowSteps().addAll(workflowSteps);
			if(recipientEmails != null) {
				int step = 1;
				for (WorkflowStep workflowStep : workflowSteps) {
					userPropertieService.createUserPropertie(user, step, workflowStep, data.getForm());
					step++;
				}
			}
			return defaultWorkflow;
		} catch (Exception e) {
			logger.error("bean cloning fail", e);
		}
		return null;
	}

	public Data cloneData(Data data) {
		Form form = formRepository.findFormByNameAndActiveVersion(data.getForm().getName(), true).get(0);
		Data cloneData = new Data();
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmm");
		cloneData.setName(format.format(new Date()) + "_" + form.getTitle());
		cloneData.setStatus(SignRequestStatus.draft);
		cloneData.setCreateBy(userService.getCurrentUser().getEppn());
		cloneData.setCreateDate(new Date());
		cloneData.setOwner(data.getOwner());
		cloneData.getDatas().putAll(data.getDatas());
		cloneData.setForm(form);
		dataRepository.save(cloneData);
		return cloneData;
	}

	public InputStream generateFile(Data data) {
		Form form = data.getForm();
		return pdfService.fill(form.getDocument().getInputStream(), data.getDatas());
	}

	public Data getDataFromSignRequest(SignRequest signRequest) {
		if(signRequest.getParentSignBook() != null) {
			return getDataFromSignBook(signRequest.getParentSignBook());
		}
		return null;
	}

	public Data getDataFromSignBook(SignBook signBook) {
		List<Data> datas = dataRepository.findBySignBook(signBook);
		if(datas.size() > 0) {
			return datas.get(0);
		}
		return null;
	}

}