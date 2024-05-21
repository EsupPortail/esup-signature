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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class WsAccessTokenService {

    @Resource
    private WsAccessTokenRepository wsAccessTokenRepository;

    @Resource
    private SignRequestService signRequestService;


    @Resource
    private WorkflowService workflowService;

    @Transactional
    public boolean isAllAccess(String token) {
        return allAccess(token);
    }

    @Transactional
    public boolean createWorkflowAccess(Long id, String token) {
        if(allAccess(token)) return true;
        Workflow workflow = workflowService.getById(id);
        List<WsAccessToken> wsAccessToken = wsAccessTokenRepository.findByTokenAndWorkflowsContains(token, workflow);
        return !wsAccessToken.isEmpty() && wsAccessToken.stream().anyMatch(WsAccessToken::getCreateSignrequest);
    }

    @Transactional
    public boolean readWorkflowAccess(Long id, String token) {
        if(allAccess(token)) return true;
        SignRequest signRequest = signRequestService.getById(id);
        Workflow workflow = signRequest.getParentSignBook().getLiveWorkflow().getWorkflow();
        List<WsAccessToken> wsAccessToken = wsAccessTokenRepository.findByTokenAndWorkflowsContains(token, workflow);
        return !wsAccessToken.isEmpty() && wsAccessToken.stream().anyMatch(WsAccessToken::getReadSignrequest);
    }

    @Transactional
    public boolean deleteWorkflowAccess(Long id, String token) {
        if(allAccess(token)) return true;
        SignRequest signRequest = signRequestService.getById(id);
        Workflow workflow = signRequest.getParentSignBook().getLiveWorkflow().getWorkflow();
        List<WsAccessToken> wsAccessToken = wsAccessTokenRepository.findByTokenAndWorkflowsContains(token, workflow);
        return !wsAccessToken.isEmpty() && wsAccessToken.stream().anyMatch(WsAccessToken::getDeleteSignrequest);
    }

    public boolean allAccess(String token) {
        return !wsAccessTokenRepository.findByTokenIsNullAndWorkflowsEmpty().isEmpty() || wsAccessTokenRepository.findByTokenAndWorkflowsEmpty(token).stream().anyMatch(WsAccessToken::getReadSignrequest);
    }

    public List<WsAccessToken> getAll() {
        List<WsAccessToken> list = new ArrayList<>();
        wsAccessTokenRepository.findAll().forEach(list::add);
        return list;
    }

    public void delete(Long wsAccessTokenId) {
        WsAccessToken wsAccessToken = wsAccessTokenRepository.findById(wsAccessTokenId).get();
        wsAccessTokenRepository.delete(wsAccessToken);
    }

    public boolean createDefaultWsAccessToken() {
        if(!wsAccessTokenRepository.findAll().iterator().hasNext()) {
            WsAccessToken wsAccessToken = new WsAccessToken();
            wsAccessTokenRepository.save(wsAccessToken);
            return true;
        }
        return false;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    @Transactional
    public void initWsAccessToken() {
        createDefaultWsAccessToken();
    }

    public void createToken(String appName, List<Long> workflowIds) {
        WsAccessToken wsAccessToken = new WsAccessToken();
        wsAccessToken.setAppName(appName);
        if(workflowIds.stream().noneMatch(workflowId -> workflowId.equals(0L))) {
            for(Long workflowId : workflowIds) {
                wsAccessToken.getWorkflows().add(workflowService.getById(workflowId));
            }
        }
        wsAccessToken.setToken(UUID.randomUUID().toString());
        wsAccessTokenRepository.save(wsAccessToken);
    }

    @Transactional
    public void renew(Long wsAccessTokenId) {
        WsAccessToken wsAccessToken = wsAccessTokenRepository.findById(wsAccessTokenId).get();
        wsAccessToken.setToken(UUID.randomUUID().toString());
    }

    @Transactional
    public void updateToken(Long wsAccessTokenId, String appName, List<Long> workflowIds) {
        WsAccessToken wsAccessToken = wsAccessTokenRepository.findById(wsAccessTokenId).get();
        wsAccessToken.setAppName(appName);
        wsAccessToken.getWorkflows().clear();
        if(workflowIds.stream().noneMatch(workflowId -> workflowId.equals(0L))) {
            for(Long workflowId : workflowIds) {
                wsAccessToken.getWorkflows().add(workflowService.getById(workflowId));
            }
        }
    }
}
