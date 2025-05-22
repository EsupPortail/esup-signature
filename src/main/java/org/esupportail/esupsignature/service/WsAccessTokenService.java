package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WsAccessToken;
import org.esupportail.esupsignature.repository.WsAccessTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WsAccessTokenService {

    private final WsAccessTokenRepository wsAccessTokenRepository;

    private final SignRequestService signRequestService;

    private final WorkflowService workflowService;

    private final UserService userService;

    public WsAccessTokenService(WsAccessTokenRepository wsAccessTokenRepository, SignRequestService signRequestService, WorkflowService workflowService, UserService userService) {
        this.wsAccessTokenRepository = wsAccessTokenRepository;
        this.signRequestService = signRequestService;
        this.workflowService = workflowService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public boolean isAllAccess(String token) {
        return allAccess(token);
    }

    @Transactional(readOnly = true)
    public boolean isTokenExist(String token) {
        return allAccess(token) || wsAccessTokenRepository.findByToken(token) != null;
    }

    @Transactional(readOnly = true)
    public WsAccessToken getByToken(String token) {
        return wsAccessTokenRepository.findByToken(token);
    }

    @Transactional
    public boolean createWorkflowAccess(String id, String token) {
        if(allAccess(token)) return true;
        Workflow workflow = workflowService.getByIdOrToken(id);
        if(workflow == null) return true;
        List<WsAccessToken> wsAccessTokens = wsAccessTokenRepository.findByTokenAndWorkflowsContains(token, workflow);
        if(!wsAccessTokens.isEmpty() && wsAccessTokens.stream().anyMatch(WsAccessToken::getCreateSignrequest)) {
            return true;
        }
        List<WsAccessToken> allWsAccessTokens = wsAccessTokenRepository.findByWorkflowsContains(workflow);
        if(!allWsAccessTokens.isEmpty() && allWsAccessTokens.stream().anyMatch(WsAccessToken::getPublicAccess)) {
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public boolean workflowCsv(String id, String csvToken) {
        Workflow workflow = workflowService.getByIdOrToken(id);
        User user = userService.getByAccessToken(csvToken);
        return user != null && (workflow.getManagers().contains(user.getEmail()) || user.getRoles().contains(workflow.getManagerRole()));
    }

    @Transactional(readOnly = true)
    public boolean readWorkflowAccess(Long id, String token) {
        if(allAccess(token)) return true;
        SignRequest signRequest = signRequestService.getById(id);
        if(signRequest == null) return true;
        Workflow workflow = signRequest.getParentSignBook().getLiveWorkflow().getWorkflow();
        List<WsAccessToken> wsAccessTokens = wsAccessTokenRepository.findByTokenAndWorkflowsContains(token, workflow);
        if(!wsAccessTokens.isEmpty() && wsAccessTokens.stream().anyMatch(WsAccessToken::getReadSignrequest)) {
            return true;
        }
        List<WsAccessToken> allWsAccessTokens = wsAccessTokenRepository.findByWorkflowsContains(workflow);
        if(!allWsAccessTokens.isEmpty() && allWsAccessTokens.stream().anyMatch(WsAccessToken::getPublicAccess)) {
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public boolean updateWorkflowAccess(Long id, String token) {
        if(allAccess(token)) return true;
        SignRequest signRequest = signRequestService.getById(id);
        if(signRequest == null) return true;
        Workflow workflow = signRequest.getParentSignBook().getLiveWorkflow().getWorkflow();
        List<WsAccessToken> wsAccessTokens = wsAccessTokenRepository.findByTokenAndWorkflowsContains(token, workflow);
        if(!wsAccessTokens.isEmpty() && wsAccessTokens.stream().anyMatch(WsAccessToken::getUpdateSignrequest)) {
            return true;
        }
        List<WsAccessToken> allWsAccessTokens = wsAccessTokenRepository.findByWorkflowsContains(workflow);
        if(!allWsAccessTokens.isEmpty() && allWsAccessTokens.stream().anyMatch(WsAccessToken::getPublicAccess)) {
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public boolean allAccess(String token) {
        return !wsAccessTokenRepository.findAll().iterator().hasNext()
            || !wsAccessTokenRepository.findByTokenIsNullAndWorkflowsEmpty().isEmpty()
            || (getGlobalToken() == null && wsAccessTokenRepository.findByToken(token) == null)
            || wsAccessTokenRepository.findByWorkflowsEmpty().stream().anyMatch(WsAccessToken::getPublicAccess)
            || wsAccessTokenRepository.findByTokenAndWorkflowsEmpty(token).stream().anyMatch(WsAccessToken::getReadSignrequest);
    }

    @Transactional(readOnly = true)
    public List<WsAccessToken> getAll() {
        List<WsAccessToken> list = new ArrayList<>();
        wsAccessTokenRepository.findAll().forEach(list::add);
        return list.stream()
                .sorted(Comparator.comparing(WsAccessToken::getAppName, Comparator.nullsFirst(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long wsAccessTokenId) {
        WsAccessToken wsAccessToken = wsAccessTokenRepository.findById(wsAccessTokenId).get();
        wsAccessTokenRepository.delete(wsAccessToken);
    }

    @Transactional
    public boolean createDefaultWsAccessToken() {
        if(getGlobalToken() == null) {
            WsAccessToken wsAccessToken = new WsAccessToken();
            wsAccessToken.setToken(UUID.randomUUID().toString());
            wsAccessTokenRepository.save(wsAccessToken);
            return true;
        }
        return false;
    }

    @Transactional
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
        wsAccessToken.setPublicAccess(false);
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

    @Transactional
    public void togglePublic(Long wsAccessTokenId) {
        WsAccessToken wsAccessToken = wsAccessTokenRepository.findById(wsAccessTokenId).get();
        wsAccessToken.setPublicAccess(!wsAccessToken.getPublicAccess());
    }

    public WsAccessToken getGlobalToken() {
        List<WsAccessToken> wsAccessTokens = wsAccessTokenRepository.findByWorkflowsEmpty();
        if(!wsAccessTokens.isEmpty()) {
            return wsAccessTokens.get(0);
        } else {
            return null;
        }
    }
}
