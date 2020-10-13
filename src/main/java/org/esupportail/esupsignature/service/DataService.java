package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.extvalue.ExtValue;
import org.esupportail.esupsignature.service.extvalue.ExtValueService;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.esupportail.esupsignature.service.prefill.PreFill;
import org.esupportail.esupsignature.service.prefill.PreFillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
public class DataService {

    private static final Logger logger = LoggerFactory.getLogger(DataService.class);

    @Resource
    private DataRepository dataRepository;

    @Resource
    private FormRepository formRepository;

    @Resource
    private PreFillService preFillService;

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

    public SignBook sendForSign(Data data, List<String> recipientEmails, List<String> targetEmails, User user) throws EsupSignatureException, EsupSignatureIOException, InterruptedException {
        if (recipientEmails == null) {
            recipientEmails = new ArrayList<>();
        }
        Form form = data.getForm();
        for(Field field : form.getFields()) {
            if("default".equals(field.getExtValueServiceName()) && "system".equals(field.getExtValueType())) {
                if(field.getExtValueReturn().equals("id")) {
                    data.getDatas().put("id", data.getId().toString());
                }
            }
        }
        if (form.getTargetType().equals(DocumentIOType.mail)) {
            if (targetEmails == null || targetEmails.size() == 0) {
                throw new EsupSignatureException("Target email empty");
            }
            String targetUrl = String.join(",", targetEmails);
            userPropertieService.createTargetPropertie(user, targetUrl, form);
        }
        String name = form.getTitle().replaceAll("[\\\\/:*?\"<>|]", "-");
        Workflow workflow = workflowService.getWorkflowByDataAndUser(data, recipientEmails, user);
        workflow.setName(workflow.getName() + "_" + form.getName());
        SignBook signBook = signBookService.createSignBook(form.getTitle(), name, user, false);
        String docName = user.getFirstname().substring(0, 1).toUpperCase();
        docName += user.getName().substring(0, 1).toUpperCase();
        SignRequest signRequest = signRequestService.createSignRequest(signBookService.generateName(name, docName, user), user);
        signRequestService.addDocsToSignRequest(signRequest, fileService.toMultipartFile(generateFile(data), name + ".pdf", "application/pdf"));
        signRequestRepository.save(signRequest);
        signBookService.addSignRequest(signBook, signRequest);
        signBookService.importWorkflow(signBook, workflow);
        signBookService.nextWorkFlowStep(signBook);
        if (form.getTargetType() != null && !form.getTargetType().equals(DocumentIOType.none)) {
            signBook.setTargetType(form.getTargetType());
            if(form.getTargetType().equals(DocumentIOType.mail)) {
                signBook.setDocumentsTargetUri(targetEmails.get(0));
            } else {
                signBook.setDocumentsTargetUri(form.getTargetUri());
            }
        }
        signBookRepository.save(signBook);
        signBookService.pendingSignBook(signBook, user);
        data.setSignBook(signBook);
        data.setStatus(SignRequestStatus.pending);
        return signBook;
    }



    public Data updateData(@RequestParam MultiValueMap<String, String> formDatas, User user, Form form, Data data) {

        List<Field> fields = preFillService.getPreFilledFieldsByServiceName(form.getPreFillType(), form.getFields(), user);

        for(Field field : fields) {
            if(field.getExtValueType() != null && field.getExtValueType().equals("system")) {
                formDatas.add(field.getName(), field.getDefaultValue());
            }
        }

        for(String savedDataKeys : data.getDatas().keySet()) {
            if(!formDatas.containsKey(savedDataKeys)) {
                formDatas.put(savedDataKeys, Collections.singletonList(""));
            }
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        data.setName(form.getTitle() + "_" + format.format(new Date()));
        data.getDatas().putAll(formDatas.toSingleValueMap());
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
        if(form.getDocument() != null) {
            return pdfService.fill(form.getDocument().getInputStream(), data.getDatas());
        } else {
            try {
                return pdfService.generatePdfFromData(data);
            } catch (IOException e) {
                logger.error("pdf generation error", e);
            }
        }
        return null;
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