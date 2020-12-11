package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.LiveWorkflow;
import org.esupportail.esupsignature.repository.LiveWorkflowRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class LiveWorkflowService {

    @Resource
    LiveWorkflowRepository liveWorkflowRepository;

    public LiveWorkflow create() {
        LiveWorkflow liveWorkflow = new LiveWorkflow();
        liveWorkflowRepository.save(liveWorkflow);
        return liveWorkflow;
    }

    public void delete(LiveWorkflow liveWorkflow) {
        liveWorkflowRepository.delete(liveWorkflow);
    }

}
