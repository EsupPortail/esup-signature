package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.LogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
public class LogService {

    @Resource
    private LogRepository logRepository;

    @Resource
    private UserService userService;

    public List<Log> getByEppnAndSignRequestId(String eppn, Long id) {
        return logRepository.findByEppnAndSignRequestId(eppn, id);
    }

    public List<Log> getRefuseLogs(Long id) {
        List<Log> logs = logRepository.findBySignRequestIdAndFinalStatus(id, SignRequestStatus.refused.name());
        return setUsers(logs);
    }

    public List<Log> getByEppn(String eppn) {
        return logRepository.findByEppn(eppn);
    }

    public List<Log> getBySignRequestId(Long id) {
        return logRepository.findBySignRequestId(id);
    }

    public List<Log> getLogs(Long id) {
        List<Log> logs = logRepository.findBySignRequestIdAndPageNumberIsNotNullAndStepNumberIsNullAndCommentIsNotNull(id);
        return setUsers(logs);
    }

    public List<Log> getGlobalLogs(Long id) {
        List<Log> logs = logRepository.findBySignRequestIdAndStepNumberIsNotNullAndCommentIsNotNull(id);
        return setUsers(logs);
    }

    private List<Log> setUsers(List<Log> logs) {
        for (Log log :logs) {
            log.setUser(userService.getUserByEppn(log.getEppn()));
        }
        return logs;
    }

    public List<Log> getById(Long id) {
        return logRepository.findBySignRequestId(id);
    }

    @Transactional
    public List<Log> getFullBySignRequest(Long id) {
        List<Log> logs = logRepository.findBySignRequestId(id);
        for (Log log : logs) {
            User user = userService.getUserByEppn(log.getEppn());
            log.setUserName(user.getFirstname() + " " + user.getName());
        }
        return logs;
    }

    public Log create(Long id, String status, String action, String returnCode, String comment, String userEppn,  String authUserEppn) {
        Log log = new Log();
        log.setSignRequestId(id);
        log.setEppn(authUserEppn);
        log.setEppnFor(userEppn);
//        log.setIp(authUser.getIp());
        log.setInitialStatus(status);
        log.setLogDate(new Date());
        log.setAction(action);
        log.setReturnCode(returnCode);
        log.setComment(comment);
        logRepository.save(log);
        return log;
    }

    public void create(SignRequest signRequest, SignRequestStatus signRequestStatus, String action, String comment, String returnCode, Integer pageNumber, Integer posX, Integer posY, Integer stepNumber, String userEppn, String authUserEppn) {
        Log log = new Log();
        log.setSignRequestId(signRequest.getId());
        log.setSignRequestToken(signRequest.getToken());
        log.setEppn(authUserEppn);
        log.setEppnFor(userEppn);
//        log.setIp(authUser.getIp());
        log.setInitialStatus(signRequest.getStatus().toString());
        log.setLogDate(new Date());
        log.setAction(action);
        log.setReturnCode(returnCode);
        log.setComment(comment);
        if(pageNumber != null) {
            log.setPageNumber(pageNumber);
            log.setPosX(posX);
            log.setPosY(posY);
        }
        if(stepNumber != null) {
            log.setStepNumber(stepNumber);
        }
        if(signRequestStatus != null) {
            log.setFinalStatus(signRequestStatus.toString());
            signRequest.setStatus(signRequestStatus);
        } else {
            log.setFinalStatus(signRequest.getStatus().toString());
        }
        logRepository.save(log);
    }

    public void delete(Log log) {
        logRepository.delete(log);
    }

}
