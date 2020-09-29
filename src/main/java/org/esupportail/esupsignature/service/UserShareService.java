package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.repository.FormRepository;
import org.esupportail.esupsignature.repository.UserShareRepository;
import org.esupportail.esupsignature.repository.WorkflowRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class UserShareService {

    @Resource
    private UserService userService;

    @Resource
    private UserShareRepository userShareRepository;

    @Resource
    private FormRepository formRepository;

    @Resource
    private WorkflowRepository workflowRepository;

    public List<User> getSuUsers(User authUser) {
        List<User> suUsers = new ArrayList<>();
        for (UserShare userShare : userShareRepository.findByToUsersIn(Arrays.asList(authUser))) {
            if(!suUsers.contains(userShare.getUser()) && checkUserShareDate(userShare)) {
                suUsers.add(userShare.getUser());
            }
        }
        return suUsers;
    }

    public void createUserShare(List<Long> forms, List<Long> workflows, String[] types, List<User> userEmails, Date beginDate, Date endDate, User user) {
        UserShare userShare = new UserShare();
        userShare.setUser(user);
        for(String type : types) {
            userShare.getShareTypes().add(ShareType.valueOf(type));
        }
        for(Long form : forms) {
            userShare.getForms().add(formRepository.findById(form).get());
        }
        for(Long workflow : workflows) {
            userShare.getWorkflows().add(workflowRepository.findById(workflow).get());
        }
        userShare.getToUsers().addAll(userEmails);
        userShare.setBeginDate(beginDate);
        userShare.setEndDate(endDate);
        userShareRepository.save(userShare);
    }


    public Boolean switchToShareUser(String eppn) {
        if(eppn == null || eppn.isEmpty()) {
            userService.setSuEppn(null);
            return true;
        }else {
            if(checkShare(userService.getUserByEppn(eppn), userService.getUserFromAuthentication())) {
                userService.setSuEppn(eppn);
                return true;
            }
        }
        return false;
    }

    public Boolean checkSignShare(User fromUser, User toUser, SignRequest signRequest, ShareType shareType) {
        if(signRequest.getParentSignBook() != null && signRequest.getParentSignBook().getWorkflowId() != null) {
            Workflow workflow = workflowRepository.findById(signRequest.getParentSignBook().getWorkflowId()).get();
            List<UserShare> userShares = userShareRepository.findByUserAndToUsersInAndWorkflowsContainsAndShareTypesContains(fromUser, Arrays.asList(toUser), workflow, shareType);
            for (UserShare userShare : userShares) {
                if (checkUserShareDate(userShare)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Boolean checkShare(User fromUser, User toUser) {
        List<UserShare> userShares = userShareRepository.findByUserAndToUsersIn(fromUser, Arrays.asList(toUser));
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
        List<UserShare> userShares = userShareRepository.findByUserAndToUsersInAndShareTypesContains(fromUser, Arrays.asList(toUser), shareType);
        if(shareType.equals(ShareType.sign) && userShares.size() > 0) {
            return true;
        }
        for(UserShare userShare : userShares) {
            if(userShare.getForms().contains(form) && checkUserShareDate(userShare)) {
                return true;
            }
        }
        return false;
    }

    public Boolean isOneShareByType(User fromUser, User toUser, ShareType shareType) {
        if(fromUser.equals(toUser)) {
            return true;
        }
        List<UserShare> userShares = userShareRepository.findByUserAndToUsersInAndShareTypesContains(fromUser, Arrays.asList(toUser), shareType);
        if(userShares.size() > 0 ) {
            return true;
        }
        return false;
    }

    public Boolean checkUserShareDate(UserShare userShare) {
        Date today = new Date();
        if((userShare.getBeginDate() == null || today.after(userShare.getBeginDate())) && (userShare.getEndDate() == null || today.before(userShare.getEndDate()))) {
            return true;
        }
        return false;
    }

}
