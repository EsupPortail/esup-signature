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
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;

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
    private WorkflowRepository workflowRepository;

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

    public boolean preAuthorizeUpdate(Long id, User user) {
        Data data = dataRepository.findById(id).get();
        if (data.getCreateBy().equals(user.getEppn()) || data.getOwner().equals(user.getEppn())) {
            return true;
        }
        return false;
    }

    public void delete(Data data) {
        if (data.getSignBook() != null) {
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
        if (recipientEmails == null) {
            recipientEmails = new ArrayList<>();
        }
        Form form = data.getForm();
        if (form.getTargetType().equals(DocumentIOType.mail)) {
            if (targetEmails == null || targetEmails.size() == 0) {
                throw new EsupSignatureException("Target email empty");
            }
            String targetUrl = String.join(",", targetEmails);
            userPropertieService.createTargetPropertie(user, targetUrl, form);
        }
        String name = form.getTitle().replaceAll("[\\\\/:*?\"<>|]", "-");
        SignBook signBook = signBookService.createSignBook(name, user, false);
        SignRequest signRequest = signRequestService.createSignRequest(name, user);
        signRequestService.addDocsToSignRequest(signRequest, fileService.toMultipartFile(generateFile(data), name + ".pdf", "application/pdf"));
        signRequestRepository.save(signRequest);
        signBookService.addSignRequest(signBook, signRequest);
        Workflow workflow = getWorkflowByDataAndUser(data, recipientEmails, user);
        workflow.setName(workflow.getName() + "_" + form.getName());
        signBookService.importWorkflow(signBook, workflow);
        signBookService.nextWorkFlowStep(signBook);
        if (form.getTargetType() != null && !form.getTargetType().equals(DocumentIOType.none)) {
            signBook.setTargetType(form.getTargetType());
            signBook.setDocumentsTargetUri(targetEmails.get(0));
        }
        signBookRepository.save(signBook);
        signBookService.pendingSignBook(signBook, user);
        data.setSignBook(signBook);
        data.setStatus(SignRequestStatus.pending);
        return signBook;
    }

    public Workflow getWorkflowByDataAndUser(Data data, List<String> recipientEmails, User user) throws EsupSignatureException {
        Workflow workflow;
        List<WorkflowStep> workflowSteps = new ArrayList<>();
        Workflow modelWorkflow = workflowService.getWorkflowByName(data.getForm().getWorkflowType());
        try {
            if (modelWorkflow instanceof DefaultWorkflow) {
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) BeanUtils.cloneBean(modelWorkflow);
                workflowSteps.addAll(((DefaultWorkflow) modelWorkflow).generateWorkflowSteps(user, data, recipientEmails));
                defaultWorkflow.initWorkflowSteps();
                defaultWorkflow.getWorkflowSteps().addAll(workflowSteps);
                workflow = defaultWorkflow;
            } else {
                workflow = (Workflow) BeanUtils.cloneBean(modelWorkflow);
                workflowSteps.addAll(workflow.getWorkflowSteps());
                if(recipientEmails != null) {
                    for (WorkflowStep workflowStep : workflow.getWorkflowSteps()) {
                        if (workflowStep.getChangeable()) {
                            workflowStep.getRecipients().clear();
                            List<Recipient> recipients = workflowService.getFavoriteRecipientEmail(workflowStep.getStepNumber(), data.getForm(), recipientEmails, user);
                            for (Recipient recipient : recipients) {
                                recipientRepository.save(recipient);
                                workflowStep.getRecipients().add(recipient);
                            }
                        }
                    }
                    workflowSteps.addAll(workflow.getWorkflowSteps());
                }
            }
            if (recipientEmails != null) {
                int step = 1;
                for (WorkflowStep workflowStep : workflowSteps) {
                    userPropertieService.createUserPropertie(user, step, workflowStep, data.getForm());
                    step++;
                }
            }
            for(WorkflowStep workflowStep : workflow.getWorkflowSteps()) {
                for(Recipient recipient : workflowStep.getRecipients()) {
                    if (recipient.getUser().getEppn().equals("creator")) {
                        recipient.setUser(user);
                    }
                }
            }
            return workflow;
        } catch (Exception e) {
            logger.error("workflow not found", e);
            throw new EsupSignatureException("workflow not found", e);
        }
    }

    public Data updateData(@RequestParam MultiValueMap<String, String> formData, User user, Form form, Data data) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmm");
        data.setName(format.format(new Date()) + "_" + form.getTitle());
        data.getDatas().putAll(formData.toSingleValueMap());
        data.setForm(form);
        data.setFormName(form.getName());
        data.setFormVersion(form.getVersion());
        data.setStatus(SignRequestStatus.draft);
        data.setCreateBy(userService.getUserFromAuthentication().getEppn());
        data.setOwner(user.getEppn());
        data.setCreateDate(new Date());
        dataRepository.save(data);
        return data;
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
        if (signRequest.getParentSignBook() != null) {
            return getDataFromSignBook(signRequest.getParentSignBook());
        }
        return null;
    }

    public Data getDataFromSignBook(SignBook signBook) {
        List<Data> datas = dataRepository.findBySignBook(signBook);
        if (datas.size() > 0) {
            return datas.get(0);
        }
        return null;
    }

}