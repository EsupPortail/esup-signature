package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFillService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class DataService {

    private static final Logger logger = LoggerFactory.getLogger(DataService.class);

    @Resource
    private DataRepository dataRepository;

    @Resource
    private FormService formService;

    @Resource
    private PreFillService preFillService;

    @Resource
    private UserPropertieService userPropertieService;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private FileService fileService;

    @Resource
    private PdfService pdfService;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private UserShareService userShareService;

    @Resource
    private UserService userService;

    public Data getById(Long dataId) {
        return dataRepository.findById(dataId).get();
    }

    public void delete(Data data) {
        if (data.getSignBook() != null) {
            signBookService.delete(data.getSignBook());
        }
        data.setForm(null);
        dataRepository.delete(data);
    }

    public SignBook sendForSign(Data data, List<String> recipientEmails, List<String> targetEmails, User user, User authUser) throws EsupSignatureException, EsupSignatureIOException {
        if (recipientEmails == null) {
            recipientEmails = new ArrayList<>();
        }
        Form form = data.getForm();
        if (form.getTargetType().equals(DocumentIOType.mail)) {
            if (targetEmails == null || targetEmails.size() == 0) {
                throw new EsupSignatureException("Target email empty");
            }
            String targetUrl = String.join(",", targetEmails);
            userPropertieService.createTargetPropertie(user, workflowService.getWorkflowByName(form.getWorkflowType()).getWorkflowSteps().get(0), targetUrl);
        }
        String name = form.getTitle().replaceAll("[\\\\/:*?\"<>|]", "-").replace("\t", "");
        Workflow modelWorkflow = workflowService.getWorkflowByName(data.getForm().getWorkflowType());
        Workflow computedWorkflow = workflowService.computeWorkflow(modelWorkflow.getId(), recipientEmails, user.getEppn(), false);
        SignBook signBook = signBookService.createSignBook(form.getTitle(), "", user, false);
        String docName = user.getFirstname().substring(0, 1).toUpperCase();
        docName += user.getName().substring(0, 1).toUpperCase();
        SignRequest signRequest = signRequestService.createSignRequest(signBookService.generateName(name, docName, user.getEppn()), signBook, user.getEppn(), authUser.getEppn());
        signBookService.importWorkflow(signBook, computedWorkflow);
        InputStream inputStream = generateFile(data);
        if(signBook.getLiveWorkflow().getLiveWorkflowSteps().size() == 0) {
            try {
                inputStream = pdfService.convertGS(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        MultipartFile multipartFile = fileService.toMultipartFile(inputStream, name + ".pdf", "application/pdf");
        signRequestService.addDocsToSignRequest(signRequest, multipartFile);
        workflowService.saveProperties(user, modelWorkflow, computedWorkflow);
        signBookService.nextWorkFlowStep(signBook);
        if (form.getTargetType() != null && !form.getTargetType().equals(DocumentIOType.none)) {
            signBook.getLiveWorkflow().setTargetType(form.getTargetType());
            if(form.getTargetType().equals(DocumentIOType.mail)) {
                signBook.getLiveWorkflow().setDocumentsTargetUri(targetEmails.get(0));
            } else {
                signBook.getLiveWorkflow().setDocumentsTargetUri(form.getTargetUri());
            }
        }
        data.setSignBook(signBook);
        dataRepository.save(data);
        signBookService.pendingSignBook(signBook, data, user.getEppn(), authUser.getEppn());
        data.setStatus(SignRequestStatus.pending);
        return signBook;
    }

    public Data updateDatas(Form form, Data data, @RequestParam Map<String, String> formDatas, User user, User authUser) {
        List<Field> fields = preFillService.getPreFilledFieldsByServiceName(form.getPreFillType(), form.getFields(), user);

        for(Field field : fields) {
            if(field.getExtValueType() != null && field.getExtValueType().equals("system")) {
                formDatas.put(field.getName(), field.getDefaultValue());
            }
        }

        for(String savedDataKeys : data.getDatas().keySet()) {
            if(!formDatas.containsKey(savedDataKeys)) {
                formDatas.put(savedDataKeys, "");
            }
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        data.setName(form.getTitle() + "_" + format.format(new Date()));
        data.getDatas().putAll(formDatas);
        data.setForm(form);
        data.setFormName(form.getName());
        data.setFormVersion(form.getVersion());
        data.setStatus(SignRequestStatus.draft);
        data.setCreateBy(authUser.getEppn());
        data.setOwner(user.getEppn());
        data.setCreateDate(new Date());
        dataRepository.save(data);
        return data;
    }

    public Data cloneData(Data data, User authUser) {
        Form form = formService.getFormByNameAndActiveVersion(data.getForm().getName(), true).get(0);
        Data cloneData = new Data();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmm");
        cloneData.setName(format.format(new Date()) + "_" + form.getTitle());
        cloneData.setStatus(SignRequestStatus.draft);
        cloneData.setCreateBy(authUser.getEppn());
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

    public Data getBySignRequest(SignRequest signRequest) {
        return getBySignBook(signRequest.getParentSignBook());
    }

    public Data getBySignBook(SignBook signBook) {
        return dataRepository.findBySignBook(signBook);
    }

    public List<Data> getDataDraftByOwner(String userEppn) {
        User user = userService.getByEppn(userEppn);
        return dataRepository.findByOwnerAndStatus(user.getEppn(), SignRequestStatus.draft);
    }

    public Page<Data> getDatasPaged(List<Data> datas, Pageable pageable, String userEppn, String authUserEppn) {
        Page<Data> datasPage;
        if(!userEppn.equals(authUserEppn)) {
            List<Data> datasOk = new ArrayList<>();
            for(Data data : datas) {
                if(userShareService.checkFormShare(userEppn, authUserEppn, ShareType.create, data.getForm())) {
                    datasOk.add(data);
                }
            }
            datasPage = new PageImpl<>(datasOk, pageable, datas.size());
        } else {
            datasPage = new PageImpl<>(datas, pageable, datas.size());
        }
        return datasPage;
    }

    public List<Field> getPrefilledFields(Form form, User user) {
        List<Field> prefilledFields;
        if (form.getPreFillType() != null && !form.getPreFillType().isEmpty()) {
            List<Field> fields = new ArrayList<>(form.getFields());
            prefilledFields = preFillService.getPreFilledFieldsByServiceName(form.getPreFillType(), fields, user);
            for (Field field : prefilledFields) {
                if(!field.getStepNumbers().contains("0")) {
                    field.setDefaultValue("");
                }
            }
        } else {
            prefilledFields = form.getFields();
        }
        return prefilledFields;
    }

    @Transactional
    public SignBook initSendData(Long dataId, User user, List<String> recipientEmails, List<String> targetEmails, User authUser) throws EsupSignatureIOException, EsupSignatureException {
        Data data = getById(dataId);
        if(data.getStatus().equals(SignRequestStatus.draft)) {
            try {
                SignBook signBook = sendForSign(data, recipientEmails, targetEmails, user, authUser);
                if(signBook.getStatus().equals(SignRequestStatus.pending)) {
                    signBook.setComment("La procédure est démarrée");
                } else {
                    signBook.setComment("Le document est prêt");
                }
                return signBook;
            } catch (EsupSignatureException e) {
                logger.error(e.getMessage(), e);
                throw new EsupSignatureException(e.getMessage(), e);
            }
        } else {
            throw new EsupSignatureException("Attention, la procédure est déjà démarrée");
        }
    }

    public Data cloneFromSignRequest(SignRequest signRequest, User authUser) {
        Data data = getBySignRequest(signRequest);
        return cloneData(data, authUser);
    }

    public List<Field> setFieldsDefaultsValues(Data data, Form form) {
        List<Field> fields = form.getFields();
        for (Field field : fields) {
            field.setDefaultValue(data.getDatas().get(field.getName()));
        }
        return fields;
    }

    @Transactional
    public Data addData(Long id, Long dataId, Map<String, String> datas, User user, User authUser) {
        Form form = formService.getById(id);
        Data data;
        if(dataId != null) {
            data = getById(dataId);
        } else {
            data = new Data();
        }
        return updateDatas(form, data, datas, user, authUser);
    }

    public void nullifyForm(Form form) {
        List<Data> datas = dataRepository.findByForm(form);
        for(Data data : datas) {
            data.setForm(null);
        }
    }

    public void nullifySignBook(SignBook signBook) {
        Data data = getBySignBook(signBook);
        if(data != null) data.setSignBook(null);
    }

    public long getNbCreateByAndStatus(String userEppn) {
        return dataRepository.countByCreateByAndStatus(userEppn, SignRequestStatus.draft);
    }

    @Transactional
    public Map<String, Object> getModelResponse(Long formId) throws SQLException, IOException {
        Form form = formService.getById(formId);
        Document model = form.getDocument();
        if (model != null) {
            return fileService.getFileResponse(model.getBigFile().getBinaryFile().getBinaryStream().readAllBytes(), model.getFileName(), model.getContentType());
        }
        return null;
    }

}