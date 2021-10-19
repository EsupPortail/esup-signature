package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.Target;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.repository.TargetRepository;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class TargetService {

    @Resource
    private TargetRepository targetRepository;

    @Resource
    private FsAccessFactory fsAccessFactory;

    public Target getById(Long id) {
        return targetRepository.findById(id).get();
    }

    @Transactional
    public Target createTarget(String targetUri) throws EsupSignatureFsException {
        Target target = new Target();
        target.setTargetType(fsAccessFactory.getPathIOType(targetUri));
        target.setTargetUri(targetUri);
        targetRepository.save(target);
        return target;
    }


    public void copyTargets(List<Target> targets, SignBook signBook, List<String> targetEmails) throws EsupSignatureFsException, EsupSignatureException {
        signBook.getLiveWorkflow().getTargets().clear();
        for(Target target : targets) {
            if(target.getTargetType() != DocumentIOType.none && target.getTargetType() != DocumentIOType.mail && target.getTargetUri() != null && !target.getTargetUri().isEmpty()) {
                signBook.getLiveWorkflow().getTargets().add(createTarget(target.getTargetUri()));
            }
        }
        signBook.getLiveWorkflow().getTargets().add(addTargetEmails(targetEmails, targets));
    }

    public Target addTargetEmails(List<String> targetEmails, List<Target> targets) throws EsupSignatureFsException {
        StringBuilder targetEmailsToAdd = new StringBuilder();
        for(Target target1 : targets) {
            if(target1.getTargetType().equals(DocumentIOType.mail)) {
                for(String targetEmail : target1.getTargetUri().split(";")) {
                    if (!targetEmailsToAdd.toString().contains(targetEmail)) {
                        targetEmailsToAdd.append(targetEmail).append(";");
                    }
                }
            }
        }
        if(targetEmails != null) {
            for (String targetEmail : targetEmails) {
                if (!targetEmailsToAdd.toString().contains(targetEmail)) {
                    targetEmailsToAdd.append(targetEmail).append(";");
                }
            }
        }
        if(!targetEmailsToAdd.toString().isEmpty()) {
            return createTarget(targetEmailsToAdd.toString());
        } else {
            return null;
        }
    }

    public void delete(Target target) {
        targetRepository.delete(target);
    }

}
