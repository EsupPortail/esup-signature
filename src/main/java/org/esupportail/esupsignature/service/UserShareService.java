package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.repository.FormRepository;
import org.esupportail.esupsignature.repository.UserShareRepository;
import org.esupportail.esupsignature.repository.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class UserShareService {

    private static final Logger logger = LoggerFactory.getLogger(UserShareService.class);

    private final GlobalProperties globalProperties;

    @Resource
    private UserService userService;

    @Resource
    private UserShareRepository userShareRepository;

    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm";

    @Resource
    private FormRepository formRepository;

    @Resource
    private WorkflowRepository workflowRepository;

    @Resource
    private DataRepository dataRepository;

    public UserShareService(GlobalProperties globalProperties) {
        this.globalProperties = globalProperties;
    }

    @Transactional
    public List<User> getSuUsers(String authUserEppn) {
        List<User> suUsers = new ArrayList<>();
        if(globalProperties.getShareMode() > 0) {
            for (UserShare userShare : userShareRepository.findByToUsersEppnIn(Collections.singletonList(authUserEppn))) {
                if (!suUsers.contains(userShare.getUser()) && checkUserShareDate(userShare)) {
                    User user = userShare.getUser();
                    user.setUserShareId(userShare.getId());
                    suUsers.add(user);
                }
            }
        }
        return suUsers;
    }

    public List<UserShare> getByWorkflowId(Long id) {
        return userShareRepository.findByWorkflowId(id);
    }

    public void createUserShare(Boolean signWithOwnSign, Boolean forceTransmitEmails, List<Long> formsIds, List<Long> workflowsIds, String[] types, List<User> userEmails, Date beginDate, Date endDate, User user) throws EsupSignatureUserException {
        UserShare userShare = new UserShare();
        userShare.setUser(user);
        if(globalProperties.getShareMode() > 2) {
            userShare.setSignWithOwnSign(signWithOwnSign);
        } else if(globalProperties.getShareMode() > 1) {
            userShare.setSignWithOwnSign(false);
        } else if(globalProperties.getShareMode() > 0) {
            userShare.setSignWithOwnSign(true);
        }
        List<ShareType> shareTypes = new ArrayList<>();
        for(String type : types) {
            shareTypes.add(ShareType.valueOf(type));
        }
        userShare.getShareTypes().addAll(shareTypes);
        for(Long formId : formsIds) {
            Form form = formRepository.findById(formId).orElseThrow();
            if(new HashSet<>(form.getAuthorizedShareTypes()).containsAll(shareTypes)) {
                userShare.setForm(form);
            } else {
                throw new EsupSignatureUserException("La délégation du formulaire : " + form.getTitle() + " n'est pas autorisée");
            }
        }
        for(Long workflowId : workflowsIds) {
            Workflow workflow = workflowRepository.findById(workflowId).orElseThrow();
            if(userShareRepository.findByUserEppnAndWorkflow(user.getEppn(), workflow).isEmpty()) {
                if (new HashSet<>(workflow.getAuthorizedShareTypes()).containsAll(shareTypes)) {
                 userShare.setWorkflow(workflow);
                } else {
                 throw new EsupSignatureUserException("La délégation du circuit : " + workflow.getDescription() + " n'est pas autorisée");
                }
            } else {
                throw new EsupSignatureUserException("Une délégation pour ce circuit existe déjà, merci de la modifier");
            }
        }
        if(formsIds.isEmpty() && workflowsIds.isEmpty()) {
            userShare.setAllSignRequests(true);
        }
        userShare.setForceTransmitEmails(forceTransmitEmails);
        userShare.getToUsers().addAll(userEmails);
        userShare.setBeginDate(beginDate);
        userShare.setEndDate(endDate);
        userShareRepository.save(userShare);
    }

    public void addUserShare(User authUser, Boolean signWithOwnSign, Boolean forceTransmitEmails, Long[] form, Long[] workflow, String[] types, String[] userEmails, String beginDate, String endDate) throws EsupSignatureUserException {
        List<User> users = new ArrayList<>();
        for (String userEmail : userEmails) {
            users.add(userService.getUserByEmail(userEmail));
        }
        Date beginDateDate = null;
        Date endDateDate = null;
        if (StringUtils.hasText(beginDate) && StringUtils.hasText(endDate)) {
            try {
                beginDateDate = new SimpleDateFormat(DATE_PATTERN).parse(beginDate);
                endDateDate = new SimpleDateFormat(DATE_PATTERN).parse(endDate);
            } catch (ParseException e) {
                logger.error("error on parsing dates");
            }
        }
        createUserShare(signWithOwnSign, forceTransmitEmails, Arrays.asList(form), Arrays.asList(workflow), types, users, beginDateDate, endDateDate, authUser);
    }

    @Transactional
    public void updateUserShare(String authUserEppn, String[] types, String[] userEmails, String beginDate, String endDate, Long userShareId, Boolean signWithOwnSign, Boolean forceTransmitEmails) {
        User authUser = userService.getByEppn(authUserEppn);
        UserShare userShare = getById(userShareId);
        if(globalProperties.getShareMode() > 2) {
            userShare.setSignWithOwnSign(signWithOwnSign);
        } else if(globalProperties.getShareMode() > 1) {
            userShare.setSignWithOwnSign(false);
        } else if(globalProperties.getShareMode() > 0) {
            userShare.setSignWithOwnSign(true);
        }
        userShare.setForceTransmitEmails(forceTransmitEmails);
        if(userShare.getUser().equals(authUser)) {
            userShare.getToUsers().clear();
            for (String userEmail : userEmails) {
                userShare.getToUsers().add(userService.getUserByEmail(userEmail));
            }
            userShare.getShareTypes().clear();
            List<ShareType> authorizedShareTypes = new ArrayList<>();
            if(userShare.getWorkflow() != null) {
                authorizedShareTypes.addAll(userShare.getWorkflow().getAuthorizedShareTypes());
            }
            if(userShare.getForm() != null ) {
                authorizedShareTypes.addAll(userShare.getForm().getAuthorizedShareTypes());
            }
            if(userShare.getAllSignRequests()) {
                authorizedShareTypes.addAll(Arrays.asList(ShareType.values()));
            }
            for(String type : types) {
                if(authorizedShareTypes.contains(ShareType.valueOf(type))) {
                    userShare.getShareTypes().add(ShareType.valueOf(type));
                }
            }
            if (StringUtils.hasText(beginDate) && StringUtils.hasText(endDate)) {
                try {
                    userShare.setBeginDate(new SimpleDateFormat(DATE_PATTERN).parse(beginDate));
                    userShare.setEndDate(new SimpleDateFormat(DATE_PATTERN).parse(endDate));
                } catch (ParseException e) {
                    logger.error("error on parsing dates");
                }
            }
        }
    }

    @Transactional
    public Boolean checkShareForSignRequest(String fromUserEppn, String toUserEppn, SignBook signbook, ShareType shareType) {
        List<UserShare> userShares = userShareRepository.findByUserEppnAndToUsersEppnInAndAllSignRequestsIsTrueAndShareTypesContains(fromUserEppn, Collections.singletonList(toUserEppn), shareType);
        if (signbook.getLiveWorkflow().getWorkflow() != null) {
            userShares.addAll(userShareRepository.findByUserEppnAndToUsersEppnInAndWorkflowAndShareTypesContains(fromUserEppn, Collections.singletonList(toUserEppn), signbook.getLiveWorkflow().getWorkflow(), shareType));
            List<Form> forms = formRepository.findByWorkflowIdEquals(signbook.getLiveWorkflow().getWorkflow().getId());
            if(!forms.isEmpty() && forms.get(0) != null) {
                userShares.addAll(userShareRepository.findByUserEppnAndToUsersEppnInAndFormAndShareTypesContains(fromUserEppn, Collections.singletonList(toUserEppn), forms.get(0), shareType));
            }
        }
        Data data = dataRepository.findBySignBook(signbook);
        if(data != null && data.getForm() != null) {
            userShares.addAll(userShareRepository.findByUserEppnAndToUsersEppnInAndFormAndShareTypesContains(fromUserEppn, Collections.singletonList(toUserEppn), data.getForm(), shareType));
        }
        for (UserShare userShare : userShares) {
            if (checkUserShareDate(userShare) && checkUserShareDate(userShare, signbook.getCreateDate())) {
                return true;
            }
        }
        return false;
    }

    public Boolean isOneShareActive(String fromUserEppn, String toUserEppn) {
        List<UserShare> userShares = userShareRepository.findByUserEppnAndToUsersEppnIn(fromUserEppn, Collections.singletonList(toUserEppn));
        for(UserShare userShare : userShares) {
            if (checkUserShareDate(userShare)) {
                return true;
            }
        }
        return false;
    }

    public Boolean checkFormShare(String fromUserEppn, String toUserEppn, ShareType shareType, Form form) {
        if(fromUserEppn.equals(toUserEppn)) {
            return true;
        }
        List<UserShare> userShares = getUserShares(fromUserEppn, Collections.singletonList(toUserEppn), shareType);
        if(shareType.equals(ShareType.sign) && !userShares.isEmpty()) {
            return true;
        }
        for(UserShare userShare : userShares) {
            if(userShare.getForm().equals(form) && checkUserShareDate(userShare)) {
                return true;
            }
        }
        return false;
    }

    public Boolean isOneShareByType(String fromUserEppn, String toUserEppn, ShareType shareType) {
        if(fromUserEppn.equals(toUserEppn)) {
            return true;
        }
        List<UserShare> userShares = getUserShares(fromUserEppn, Collections.singletonList(toUserEppn), shareType);
        if(!userShares.isEmpty()) {
            return true;
        }
        return false;
    }

    public List<UserShare> getUserShares(String fromUserEppn, List<String> toUsers, ShareType shareType) {
        return userShareRepository.findByUserEppnAndToUsersEppnInAndShareTypesContains(fromUserEppn, toUsers, shareType);
    }

    public Boolean checkUserShareDate(UserShare userShare, Date checkDate) {
        if((userShare.getBeginDate() == null
                || checkDate.after(userShare.getBeginDate()))
                && (userShare.getEndDate() == null
                || checkDate.before(userShare.getEndDate()))) {
            return true;
        }
        return false;
    }

    public Boolean checkUserShareDate(UserShare userShare) {
        Date checkDate = new Date();
        return checkUserShareDate(userShare, checkDate);
    }

    public List<UserShare> getUserSharesByUser(String authUserEppn) {
        return userShareRepository.findByUserEppn(authUserEppn);
    }

    public UserShare getById(Long id) {
        return userShareRepository.findById(id).orElseThrow();
    }

    public void delete(Long userShareId, String authUserEppn) {
        User authUser = userService.getByEppn(authUserEppn);
        UserShare userShare = getById(userShareId);
        if (userShare.getUser().equals(authUser)) {
            delete(userShare);
        }
    }

    public void deleteAll(String authUserEppn) {
        List<UserShare> userShares = userShareRepository.findByUserEppn(authUserEppn);
        userShareRepository.deleteAll(userShares);
    }

    public void delete(UserShare userShare) {
        userShareRepository.delete(userShare);
    }

    public List<UserShare> getUserSharesByForm(Form form) {
        return userShareRepository.findByFormId(form.getId());
    }


    public List<UserShare> getByUserAndToUsersInAndShareTypesContains(String userEppn, User authUser, ShareType shareType) {
        return userShareRepository.findByUserEppnAndToUsersEppnInAndShareTypesContains(userEppn, Collections.singletonList(authUser.getEppn()), shareType);
    }

    public List<UserShare> getUserSharesToUser(String eppn) {
        return userShareRepository.findByToUsersEppnIn(Collections.singletonList(eppn));
    }

    public List<UserShare> getByToUsersEppnInAndShareTypesContains(List<String> eppns, ShareType shareType) {
        return userShareRepository.findByToUsersEppnInAndShareTypesContains(eppns, shareType);
    }
}
