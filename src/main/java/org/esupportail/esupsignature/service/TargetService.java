package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.Target;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.repository.TargetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class TargetService {

    @Resource
    private TargetRepository targetRepository;

    public Target getById(Long id) {
        return targetRepository.findById(id).get();
    }

    @Transactional
    public Target createTarget(DocumentIOType targetType, String targetUri) {
        Target target = new Target();
        target.setTargetType(targetType);
        target.setTargetUri(targetUri);
        targetRepository.save(target);
        return target;
    }


    public void copyTargets(List<Target> targets, SignBook signBook) {
        for(Target target : targets) {
            signBook.getLiveWorkflow().getTargets().add(createTarget(target.getTargetType(), target.getTargetUri()));
        }
    }

    public void delete(Target target) {
        targetRepository.delete(target);
    }

}
