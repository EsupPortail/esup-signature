package org.esupportail.esupsignature.service.export;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkflowExportService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowExportService.class);

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private SignBookService signBookService;

    public InputStream getCsvDatasFromWorkflow(List<Workflow> workflows) throws IOException {
        return mapListToCSV(getDatasToExport(workflows));
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

    public LinkedHashMap<String, String> getJsonDatasFromSignRequest(Long id) {
        SignRequest signRequest = signRequestService.getById(id);
        if(signRequest != null && signRequest.getParentSignBook() != null) {
            Data data = signBookService.getBySignRequest(signRequest);
            if(data != null) {
                return getToExportDatas(signRequest.getParentSignBook());
            } else {
                logger.warn("signRequest " + id + " doesn't have any data");
            }
        } else {
            logger.warn("signRequest " + id + " doesn't exist");
        }
        return null;
    }

    private LinkedHashMap<String, String> getToExportDatas(SignBook signBook) {
        LinkedHashMap<String, String> toExportDatas = new LinkedHashMap<>();
        if(signBook != null) {
            toExportDatas.put("sign_request_id", signBook.getSignRequests().get(0).getId().toString());
            toExportDatas.put("sign_request_attachements_size", String.valueOf(signBook.getSignRequests().get(0).getAttachments().size()));
        } else {
            toExportDatas.put("sign_request_id", "");
            toExportDatas.put("sign_request_attachements_size", "");
        }
        Map<Recipient, Action> recipientHasSigned = null;
        if(signBook != null) {
            recipientHasSigned = signBook.getSignRequests().get(0).getRecipientHasSigned();
            if (recipientHasSigned != null && recipientHasSigned.size() > 0 && signBook.getStatus().equals(SignRequestStatus.completed) || signBook.getStatus().equals(SignRequestStatus.exported) || signBook.getStatus().equals(SignRequestStatus.archived)) {
                Optional<Action> lastActionHero = recipientHasSigned.values().stream().filter(action -> !action.getActionType().equals(ActionType.none)).findFirst();
                if (lastActionHero.isPresent()) {
                    toExportDatas.put("completed_date", lastActionHero.get().getDate().toString());
                    toExportDatas.put("completed_by", recipientHasSigned.entrySet().stream().filter(entry -> lastActionHero.get().equals(entry.getValue())).map(Map.Entry::getKey).findFirst().get().getUser().getEppn());
                } else {
                    toExportDatas.put("completed_date", "");
                    toExportDatas.put("completed_by", "");
                }
            } else {
                toExportDatas.put("completed_date", "");
                toExportDatas.put("completed_by", "");
            }
        } else {
            toExportDatas.put("completed_date", "");
            toExportDatas.put("completed_by", "");
        }
        if(signBook != null) {
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
        }
        return toExportDatas;
    }

    public InputStream mapListToCSV(List<Map<String, String>> list) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputStreamWriter out = new OutputStreamWriter(outputStream);
        String[] headers = list.stream().flatMap(map -> map.keySet().stream()).distinct().toArray(String[]::new);
        CSVPrinter printer = new CSVPrinter(out, CSVFormat.EXCEL.withHeader(headers));
        for (Map<String, String> map : list) {
            printer.printRecord(map.values());
        }
        out.flush();
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        return inputStream;
    }

}
