package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Action;
import org.esupportail.esupsignature.repository.ActionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ActionService {

    private static final Logger logger = LoggerFactory.getLogger(ActionService.class);

    private final ActionRepository actionRepository;

    public ActionService(ActionRepository actionRepository) {
        this.actionRepository = actionRepository;
    }

    public Action getEmptyAction() {
        Action action = new Action();
        actionRepository.save(action);
        return action;
    }

}
