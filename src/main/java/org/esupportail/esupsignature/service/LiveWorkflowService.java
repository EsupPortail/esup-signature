package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.LiveWorkflow;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.repository.LiveWorkflowRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class LiveWorkflowService {

    @Resource
    LiveWorkflowRepository liveWorkflowRepository;

    public LiveWorkflow create(String title, Workflow workflow) {
        LiveWorkflow liveWorkflow = new LiveWorkflow();
        liveWorkflow.setTitle(title);
        liveWorkflow.setWorkflow(workflow);
        liveWorkflowRepository.save(liveWorkflow);
        return liveWorkflow;
    }

    public List<LiveWorkflow> getByWorkflow(Workflow workflow) {
        return liveWorkflowRepository.findByWorkflow(workflow);
    }

    public void delete(LiveWorkflow liveWorkflow) {
        liveWorkflowRepository.delete(liveWorkflow);
    }

}
