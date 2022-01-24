package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFillService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.esupportail.esupsignature.web.ws.json.JsonExternalUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

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
    private UserService userService;

    @Resource
    private FieldPropertieService fieldPropertieService;

    @Resource
    private TargetService targetService;

    @Resource
    private UserPropertieService userPropertieService;

    public Data getById(Long dataId) {
        return dataRepository.findById(dataId).get();
    }

    public void delete(Long id, String userEppn) {
        Data data = getById(id);
        if (data.getSignBook() != null) {
            signBookService.delete(data.getSignBook().getId(), userEppn);
        }
        data.setForm(null);
        dataRepository.delete(data);
    }

    @Transactional
    public SignBook sendForSign(Long dataId, List<String> recipientsEmails, List<String> allSignToCompletes, List<JsonExternalUserInfo> externalUsersInfos, List<String> targetEmails, List<String> targetUrls, String userEppn, String authUserEppn, boolean forceSendEmail) throws EsupSignatureException, EsupSignatureIOException, EsupSignatureFsException {
        User user = userService.getUserByEppn(userEppn);
        User authUser = userService.getUserByEppn(authUserEppn);
        Data data = getById(dataId);
        if (recipientsEmails == null) {
            recipientsEmails = new ArrayList<>();
        }
        Form form = data.getForm();
        String name = form.getTitle().replaceAll("[\\\\/:*?\"<>|]", "-").replace("\t", "");
        Workflow modelWorkflow = data.getForm().getWorkflow();
        Workflow computedWorkflow = workflowService.computeWorkflow(modelWorkflow.getId(), recipientsEmails, allSignToCompletes, user.getEppn(), false);
        SignBook signBook = signBookService.createSignBook(form.getTitle(), form.getTitle(), modelWorkflow, form.getTitle(),null, user, false);
        SignRequest signRequest = signRequestService.createSignRequest(null, signBook.getId(), user.getEppn(), authUser.getEppn());
        InputStream inputStream = generateFile(data);
        if(computedWorkflow.getWorkflowSteps().size() == 0) {
            try {
                inputStream = pdfService.convertGS(inputStream, signRequest.getToken());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        MultipartFile multipartFile = fileService.toMultipartFile(inputStream, name + ".pdf", "application/pdf");
        signRequestService.addDocsToSignRequest(signRequest, true, 0, form.getSignRequestParams(), multipartFile);
        signBookService.importWorkflow(signBook, computedWorkflow, externalUsersInfos);
        signBookService.nextWorkFlowStep(signBook);
        Workflow workflow = workflowService.getById(form.getWorkflow().getId());
        targetService.copyTargets(workflow.getTargets(), signBook, targetEmails);
        if (targetUrls != null) {
            for (String targetUrl : targetUrls) {
                signBook.getLiveWorkflow().getTargets().add(targetService.createTarget(targetUrl));
            }
        }
        data.setSignBook(signBook);
        dataRepository.save(data);
        signRequestService.pendingSignBook(signBook.getId(), data, user.getEppn(), authUser.getEppn(), forceSendEmail);
        data.setStatus(SignRequestStatus.pending);
        for (String recipientEmail : recipientsEmails) {
            userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUser.getEppn()), Collections.singletonList(recipientEmail.split("\\*")[1]));
        }
        if(workflow.getCounter() != null) {
            workflow.setCounter(workflow.getCounter() + 1);
        } else {
            workflow.setCounter(0);
        }
        return signBook;
    }

    public Data updateDatas(Form form, Data data, @RequestParam Map<String, String> formDatas, User user, User authUser) {
        List<Field> fields = preFillService.getPreFilledFieldsByServiceName(form.getPreFillType(), form.getFields(), user, data.getSignBook().getSignRequests().get(0));
        for(Field field : fields) {
            if(!field.getStepZero()) {
                field.setDefaultValue("");
            }
            if (field.getFavorisable()) {
                fieldPropertieService.createFieldPropertie(authUser, field, formDatas.get(field.getName()));
            }
            if(field.getExtValueType() != null && field.getExtValueType().equals("system") && !field.getDefaultValue().isEmpty()) {
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
        data.setUpdateBy(authUser);
        data.setUpdateDate(new Date());
        dataRepository.save(data);
        return data;
    }

    public Data cloneData(Data data, String authUserEppn) {
        User authUser = userService.getUserByEppn(authUserEppn);
        Form form = formService.getFormByNameAndActiveVersion(data.getForm().getName(), true).get(0);
        Data cloneData = new Data();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmm");
        cloneData.setName(format.format(new Date()) + "_" + form.getTitle());
        cloneData.setStatus(SignRequestStatus.draft);
        cloneData.setCreateBy(authUser);
        cloneData.setCreateDate(new Date());
        cloneData.getDatas().putAll(data.getDatas());
        cloneData.setForm(form);
        dataRepository.save(cloneData);
        return cloneData;
    }

    public InputStream generateFile(Data data) {
        Form form = data.getForm();
        if(form.getDocument() != null) {
            return pdfService.fill(form.getDocument().getInputStream(), data.getDatas(), false);
        } else {
            try {
                return pdfService.generatePdfFromData(data);
            } catch (IOException e) {
                logger.error("pdf generation error", e);
            }
        }
        return null;
    }

    public List<Field> getPrefilledFields(Form form, User user, SignRequest signRequest) {
        List<Field> prefilledFields;
        if (form.getPreFillType() != null && !form.getPreFillType().isEmpty()) {
            List<Field> fields = new ArrayList<>(form.getFields());
            prefilledFields = preFillService.getPreFilledFieldsByServiceName(form.getPreFillType(), fields, user, signRequest);
            for (Field field : prefilledFields) {
                if(field.getName().equals("Su_DateSign")) {
                    logger.info("test");
                }
                if(!field.getStepZero()) {
                    field.setDefaultValue("");
                }
            }
        } else {
            prefilledFields = form.getFields();
        }
        return prefilledFields;
    }

    @Transactional
    public SignBook initSendData(Long dataId, List<String> targetEmails, List<String> recipientEmails, List<String> allSignToCompletes, String userEppn, String authUserEppn) throws EsupSignatureIOException, EsupSignatureException {
        Data data = getById(dataId);
        if(data.getStatus().equals(SignRequestStatus.draft)) {
            try {
                SignBook signBook = sendForSign(dataId, recipientEmails, allSignToCompletes, null, targetEmails, null, userEppn, authUserEppn, false);
                if(signBook.getStatus().equals(SignRequestStatus.pending)) {
                    signBook.setComment("La procédure est démarrée");
                } else {
                    signBook.setComment("Le document est prêt");
                }
                return signBook;
            } catch (EsupSignatureException | EsupSignatureFsException e) {
                logger.error(e.getMessage(), e);
                throw new EsupSignatureException(e.getMessage(), e);
            }
        } else {
            throw new EsupSignatureException("Attention, la procédure est déjà démarrée");
        }
    }

    public List<Field> setFieldsDefaultsValues(Data data, Form form, User user) {
        List<Field> fields = getPrefilledFields(form, user, data.getSignBook().getSignRequests().get(0));
        for (Field field : fields) {
            if(data.getDatas().get(field.getName()) != null && !data.getDatas().get(field.getName()).isEmpty()) {
                field.setDefaultValue(data.getDatas().get(field.getName()));
            }
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

    @Transactional
    public Data addData(Long id, String authUserEppn) {
        User authUser = userService.getUserByEppn(authUserEppn);
        Form form = formService.getById(id);
        Data data = new Data();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        data.setName(form.getTitle() + "_" + format.format(new Date()));
        data.setForm(form);
        data.setFormName(form.getName());
        data.setFormVersion(form.getVersion());
        data.setStatus(SignRequestStatus.draft);
        data.setCreateBy(authUser);
        data.setCreateDate(new Date());
        dataRepository.save(data);
        return data;
    }

}