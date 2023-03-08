package org.esupportail.esupsignature.service;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.repository.LiveWorkflowStepRepository;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.web.ws.json.JsonExternalUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

@Service
public class LiveWorkflowStepService {

    private static final Logger logger = LoggerFactory.getLogger(LiveWorkflowService.class);

    @Resource
    private LiveWorkflowStepRepository liveWorkflowStepRepository;

    @Resource
    private RecipientService recipientService;

    @Resource
    private UserService userService;

    @Resource
    private UserPropertieService userPropertieService;

    @Resource
    private SignBookRepository signBookRepository;

    public LiveWorkflowStep createLiveWorkflowStep(SignBook signBook, WorkflowStep workflowStep, Boolean repeatable, SignType repeatableSignType, Boolean multiSign, Boolean autoSign, Boolean allSignToComplete, SignType signType, List<String> recipientsEmails, List<JsonExternalUserInfo> externalUsersInfos) {
        LiveWorkflowStep liveWorkflowStep = new LiveWorkflowStep();
        liveWorkflowStep.setWorkflowStep(workflowStep);
        if(repeatable == null) {
            liveWorkflowStep.setRepeatable(false);
        } else {
            liveWorkflowStep.setRepeatable(repeatable);
        }

        if(multiSign == null) {
            liveWorkflowStep.setMultiSign(false);
        } else {
            liveWorkflowStep.setMultiSign(multiSign);
        }

        if(autoSign == null) {
            liveWorkflowStep.setAutoSign(false);
        } else {
            liveWorkflowStep.setAutoSign(autoSign);
        }

        if(allSignToComplete == null) {
            liveWorkflowStep.setAllSignToComplete(false);
        } else {
            liveWorkflowStep.setAllSignToComplete(allSignToComplete);
        }
        liveWorkflowStep.setSignType(signType);
        liveWorkflowStep.setRepeatableSignType(repeatableSignType);
        addRecipientsToWorkflowStep(signBook, liveWorkflowStep, recipientsEmails, externalUsersInfos);
        liveWorkflowStepRepository.save(liveWorkflowStep);
        return liveWorkflowStep;
    }

    public void addRecipientsToWorkflowStep(SignBook signBook, LiveWorkflowStep liveWorkflowStep, List<String> recipientsEmails, List<JsonExternalUserInfo> externalUsersInfos) {
        for (String recipientEmail : recipientsEmails) {
            User recipientUser = userService.getUserByEmail(recipientEmail);
            if(recipientUser != null && recipientUser.getUserType().equals(UserType.external) && externalUsersInfos != null) {
                Optional<JsonExternalUserInfo> optionalJsonExternalUserInfo = externalUsersInfos.stream().filter(jsonExternalUserInfo1 -> jsonExternalUserInfo1.getEmail().equals(recipientEmail)).findFirst();
                if(optionalJsonExternalUserInfo.isPresent()) {
                    JsonExternalUserInfo jsonExternalUserInfo = optionalJsonExternalUserInfo.get();
                    recipientUser.setName(jsonExternalUserInfo.getName());
                    recipientUser.setFirstname(jsonExternalUserInfo.getFirstname());
                    if(StringUtils.hasText(jsonExternalUserInfo.getPhone())) {
                        recipientUser.setPhone(PhoneNumberUtil.normalizeDiallableCharsOnly(jsonExternalUserInfo.getPhone()));
                        recipientUser.setForceSms(jsonExternalUserInfo.getForcesms() != null);
                    }
                }
            }
            if(liveWorkflowStep.getId() != null) {
                for (Recipient recipient : liveWorkflowStep.getRecipients()) {
                    if (recipient.getUser().equals(recipientUser)) {
                        return;
                    }
                }
            }
            if(recipientUser != null) {
                Recipient recipient = recipientService.createRecipient(recipientUser);
                liveWorkflowStep.getRecipients().add(recipient);
                if(!signBook.getTeam().contains(recipientUser)) {
                    signBook.getTeam().add(recipientUser);
                }
            }
        }
    }

    @Transactional
    public void addNewStepToSignBook(Long signBookId, SignType signType, Boolean allSignToComplete, List<String> recipientsEmails, List<JsonExternalUserInfo> externalUsersInfos, String authUserEppn) {
        SignBook signBook = signBookRepository.findById(signBookId).get();
        logger.info("add new workflow step to signBook " + signBook.getSubject() + " - " + signBook.getId());
        LiveWorkflowStep liveWorkflowStep = createLiveWorkflowStep(signBook, null,false, null,true, false, allSignToComplete, signType, recipientsEmails, externalUsersInfos);
        signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStep);
        if(recipientsEmails != null) {
            userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUserEppn), recipientsEmails);
        }
    }

//    public void addRecipients(LiveWorkflowStep liveWorkflowStep, String... recipientsEmail) {
//        for (String recipientEmail : recipientsEmail) {
//            User recipientUser = userService.getUserByEmail(recipientEmail);
//            if (liveWorkflowStep.getRecipients().stream().anyMatch(r -> r.getUser().equals(recipientUser))) {
//                Recipient recipient = recipientService.createRecipient(recipientUser);
//                liveWorkflowStep.getRecipients().add(recipient);
//            }
//        }
//    }

    public void delete(LiveWorkflowStep liveWorkflowStep) {
        liveWorkflowStepRepository.delete(liveWorkflowStep);
    }

    public void delete(Long id) {
        Optional<LiveWorkflowStep> liveWorkflowStep = liveWorkflowStepRepository.findById(id);
        liveWorkflowStep.ifPresent(workflowStep -> liveWorkflowStepRepository.delete(workflowStep));
    }

    public List<LiveWorkflowStep> getLiveWorkflowStepByWorkflowStep(WorkflowStep workflowStep) {
        return liveWorkflowStepRepository.findByWorkflowStep(workflowStep);
    }

}