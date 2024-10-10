package org.esupportail.esupsignature.service;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WsAccessToken;
import org.esupportail.esupsignature.repository.WsAccessTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class WsAccessTokenService {

    @Resource
    private WsAccessTokenRepository wsAccessTokenRepository;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private WorkflowService workflowService;


    public boolean isAllAccess(String token) {
        return allAccess(token);
    }

    public boolean createWorkflowAccess(Long id, String token) {
        if(allAccess(token)) return true;
        Workflow workflow = workflowService.getById(id);
        if(workflow == null) return true;
        List<WsAccessToken> wsAccessToken = wsAccessTokenRepository.findByTokenAndWorkflowsContains(token, workflow);
        return !wsAccessToken.isEmpty() && wsAccessToken.stream().anyMatch(WsAccessToken::getCreateSignrequest);
    }

    public boolean workflowCsv(Long id, String csvToken) {
        Workflow workflow = workflowService.getById(id);
        return StringUtils.hasText(workflow.getCsvToken()) && workflow.getCsvToken().equals(csvToken);
    }

    public boolean readWorkflowAccess(Long id, String token) {
        if(allAccess(token)) return true;
        SignRequest signRequest = signRequestService.getById(id);
        if(signRequest == null) return true;
        Workflow workflow = signRequest.getParentSignBook().getLiveWorkflow().getWorkflow();
        List<WsAccessToken> wsAccessToken = wsAccessTokenRepository.findByTokenAndWorkflowsContains(token, workflow);
        return !wsAccessToken.isEmpty() && wsAccessToken.stream().anyMatch(WsAccessToken::getReadSignrequest);
    }

    public boolean deleteWorkflowAccess(Long id, String token) {
        if(allAccess(token)) return true;
        SignRequest signRequest = signRequestService.getById(id);
        if(signRequest == null) return true;
        Workflow workflow = signRequest.getParentSignBook().getLiveWorkflow().getWorkflow();
        List<WsAccessToken> wsAccessToken = wsAccessTokenRepository.findByTokenAndWorkflowsContains(token, workflow);
        return !wsAccessToken.isEmpty() && wsAccessToken.stream().anyMatch(WsAccessToken::getDeleteSignrequest);
    }

    public boolean allAccess(String token) {
        return !wsAccessTokenRepository.findAll().iterator().hasNext() || !wsAccessTokenRepository.findByTokenIsNullAndWorkflowsEmpty().isEmpty() || wsAccessTokenRepository.findByTokenAndWorkflowsEmpty(token).stream().anyMatch(WsAccessToken::getReadSignrequest);
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
            wsAccessToken.setToken(UUID.randomUUID().toString());
            wsAccessTokenRepository.save(wsAccessToken);
            return true;
        }
        return false;
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

    public void renew(Long wsAccessTokenId) {
        WsAccessToken wsAccessToken = wsAccessTokenRepository.findById(wsAccessTokenId).get();
        wsAccessToken.setToken(UUID.randomUUID().toString());
    }

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
