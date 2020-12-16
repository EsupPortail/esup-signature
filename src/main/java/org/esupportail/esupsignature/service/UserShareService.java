package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.UserShareRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class UserShareService {

    private static final Logger logger = LoggerFactory.getLogger(UserShareService.class);

    @Resource
    private UserService userService;

    @Resource
    private UserShareRepository userShareRepository;

    @Resource
    private DataService dataService;

    @Resource
    private SignRequestService signRequestService;
    
    @Resource
    private FormService formService;
    
    @Resource
    private WorkflowService workflowService;

    public List<User> getSuUsers(Long authUserId) {
        List<User> suUsers = new ArrayList<>();
        for (UserShare userShare : userShareRepository.findByToUsersIdIn(Arrays.asList(authUserId))) {
            if(!suUsers.contains(userShare.getUser()) && checkUserShareDate(userShare)) {
                suUsers.add(userShare.getUser());
            }
        }
        return suUsers;
    }

    public List<UserShare> getByWorkflowId(Long id) {
        return userShareRepository.findByWorkflowId(id);
    }


    public void createUserShare(List<Long> formsIds, List<Long> workflowsIds, String[] types, List<User> userEmails, Date beginDate, Date endDate, User user) throws EsupSignatureUserException {
        UserShare userShare = new UserShare();
        userShare.setUser(user);
        List<ShareType> shareTypes = new ArrayList<>();
        for(String type : types) {
            shareTypes.add(ShareType.valueOf(type));
        }
        userShare.getShareTypes().addAll(shareTypes);
        for(Long formId : formsIds) {
            Form form = formService.getById(formId);
            if(form.getAuthorizedShareTypes().containsAll(shareTypes)) {
                userShare.setForm(form);
            } else {
                throw new EsupSignatureUserException("La délégation du formulaire : " + form.getTitle() + " n'est pas autorisée");
            }
        }
        for(Long workflowId : workflowsIds) {
            Workflow workflow = workflowService.getById(workflowId);
            if(userShareRepository.findByUserEppnAndWorkflow(user.getEppn(), workflow).size() == 0) {
                if (workflow.getAuthorizedShareTypes().containsAll(shareTypes)) {
                 userShare.setWorkflow(workflow);
                } else {
                 throw new EsupSignatureUserException("La délégation du circuit : " + workflow.getDescription() + " n'est pas autorisée");
                }
            } else {
                throw new EsupSignatureUserException("Une délégation pour ce circuit existe déjà, merci de la modifier");
            }
        }
        userShare.getToUsers().addAll(userEmails);
        userShare.setBeginDate(beginDate);
        userShare.setEndDate(endDate);
        userShareRepository.save(userShare);
    }

    public void addUserShare(User authUser, Long[] form, Long[] workflow, String[] types, String[] userEmails, String beginDate, String endDate) throws EsupSignatureUserException {
        List<User> users = new ArrayList<>();
        for (String userEmail : userEmails) {
            users.add(userService.getUserByEmail(userEmail));
        }
        Date beginDateDate = null;
        Date endDateDate = null;
        if (beginDate != null && endDate != null) {
            try {
                beginDateDate = new SimpleDateFormat("yyyy-MM-dd").parse(beginDate);
                endDateDate = new SimpleDateFormat("yyyy-MM-dd").parse(endDate);
            } catch (ParseException e) {
                logger.error("error on parsing dates");
            }
        }
        createUserShare(Arrays.asList(form), Arrays.asList(workflow), types, users, beginDateDate, endDateDate, authUser);
    }

    public void updateUserShare(User authUser, String[] types, String[] userEmails, String beginDate, String endDate, UserShare userShare) {
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
            for(String type : types) {
                if(authorizedShareTypes.contains(ShareType.valueOf(type))) {
                    userShare.getShareTypes().add(ShareType.valueOf(type));
                }
            }
            if (beginDate != null && endDate != null) {
                try {
                    userShare.setBeginDate(new SimpleDateFormat("yyyy-MM-dd").parse(beginDate));
                    userShare.setEndDate(new SimpleDateFormat("yyyy-MM-dd").parse(endDate));
                } catch (ParseException e) {
                    logger.error("error on parsing dates");
                }
            }
        }
    }

    public Boolean checkShare(Long fromUserId, Long toUserId, SignRequest signRequest, ShareType shareType) {
        if (signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null) {
            Workflow workflow = signRequest.getParentSignBook().getLiveWorkflow().getWorkflow();
            List<UserShare> userShares = userShareRepository.findByUserIdAndToUsersIdInAndWorkflowAndShareTypesContains(fromUserId, Arrays.asList(toUserId), workflow, shareType);
            for (UserShare userShare : userShares) {
                if (checkUserShareDate(userShare)) {
                    return true;
                }
            }
        }
        Data data = dataService.getBySignRequest(signRequest);
        if(data != null) {
            List<UserShare> userShares = userShareRepository.findByUserIdAndToUsersIdInAndFormAndShareTypesContains(fromUserId, Arrays.asList(toUserId), data.getForm(), shareType);
            for (UserShare userShare : userShares) {
                if (checkUserShareDate(userShare)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Boolean checkShare(Long fromUserId, Long toUserId, SignRequest signRequest) {
        return checkShare(fromUserId, toUserId, signRequest, ShareType.read)
            || checkShare(fromUserId, toUserId, signRequest, ShareType.sign)
            || checkShare(fromUserId, toUserId, signRequest, ShareType.create);
    }


        public Boolean checkShare(Long fromUserId, Long toUserId) {
        List<UserShare> userShares = userShareRepository.findByUserIdAndToUsersIdIn(fromUserId, Collections.singletonList(toUserId));
        for(UserShare userShare : userShares) {
            if (checkUserShareDate(userShare)) {
                return true;
            }
        }
        return false;
    }

    public Boolean checkFormShare(User fromUser, User toUser, ShareType shareType, Form form) {
        if(fromUser.equals(toUser)) {
            return true;
        }
        List<UserShare> userShares = getUserShares(fromUser.getId(), Collections.singletonList(toUser.getId()), shareType);
        if(shareType.equals(ShareType.sign) && userShares.size() > 0) {
            return true;
        }
        for(UserShare userShare : userShares) {
            if(userShare.getForm().equals(form) && checkUserShareDate(userShare)) {
                return true;
            }
        }
        return false;
    }

    public Boolean isOneShareByType(Long fromUserId, Long toUserId, ShareType shareType) {
        if(fromUserId.equals(toUserId)) {
            return true;
        }
        List<UserShare> userShares = getUserShares(fromUserId, Collections.singletonList(userService.getUserById(toUserId).getId()), shareType);
        if(userShares.size() > 0 ) {
            return true;
        }
        return false;
    }

    public List<UserShare> getUserShares(Long fromUserId, List<Long> toUsers, ShareType shareType) {
        return userShareRepository.findByUserIdAndToUsersIdInAndShareTypesContains(fromUserId, toUsers, shareType);
    }

    public Boolean checkUserShareDate(UserShare userShare) {
        Date today = new Date();
        if((userShare.getBeginDate() == null || today.after(userShare.getBeginDate())) && (userShare.getEndDate() == null || today.before(userShare.getEndDate()))) {
            return true;
        }
        return false;
    }

    public List<UserShare> getUserSharesByUser(Long authUserId) {
        return userShareRepository.findByUserId(authUserId);
    }

    public UserShare getById(Long id) {
        return userShareRepository.findById(id).get();
    }

    public void delete(UserShare userShare) {
        userShareRepository.delete(userShare);
    }

    public List<UserShare> getUserSharesByForm(Form form) {
        return userShareRepository.findByFormId(form.getId());
    }

    public List<UserShare> getByToUsersInAndShareTypesContains(List<Long> usersIds, ShareType shareType) {
        return userShareRepository.findByToUsersIdInAndShareTypesContains(usersIds, shareType);
    }

    public User checkShare(SignRequest signRequest, Long authUserId) {
        SignBook signBook = signRequest.getParentSignBook();
        if(signBook != null) {
            List<UserShare> userShares = userShareRepository.findByToUsersIdInAndShareTypesContains(Collections.singletonList(authUserId), ShareType.sign);
            for (UserShare userShare : userShares) {
                Workflow workflow = signRequest.getParentSignBook().getLiveWorkflow().getWorkflow();
                if(userShare.getWorkflow().equals(workflow) && signRequestService.checkUserSignRights(signRequest, userShare.getUser().getId(), authUserId)) {
                    return userShare.getUser();
                }
            }
            Data data = dataService.getBySignBook(signBook);
            if(data !=  null) {
                for (UserShare userShare : userShares) {
                    if (userShare.getForm().equals(data.getForm()) && signRequestService.checkUserSignRights(signRequest, userShare.getUser().getId(), authUserId)) {
                        return userShare.getUser();
                    }
                }
            }
        }
        return null;
    }

    public List<UserShare> getByUserAndToUsersInAndShareTypesContains(Long userId, User authUser, ShareType shareType) {
        return userShareRepository.findByUserIdAndToUsersIdInAndShareTypesContains(userId, Collections.singletonList(authUser.getId()), shareType);
    }


}
