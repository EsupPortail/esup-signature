package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.LogRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

@Service
public class LogService {

    private static final Logger logger = LoggerFactory.getLogger(LogService.class);

    @Resource
    private LogRepository logRepository;

    @Resource
    private UserService userService;

    private HttpServletRequest request;

    @Autowired(required = false)
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    @Resource
    private SignRequestRepository signRequestRepository;

    public List<Log> getRefuseLogs(Long id) {
        List<Log> logs = logRepository.findBySignRequestIdAndFinalStatus(id, SignRequestStatus.refused.name());
        return setUsers(logs);
    }

    public List<Log> getSignLogs(Long id) {
        List<Log> logs = logRepository.findBySignRequestIdAndFinalStatus(id, SignRequestStatus.signed.name());
        return setUsers(logs);
    }

    public Page<Log> getAllLogs(Pageable pageable) {
        Page<Log> logs = logRepository.findAll(pageable);
        return logs;
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

    private List<Log> setUsers(List<Log> logs) {
        for (Log log :logs) {
            log.setUser(userService.getByEppn(log.getEppn()));
        }
        return logs;
    }

    public List<Log> getBySignRequest(Long id) {
        return logRepository.findBySignRequestId(id);
    }

    @Transactional
    public List<Log> getFullBySignRequest(Long id) {
        List<Log> logs = logRepository.findBySignRequestId(id);
        for (Log log : logs) {
            if(log.getEppn() != null) {
                User user = userService.getByEppn(log.getEppn());
                log.setUser(user);
            }
        }
        return logs;
    }

    public Log create(Long id, String status, String action, String returnCode, String comment, String userEppn,  String authUserEppn) {
        Log log = new Log();
        log.setSignRequestId(id);
        log.setEppn(authUserEppn);
        User user = userService.getByEppn(authUserEppn);
        log.setUser(user);
        log.setEppnFor(userEppn);
        setClientIp(log);
        log.setInitialStatus(status);
        log.setLogDate(new Date());
        log.setAction(action);
        log.setReturnCode(returnCode);
        log.setComment(comment);
        logRepository.save(log);
        return log;
    }

    public void setClientIp(Log log) {
        if (request != null) {
            try {
                log.setIp(request.getRemoteAddr());
            } catch (IllegalStateException e) {
                logger.debug("unable to get IP (maybe it was launched by scheduler)");
            }
        }
    }

    @Transactional
    public Log create(Long signRequestId, SignRequestStatus signRequestStatus, String action, String comment, String returnCode, Integer pageNumber, Integer posX, Integer posY, Integer stepNumber, String userEppn, String authUserEppn) {
        SignRequest signRequest = signRequestRepository.findById(signRequestId).get();
        Log log = new Log();
        log.setSignRequestId(signRequest.getId());
        log.setSignRequestToken(signRequest.getToken());
        log.setEppn(authUserEppn);
        log.setEppnFor(userEppn);
        User user = userService.getByEppn(authUserEppn);
        log.setUser(user);
        log.setEppnFor(userEppn);
        setClientIp(log);
        log.setInitialStatus(signRequest.getStatus().toString());
        log.setLogDate(new Date());
        log.setAction(action);
        log.setReturnCode(returnCode);
        log.setComment(comment);
        if(pageNumber != null) {
            log.setPageNumber(pageNumber);
            log.setPosX(Math.round(posX * .75f));
            log.setPosY(Math.round(posY * .75f));
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
        return log;
    }

    public void delete(Log log) {
        logRepository.delete(log);
    }

    public String getIp() {
        if(request != null) {
            return request.getRemoteAddr();
        }
        return "?";
    }

    @Transactional
    public void anonymize(String userEppn) {
        List<Log> logs = logRepository.findByEppn(userEppn);
        for(Log log : logs) {
            log.setEppn("anonymous");
        }
    }

}
