package org.esupportail.esupsignature.service.export;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.repository.FormRepository;
import org.esupportail.esupsignature.service.DataService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.utils.WebUtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DataExportService {

    private static final Logger logger = LoggerFactory.getLogger(DataExportService.class);

    @Resource
    private DataRepository dataRepository;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private DataService dataService;

    @Resource
    private FormRepository formRepository;

    @Resource
    private WebUtilsService webUtilsService;

    @Transactional(readOnly = true)
    public InputStream getCsvDatasFromForms(List<Workflow> workflows) throws IOException {
        return webUtilsService.mapListToCSV(getDatasToExport(workflows));
    }

    @Transactional(readOnly = true)
    public InputStream getCsvDatasFromForm(Form form) throws IOException {
        return webUtilsService.mapListToCSV(new ArrayList<>(getDatasToExport(form)));
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> getDatasToExport(List<Workflow> workflows) {
        List<Map<String, String>> dataDatas = new ArrayList<>();
        for(Workflow workflow: workflows) {
            dataDatas.addAll(getDatasToExport(workflow));
        }
        return  dataDatas;
    }

    @Transactional
    public List<Map<String, String>> getDatasToExportByFormName(String name) {
        List<Form> forms = formRepository.findFormByNameAndDeletedIsNullOrDeletedIsFalse(name);
        if (!forms.isEmpty()) {
            try {
                return getDatasToExport(forms.stream().map(Form::getWorkflow).collect(Collectors.toList()));
            } catch (Exception e) {
                logger.error("get file error", e);
            }
        } else {
            logger.warn("form " + name + " not found");
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<LinkedHashMap<String, String>> getDatasToExport(Workflow workflow) {
        List<Form> forms = formRepository.findByWorkflowIdEquals(workflow.getId());
        if(forms.isEmpty()) {
            return new ArrayList<>();
        }
        return getDatasToExport(forms.get(0));
    }

    @Transactional(readOnly = true)
    public List<LinkedHashMap<String, String>> getDatasToExport(Form form) {
        List<Data> datas = dataRepository.findByFormId(form.getId());
        List<LinkedHashMap<String, String>> dataDatas = new ArrayList<>();
        for(Data data : datas) {
            SignBook signBook = data.getSignBook();
            LinkedHashMap<String, String> toExportDatas = getToExportDatas(data, signBook);
            dataDatas.add(toExportDatas);
        }
        return dataDatas;
    }

    @Transactional
    public LinkedHashMap<String, String> getJsonDatasFromSignRequest(Long id) {
        SignRequest signRequest = signRequestService.getById(id);
        if(signRequest != null && signRequest.getParentSignBook() != null) {
            Data data = dataService.getBySignRequest(signRequest);
            if(data != null) {
                return getToExportDatas(data, signRequest.getParentSignBook());
            } else {
                logger.warn("signRequest " + id + " doesn't have any data");
            }
        } else {
            logger.warn("signRequest " + id + " doesn't exist");
        }
        return null;
    }

    private LinkedHashMap<String, String> getToExportDatas(Data data, SignBook signBook) {
        LinkedHashMap<String, String> toExportDatas = new LinkedHashMap<>();
        SignRequest firstSignRequest = signBook != null && !signBook.getSignRequests().isEmpty() ? signBook.getSignRequests().get(0) : null;
        if(signBook != null) {
            toExportDatas.put("sign_request_id", firstSignRequest != null ? firstSignRequest.getId().toString() : "");
            toExportDatas.put("sign_request_attachements_size", firstSignRequest != null ? String.valueOf(firstSignRequest.getAttachments().size()) : "");
            toExportDatas.put("sign_request_current_status", firstSignRequest != null ? firstSignRequest.getStatus().toString() : "");
        } else {
            toExportDatas.put("sign_request_id", "");
            toExportDatas.put("sign_request_attachements_size", "");
            toExportDatas.put("sign_request_current_status", "");
        }
        toExportDatas.put("form_name", data.getForm().getName());
        toExportDatas.put("form_desc", data.getForm().getDescription());
        toExportDatas.put("form_create_date", data.getCreateDate().toString());
        toExportDatas.put("form_create_by", data.getCreateBy().getEppn());
        toExportDatas.put("form_current_status", data.getStatus().name());
        Map<Recipient, Action> recipientHasSigned = Collections.emptyMap();
        if(firstSignRequest != null) {
            recipientHasSigned = Optional.ofNullable(firstSignRequest.getRecipientHasSigned()).orElse(Collections.emptyMap());
        }
        if(signBook != null) {
            if (!recipientHasSigned.isEmpty() && (signBook.getStatus().equals(SignRequestStatus.completed) || signBook.getStatus().equals(SignRequestStatus.exported))) {
                Optional<Action> lastActionHero = recipientHasSigned.values().stream().filter(action -> !action.getActionType().equals(ActionType.none)).findFirst();
                if (lastActionHero.isPresent()) {
                    toExportDatas.put("form_completed_date", lastActionHero.get().getDate().toString());
                    toExportDatas.put("form_completed_by", recipientHasSigned.entrySet().stream()
                            .filter(entry -> lastActionHero.get().equals(entry.getValue()))
                            .map(Map.Entry::getKey)
                            .map(Recipient::getUser)
                            .filter(Objects::nonNull)
                            .map(User::getEppn)
                            .findFirst()
                            .orElse(""));
                } else {
                    toExportDatas.put("form_completed_date", "");
                    toExportDatas.put("form_completed_by", "");
                }
            } else {
                toExportDatas.put("form_completed_date", "");
                toExportDatas.put("form_completed_by", "");
            }
        } else {
            toExportDatas.put("form_completed_date", "");
            toExportDatas.put("form_completed_by", "");
        }
        for(Field field : data.getForm().getFields()) {
            toExportDatas.put("form_data_" + field.getName(), data.getDatas().get(field.getName()));
        }
        if(signBook != null) {
            if (signBook.getLiveWorkflow().getCurrentStep() != null && signBook.getLiveWorkflow().getCurrentStep().getWorkflowStep() != null) {
                toExportDatas.put("current_step_id", signBook.getLiveWorkflow().getCurrentStep().getWorkflowStep().getId().toString());
                toExportDatas.put("current_step_description", signBook.getLiveWorkflow().getCurrentStep().getWorkflowStep().getDescription());
            } else {
                toExportDatas.put("current_step_id", "");
                toExportDatas.put("current_step_description", "");
            }
            int step = 1;
            List<Map.Entry<Recipient, Action>> actionsList = recipientHasSigned.entrySet().stream().filter(recipientActionEntry -> !recipientActionEntry.getValue().getActionType().equals(ActionType.none) && recipientActionEntry.getValue().getDate() != null).sorted(Comparator.comparing(o -> o.getValue().getDate())).toList();
            for (Map.Entry<Recipient, Action> actions : actionsList) {
                toExportDatas.put("sign_step_" + step + "_user_eppn", actions.getKey().getUser().getEppn());
                toExportDatas.put("sign_step_" + step + "_type", actions.getValue().getActionType().name());
                toExportDatas.put("sign_step_" + step + "_date", actions.getValue().getDate().toString());
                step++;
            }
        }
        return toExportDatas;
    }

}
