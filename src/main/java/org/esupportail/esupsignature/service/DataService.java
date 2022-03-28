package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFillService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
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
    private PdfService pdfService;

    @Resource
    private UserService userService;

    @Resource
    private FieldPropertieService fieldPropertieService;

    public Data getById(Long dataId) {
        return dataRepository.findById(dataId).get();
    }

    public Data getBySignRequest(SignRequest signRequest) {
        return getBySignBook(signRequest.getParentSignBook());
    }

    public Data getBySignBook(SignBook signBook) {
        return dataRepository.findBySignBook(signBook);
    }

    public void deleteBySignBook(SignBook signBook) {
        Data data = getBySignBook(signBook);
        if(data != null) {
            data.setForm(null);
            dataRepository.delete(data);
        }
    }

    public Data updateDatas(Form form, Data data, Map<String, String> formDatas, User user, User authUser) {
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

        for(Map.Entry<String, String> stringStringEntry : formDatas.entrySet()) {

            logger.info("adding : " + stringStringEntry.getKey() + " = " + stringStringEntry.getValue());

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
    public Data addData(Long formId, String authUserEppn) {
        User authUser = userService.getUserByEppn(authUserEppn);
        Form form = formService.getById(formId);
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

    public void deleteOnlyData(Long id) {
        Data data = dataRepository.findById(id).get();
        data.setForm(null);
        dataRepository.delete(data);
    }

}