package org.esupportail.esupsignature.service.export;

import org.esupportail.esupsignature.dto.projection.export.WorkflowDatasCsvProjectionDto;
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
        List<WorkflowDatasCsvProjectionDto> workflowDatasCsvProjectionDtos = workflowRepository.findWorkflowDatas(workflow.getId());
        return getToExportDatas(workflowDatasCsvProjectionDtos);
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

    private List<LinkedHashMap<String, String>> getToExportDatas(List<WorkflowDatasCsvProjectionDto> workflowDatasCsvProjectionDtos) {
        List<LinkedHashMap<String, String>> dataDatas = new ArrayList<>();
        for(WorkflowDatasCsvProjectionDto workflowDatasCsvProjectionDto : workflowDatasCsvProjectionDtos) {
            LinkedHashMap<String, String> toExportDatas = new LinkedHashMap<>();
            toExportDatas.put("sign_book_id", workflowDatasCsvProjectionDto.getSignBookId());
            toExportDatas.put("sign_request_ids", workflowDatasCsvProjectionDto.getWorkflowDatasSignRequestIds());
            toExportDatas.put("sign_request_titles", workflowDatasCsvProjectionDto.getWorkflowDatasSignRequestTitles());
            toExportDatas.put("sign_book_statut", workflowDatasCsvProjectionDto.getSignBookStatus());
            toExportDatas.put("sign_book_create_by", workflowDatasCsvProjectionDto.getSignBookCreateBy());
            toExportDatas.put("sign_book_create_date", workflowDatasCsvProjectionDto.getSignBookCreateDate());
            toExportDatas.put("completed_date", workflowDatasCsvProjectionDto.getCompletedDate());
            toExportDatas.put("completed_by", workflowDatasCsvProjectionDto.getCompletedBy());
            toExportDatas.put("current_step_number", (Arrays.stream(workflowDatasCsvProjectionDto.getWorkflowDatasStepsActionsTypes().split(",")).filter(s -> !s.equals("none")).count() + 1) + "");
            toExportDatas.put("current_step_id", workflowDatasCsvProjectionDto.getCurrentStepId());
            toExportDatas.put("current_step_description", workflowDatasCsvProjectionDto.getCurrentStepDescription());
            for(int i = 0; i < workflowDatasCsvProjectionDto.getWorkflowDatasStepsActionsTypes().split(",").length; i++) {
                toExportDatas.put("sign_step_" + (i + 1) + "_email", workflowDatasCsvProjectionDto.getWorkflowDatasStepsRecipientsEmails().split(",")[i]);
                toExportDatas.put("sign_step_" + (i + 1) + "_type", workflowDatasCsvProjectionDto.getWorkflowDatasStepsActionsTypes().split(",")[i]);
                toExportDatas.put("sign_step_" + (i + 1) + "_date", workflowDatasCsvProjectionDto.getWorkflowDatasStepsActionsDates().split(",")[i]);
            }
            dataDatas.add(toExportDatas);
        }
        return dataDatas;
    }

}
