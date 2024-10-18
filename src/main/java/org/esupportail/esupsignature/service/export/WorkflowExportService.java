package org.esupportail.esupsignature.service.export;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.WorkflowService;
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
public class WorkflowExportService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowExportService.class);

    private final WorkflowService workflowService;

    private final SignBookService signBookService;

    private final WebUtilsService webUtilsService;

    public WorkflowExportService(WorkflowService workflowService, SignBookService signBookService, WebUtilsService webUtilsService) {
        this.workflowService = workflowService;
        this.signBookService = signBookService;
        this.webUtilsService = webUtilsService;
    }

    @Transactional(readOnly = true)
    public InputStream getCsvDatasFromWorkflow(List<Workflow> workflows) throws IOException {
        return webUtilsService.mapListToCSV(getDatasToExport(workflows));
    }

    public List<Map<String, String>> getDatasToExport(List<Workflow> workflows) {
        List<Map<String, String>> dataDatas = new ArrayList<>();
        for(Workflow workflow: workflows) {
            dataDatas.addAll(getDatasToExport(workflow));
        }
        return  dataDatas;
    }

    public List<LinkedHashMap<String, String>> getDatasToExport(Workflow workflow) {
        List<LinkedHashMap<String, String>> dataDatas = new ArrayList<>();
        for(SignBook signBook : signBookService.getByWorkflowId(workflow.getId())) {
            dataDatas.add(getToExportDatas(signBook));
        }
        return dataDatas;
    }

    @Transactional(readOnly = true)
    public LinkedHashMap<String, String> getJsonDatasFromWorkflow(Long id) {
        Workflow workflow = workflowService.getById(id);
        return getDatasToExport(workflow).get(0);
    }

    private LinkedHashMap<String, String> getToExportDatas(SignBook signBook) {
        LinkedHashMap<String, String> toExportDatas = new LinkedHashMap<>();
        toExportDatas.put("sign_book_id", signBook.getId().toString());
        if(signBook.getSignRequests().size() == 1) {
            toExportDatas.put("sign_request_id", signBook.getSignRequests().get(0).getId().toString());
            toExportDatas.put("sign_request_title", signBook.getSignRequests().get(0).getTitle());
        } else {
            toExportDatas.put("sign_request_ids", signBook.getSignRequests().stream().map(SignRequest::getId).map(Object::toString).collect(Collectors.joining(",")));
            toExportDatas.put("sign_request_titles", signBook.getSignRequests().stream().map(SignRequest::getTitle).collect(Collectors.joining(",")));
        }
        toExportDatas.put("sign_book_statut", signBook.getStatus().name());
        toExportDatas.put("sign_book_create_date", signBook.getCreateDate().toString());
        toExportDatas.put("completed_date", "");
        toExportDatas.put("completed_by", "");
        Map<Recipient, Action> recipientHasSigned = signBook.getSignRequests().get(0).getRecipientHasSigned();
        try {
            if (recipientHasSigned != null && !recipientHasSigned.isEmpty() && (signBook.getStatus().equals(SignRequestStatus.completed) || signBook.getStatus().equals(SignRequestStatus.refused) || signBook.getStatus().equals(SignRequestStatus.exported) || signBook.getStatus().equals(SignRequestStatus.archived))) {
                Optional<Action> lastAction = recipientHasSigned.values().stream().filter(action -> !action.getActionType().equals(ActionType.none)).findFirst();
                if (lastAction.isPresent()) {
                    toExportDatas.put("completed_date", lastAction.get().getDate().toString());
                    toExportDatas.put("completed_by", recipientHasSigned.entrySet().stream().filter(entry -> lastAction.get().equals(entry.getValue())).map(Map.Entry::getKey).findFirst().get().getUser().getEppn());
                }
            }
        } catch (Exception e) {
            logger.error("error while getting completed date", e);
        }
        if (signBook.getLiveWorkflow().getCurrentStep() != null && signBook.getLiveWorkflow().getCurrentStep().getWorkflowStep() != null) {
            toExportDatas.put("current_step_id", signBook.getLiveWorkflow().getCurrentStep().getWorkflowStep().getId().toString());
            toExportDatas.put("current_step_description", signBook.getLiveWorkflow().getCurrentStep().getWorkflowStep().getDescription());
        }
        int step = 1;
        List<Map.Entry<Recipient, Action>> actionsList = recipientHasSigned.entrySet().stream().filter(recipientActionEntry -> !recipientActionEntry.getValue().getActionType().equals(ActionType.none) && recipientActionEntry.getValue().getDate() != null).sorted(Comparator.comparing(o -> o.getValue().getDate())).collect(Collectors.toList());
        for (Map.Entry<Recipient, Action> actions : actionsList) {
            toExportDatas.put("sign_step_" + step + "_user_eppn", actions.getKey().getUser().getEppn());
            toExportDatas.put("sign_step_" + step + "_type", actions.getValue().getActionType().name());
            toExportDatas.put("sign_step_" + step + "_date", actions.getValue().getDate().toString());
            step++;
        }
        return toExportDatas;
    }

}
