package org.esupportail.esupsignature.service;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.LogRepository;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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

    @Resource
    private SignBookRepository signBookRepository;

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
        return logs.stream().sorted(Comparator.comparing(Log::getLogDate)).toList();
    }

    @Transactional
    public List<Log> getFullByToken(String token) {
        List<Log> logs = logRepository.findBySignRequestToken(token);
        for (Log log : logs) {
            if(log.getEppn() != null) {
                User user = userService.getByEppn(log.getEppn());
                log.setUser(user);
            }
        }
        return logs.stream().sorted(Comparator.comparing(Log::getLogDate)).toList();
    }

    @Transactional
    public Log create(Long id, String subject, String workflowName, SignRequestStatus signRequestStatus, String action, String comment, String returnCode, Integer pageNumber, Integer posX, Integer posY, Integer stepNumber, String userEppn, String authUserEppn) {
        Optional<SignRequest> signRequest = signRequestRepository.findById(id);
        Log log = new Log();
        log.setSignRequestId(id);
        if(signRequest.isPresent()) {
            action = "signRequest : " + action;
            log.setSignRequestToken(signRequest.get().getToken());
            log.setInitialStatus(signRequest.get().getStatus().name());
            if(signRequestStatus != null) {
                log.setFinalStatus(signRequestStatus.toString());
                signRequest.get().setStatus(signRequestStatus);
            } else {
                log.setFinalStatus(signRequest.get().getStatus().name());
            }
        } else  {
            action = "signBook : " + action;
            SignBook signBook = signBookRepository.findById(id).get();
            log.setInitialStatus(signBook.getStatus().name());
        }
        log.setEppn(authUserEppn);
        log.setEppnFor(userEppn);
        log.setSubject(subject);
        log.setWorkflowName(workflowName);
        User user = userService.getByEppn(authUserEppn);
        if(user == null) {
            user = userService.getByEppn(userEppn);
        }
        log.setUser(user);
        setClientIp(log);
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
