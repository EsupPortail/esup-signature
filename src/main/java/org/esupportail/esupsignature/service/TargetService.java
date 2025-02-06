package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.Target;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.repository.TargetRepository;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessFactoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
public class TargetService {

    private static final Logger logger = LoggerFactory.getLogger(TargetService.class);

    @Resource
    private TargetRepository targetRepository;

    private final FsAccessFactoryService fsAccessFactoryService;

    public TargetService(FsAccessFactoryService fsAccessFactoryService) {
        this.fsAccessFactoryService = fsAccessFactoryService;
    }

    public Target getById(Long id) {
        return targetRepository.findById(id).get();
    }

    @Transactional
    public Target createTarget(String targetUri, Boolean sendDocument, Boolean sendReport) {
        Target target = new Target();
        target.setTargetUri(targetUri);
        target.setSendDocument(sendDocument);
        target.setSendReport(sendReport);
        targetRepository.save(target);
        return target;
    }

    public void sendRest(String target, String signRequestId, String status, String step, String userEppn, String comment) throws EsupSignatureFsException {
        ResponseEntity<String> response;
        RestTemplate restTemplate = new RestTemplate();
        UriComponents targetUri = UriComponentsBuilder.fromUriString(target)
                .queryParam("signRequestId", signRequestId)
                .queryParam("status", status)
                .queryParam("step", step)
                .queryParam("userEppn", userEppn)
                .queryParam("comment", comment)
                .build();
        boolean sendOk = false;
        try {
            response = restTemplate.getForEntity(targetUri.toUri(), String.class);
            if(response.getStatusCode().equals(HttpStatus.OK)) {
                sendOk = true;
            }
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ObjectMapper objectMapper = new ObjectMapper();
            HttpEntity<String> postRequest = new HttpEntity<>(objectMapper.writeValueAsString(targetUri.getQueryParams().toSingleValueMap()), headers);
            response = restTemplate.exchange(target, HttpMethod.POST, postRequest, String.class);
            if(response.getStatusCode().equals(HttpStatus.OK)) {
                sendOk = true;
            }
        }catch (Exception e) {
            logger.warn(e.getMessage());
        }
        if(!sendOk) {
            throw new EsupSignatureFsException("error sending to target " + target);
        }
    }

    @Transactional
    public void copyTargets(List<Target> targets, SignBook signBook, List<String> targetEmails) throws EsupSignatureFsException {
        for(Target target : targets) {
            if(signBook.getLiveWorkflow().getTargets().stream().noneMatch(t -> t != null && t.getTargetUri().equals(target.getTargetUri()))
                    && fsAccessFactoryService.getPathIOType(target.getTargetUri()) != DocumentIOType.mail
                    && target.getTargetUri() != null && !target.getTargetUri().isEmpty()) {
                signBook.getLiveWorkflow().getTargets().add(createTarget(target.getTargetUri(), target.getSendDocument(), target.getSendReport()));
            }
        }
        signBook.getLiveWorkflow().getTargets().addAll(addTargetEmails(targetEmails, targets));
    }

    @Transactional
    public List<Target> addTargetEmails(List<String> targetEmails, List<Target> targets) throws EsupSignatureFsException {
        List<Target> targetsCreated = new ArrayList<>();
        List<String> targetEmailsToAdd = new ArrayList<>();
        for(Target target1 : targets) {
            if(fsAccessFactoryService.getPathIOType(target1.getTargetUri()).equals(DocumentIOType.mail)) {
                for(String targetEmail : target1.getTargetUri().replace("mailto:", "").split(",")) {
                    if (!targetEmailsToAdd.toString().contains(targetEmail)) {
                        targetEmailsToAdd.add(targetEmail);
                        targetsCreated.add(createTarget("mailto:" + targetEmail, target1.getSendDocument(), target1.getSendReport()));
                    }
                }
            }
        }
        if(targetEmails != null) {
            for (String targetEmail : targetEmails) {
                if (!targetEmailsToAdd.toString().contains(targetEmail)) {
                    targetEmailsToAdd.add(targetEmail);
                    targetsCreated.add(createTarget("mailto:" + targetEmail, true, false));
                }
            }
        }
        return targetsCreated;
    }

    public void delete(Target target) {
        targetRepository.delete(target);
    }

    @Transactional
    public void toggleSendDocument(Long id) {
        Target target = getById(id);
        target.setSendDocument(!target.getSendDocument());
    }

    @Transactional
    public void toggleSendReport(Long id) {
        Target target = getById(id);
        target.setSendReport(!target.getSendReport());
    }

}
