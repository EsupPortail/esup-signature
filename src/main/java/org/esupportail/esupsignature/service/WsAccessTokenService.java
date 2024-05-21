package org.esupportail.esupsignature.service;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WsAccessToken;
import org.esupportail.esupsignature.repository.WsAccessTokenRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WsAccessTokenService {

    @Resource
    private WsAccessTokenRepository wsAccessTokenRepository;

    @Resource
    private SignRequestService signRequestService;


    @Resource
    private WorkflowService workflowService;

    @Transactional
    public boolean createWorkflowAccess(Long id, String token) {
        if(allAccess()) return true;
        Workflow workflow = workflowService.getById(id);
        List<WsAccessToken> wsAccessToken = wsAccessTokenRepository.findByTokenAndWorkflowsContains(token, workflow);
        return !wsAccessToken.isEmpty() && wsAccessToken.stream().anyMatch(WsAccessToken::getCreateSignrequest);
    }

    @Transactional
    public boolean readWorkflowAccess(Long id, String token) {
        if(allAccess() || wsAccessTokenRepository.findByTokenAndWorkflowsEmpty(token).stream().anyMatch(WsAccessToken::getReadSignrequest)) return true;
        SignRequest signRequest = signRequestService.getById(id);
        Workflow workflow = signRequest.getParentSignBook().getLiveWorkflow().getWorkflow();
        List<WsAccessToken> wsAccessToken = wsAccessTokenRepository.findByTokenAndWorkflowsContains(token, workflow);
        return !wsAccessToken.isEmpty() && wsAccessToken.stream().anyMatch(WsAccessToken::getReadSignrequest);
    }

    @Transactional
    public boolean deleteWorkflowAccess(Long id, String token) {
        if(allAccess()) return true;
        SignRequest signRequest = signRequestService.getById(id);
        Workflow workflow = signRequest.getParentSignBook().getLiveWorkflow().getWorkflow();
        List<WsAccessToken> wsAccessToken = wsAccessTokenRepository.findByTokenAndWorkflowsContains(token, workflow);
        return !wsAccessToken.isEmpty() && wsAccessToken.stream().anyMatch(WsAccessToken::getDeleteSignrequest);
    }

    public boolean allAccess() {
        return !wsAccessTokenRepository.findByTokenIsNullAndWorkflowsEmpty().isEmpty();
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    @Transactional
    public void initWsAccessToken() {
        if(!wsAccessTokenRepository.findAll().iterator().hasNext()) {
            WsAccessToken wsAccessToken = new WsAccessToken();
            wsAccessTokenRepository.save(wsAccessToken);
        }
    }

}
