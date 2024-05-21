package org.esupportail.esupsignature.repository;


import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WsAccessToken;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface WsAccessTokenRepository extends CrudRepository<WsAccessToken, Long> {
    List<WsAccessToken> findByTokenAndWorkflowsEmpty(String token);
    List<WsAccessToken> findByTokenAndWorkflowsContains(String token, Workflow workflow);
    List<WsAccessToken> findByTokenIsNullAndWorkflowsEmpty();
}
