package org.esupportail.esupsignature.service.export;

import org.esupportail.esupsignature.dto.export.WorkflowDatasCsvDto;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.repository.WorkflowRepository;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.utils.WebUtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class WorkflowExportService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowExportService.class);

    private final WorkflowService workflowService;

    private final WebUtilsService webUtilsService;

    private final WorkflowRepository workflowRepository;

    public WorkflowExportService(WorkflowService workflowService, WebUtilsService webUtilsService, WorkflowRepository workflowRepository) {
        this.workflowService = workflowService;
        this.webUtilsService = webUtilsService;
        this.workflowRepository = workflowRepository;
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
        List<WorkflowDatasCsvDto> workflowDatasCsvDtos = workflowRepository.findWorkflowDatas(workflow.getId());
        return getToExportDatas(workflowDatasCsvDtos);
    }

    @Transactional(readOnly = true)
    public LinkedHashMap<String, String> getJsonDatasFromWorkflow(String id) {
        Workflow workflow = workflowService.getByIdOrToken(id);
        List<LinkedHashMap<String, String>> datas = getDatasToExport(workflow);
        if(!datas.isEmpty()) {
            return datas.get(0);
        } else {
            return new LinkedHashMap<>();
        }
    }

    private List<LinkedHashMap<String, String>> getToExportDatas(List<WorkflowDatasCsvDto> workflowDatasCsvDtos) {
        List<LinkedHashMap<String, String>> dataDatas = new ArrayList<>();
        for(WorkflowDatasCsvDto workflowDatasCsvDto : workflowDatasCsvDtos) {
            LinkedHashMap<String, String> toExportDatas = new LinkedHashMap<>();
            toExportDatas.put("sign_book_id", workflowDatasCsvDto.getSignBookId());
            toExportDatas.put("sign_request_ids", workflowDatasCsvDto.getWorkflowDatasSignRequestIds());
            toExportDatas.put("sign_request_titles", workflowDatasCsvDto.getWorkflowDatasSignRequestTitles());
            toExportDatas.put("sign_book_statut", workflowDatasCsvDto.getSignBookStatus());
            toExportDatas.put("sign_book_create_by", workflowDatasCsvDto.getSignBookCreateBy());
            toExportDatas.put("sign_book_create_date", workflowDatasCsvDto.getSignBookCreateDate());
            toExportDatas.put("completed_date", workflowDatasCsvDto.getCompletedDate());
            toExportDatas.put("completed_by", workflowDatasCsvDto.getCompletedBy());
            toExportDatas.put("current_step_number", (Arrays.stream(workflowDatasCsvDto.getWorkflowDatasStepsActionsTypes().split(",")).filter(s -> !s.equals("none")).count() + 1) + "");
            toExportDatas.put("current_step_id", workflowDatasCsvDto.getCurrentStepId());
            toExportDatas.put("current_step_description", workflowDatasCsvDto.getCurrentStepDescription());
            for(int i = 0; i < workflowDatasCsvDto.getWorkflowDatasStepsActionsTypes().split(",").length; i++) {
                toExportDatas.put("sign_step_" + (i + 1) + "_email", workflowDatasCsvDto.getWorkflowDatasStepsRecipientsEmails().split(",")[i]);
                toExportDatas.put("sign_step_" + (i + 1) + "_type", workflowDatasCsvDto.getWorkflowDatasStepsActionsTypes().split(",")[i]);
                toExportDatas.put("sign_step_" + (i + 1) + "_date", workflowDatasCsvDto.getWorkflowDatasStepsActionsDates().split(",")[i]);
            }
            dataDatas.add(toExportDatas);
        }
        return dataDatas;
    }

}
