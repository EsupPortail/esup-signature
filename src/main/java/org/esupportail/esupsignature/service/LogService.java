package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.LogRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
public class LogService {

    @Resource
    private LogRepository logRepository;

    @Resource
    private UserService userService;

    public List<Log> getRefuseLogs(Long id) {
        List<Log> logs = logRepository.findBySignRequestIdAndFinalStatus(id, SignRequestStatus.refused.name());
        return setUsers(logs);

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

    public Log create(Long id, String status, String action, String returnCode, String comment) {
        Log log = new Log();
        log.setSignRequestId(id);
        if(userService.getUserFromAuthentication() != null) {
            log.setEppn(userService.getUserFromAuthentication().getEppn());
            log.setEppnFor(userService.getSuEppn());
            log.setIp(userService.getUserFromAuthentication().getIp());
        }
        log.setInitialStatus(status);
        log.setLogDate(new Date());
        log.setAction(action);
        log.setReturnCode(returnCode);
        log.setComment(comment);
        logRepository.save(log);
        return log;
    }

}
