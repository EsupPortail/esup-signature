package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.LiveWorkflowStep;
import org.esupportail.esupsignature.repository.LiveWorkflowStepRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class LiveWorkflowStepService {

    @Resource
    private LiveWorkflowStepRepository liveWorkflowStepRepository;

    public void delete(LiveWorkflowStep liveWorkflowStep) {
        liveWorkflowStepRepository.delete(liveWorkflowStep);
    }
}
