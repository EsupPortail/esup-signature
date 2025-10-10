package org.esupportail.esupsignature.service;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.entity.Action;
import org.esupportail.esupsignature.repository.ActionRepository;
import org.springframework.stereotype.Service;

@Service
public class ActionService {

    @Resource
    private ActionRepository actionRepository;

    public Action getEmptyAction() {
        Action action = new Action();
        actionRepository.save(action);
        return action;
    }

}
