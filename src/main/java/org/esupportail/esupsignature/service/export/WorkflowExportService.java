package org.esupportail.esupsignature.service.export;

import org.esupportail.esupsignature.dto.WorkflowDatasDto;
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
        List<WorkflowDatasDto> workflowDatasDtos = workflowRepository.findWorkflowDatas(workflow.getId());
        return getToExportDatas(workflowDatasDtos);
    }

    @Transactional(readOnly = true)
    public LinkedHashMap<String, String> getJsonDatasFromWorkflow(Long id) {
        Workflow workflow = workflowService.getById(id);
        return getDatasToExport(workflow).get(0);
    }

    private List<LinkedHashMap<String, String>> getToExportDatas(List<WorkflowDatasDto> workflowDatasDtos) {
        List<LinkedHashMap<String, String>> dataDatas = new ArrayList<>();
        for(WorkflowDatasDto workflowDatasDto : workflowDatasDtos) {
            LinkedHashMap<String, String> toExportDatas = new LinkedHashMap<>();
            toExportDatas.put("sign_book_id", workflowDatasDto.getSignBookId());
            toExportDatas.put("sign_request_ids", workflowDatasDto.getWorkflowDatasSignRequestIds());
            toExportDatas.put("sign_request_titles", workflowDatasDto.getWorkflowDatasSignRequestTitles());
            toExportDatas.put("sign_book_statut", workflowDatasDto.getSignBookStatus());
            toExportDatas.put("sign_book_create_by", workflowDatasDto.getSignBookCreateBy());
            toExportDatas.put("sign_book_create_date", workflowDatasDto.getSignBookCreateDate());
            toExportDatas.put("completed_date", workflowDatasDto.getCompletedDate());
            toExportDatas.put("completed_by", workflowDatasDto.getCompletedBy());
            toExportDatas.put("current_step_number", (Arrays.stream(workflowDatasDto.getWorkflowDatasStepsActionsTypes().split(",")).filter(s -> !s.equals("none")).count() + 1) + "");
            toExportDatas.put("current_step_id", workflowDatasDto.getCurrentStepId());
            toExportDatas.put("current_step_description", workflowDatasDto.getCurrentStepDescription());
            for(int i = 0; i < workflowDatasDto.getWorkflowDatasStepsActionsTypes().split(",").length; i++) {
                toExportDatas.put("sign_step_" + i + "_email", workflowDatasDto.getWorkflowDatasStepsRecipientsEmails().split(",")[i]);
                toExportDatas.put("sign_step_" + i + "_type", workflowDatasDto.getWorkflowDatasStepsActionsTypes().split(",")[i]);
                toExportDatas.put("sign_step_" + i + "_date", workflowDatasDto.getWorkflowDatasStepsActionsDates().split(",")[i]);
            }
            dataDatas.add(toExportDatas);
        }
        return dataDatas;
    }

}
