package org.esupportail.esupsignature.web;

import jakarta.persistence.PersistenceException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.esupportail.esupsignature.service.utils.CustomErrorService;
import org.hibernate.exception.ConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.ui.Model;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.Map;

@Controller
@RequestMapping("${server.error.path:${error.path:/error}}")
public class CustomErrorController implements ErrorController {

    private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);

    private final CustomErrorService customErrorService;

    public CustomErrorController(CustomErrorService customErrorService) {
        this.customErrorService = customErrorService;
    }

    @RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
    public String errorHtml(HttpServletRequest request, HttpServletResponse response, HttpSession httpSession, Model model) {
        HttpStatus status = customErrorService.getStatus(request);
                Map<String, Object> errors = new java.util.HashMap<>(Collections
                .unmodifiableMap(customErrorService.getErrorAttributes(request, customErrorService.getErrorAttributeOptions(request))));
        response.setStatus(status.value());
        if((int) errors.get("status") != 500) {
            if(!errors.containsKey("path")) {
                errors.put("path", "error");
            }
            logger.warn(errors.get("path").toString() + " : " + errors.get("status") + " " + errors.get("error").toString());
        }
        if(httpSession != null && httpSession.getId() != null) {
            model.addAttribute("userEppn", httpSession.getAttribute("userEppn"));
        }
        model.addAllAttributes(errors);
        return "error";
    }

    @RequestMapping
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        HttpStatus status = customErrorService.getStatus(request);
        if (status == HttpStatus.NO_CONTENT) {
            return new ResponseEntity<>(status);
        }
        Map<String, Object> body = customErrorService.getErrorAttributes(request, customErrorService.getErrorAttributeOptions(request));
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<String> mediaTypeNotAcceptable(HttpServletRequest request) {
        HttpStatus status = customErrorService.getStatus(request);
        return ResponseEntity.status(status).build();
    }

    @RequestMapping("/oauth2")
    public String oauth2Error(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description,
            @RequestParam(required = false) String state) {
        String redirectUrl = "/otp-access/oauth2?" +
                "error=" + (error != null ? java.net.URLEncoder.encode(error, java.nio.charset.StandardCharsets.UTF_8) : "") +
                "&error_description=" + (error_description != null ? java.net.URLEncoder.encode(error_description, java.nio.charset.StandardCharsets.UTF_8) : "") +
                "&state=" + (state != null ? java.net.URLEncoder.encode(state, java.nio.charset.StandardCharsets.UTF_8) : "");

        return "redirect:" + redirectUrl;
    }

    @ExceptionHandler({
            DataIntegrityViolationException.class,
            ConstraintViolationException.class,
            jakarta.validation.ConstraintViolationException.class,
            TransactionSystemException.class,
            PersistenceException.class
    })
    public String handleConstraintViolation(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        Throwable root = ex;
        while (root.getCause() != null) root = root.getCause();
        String errorMessage;
        if (root instanceof jakarta.validation.ConstraintViolationException validationException) {
            validationException.getConstraintViolations().forEach(violation -> logger.error(
                    "Validation Bean Validation en échec [field={}, message={}, invalidValueLength={}]",
                    violation.getPropertyPath(),
                    violation.getMessage(),
                    getInvalidValueLength(violation.getInvalidValue())
            ));
            errorMessage = "Un champ dépasse la taille maximale autorisée";
        } else if (isValueTooLong(root)) {
            logger.error("Dépassement de taille détecté lors d'une persistance JPA", ex);
            errorMessage = "Un champ dépasse la taille maximale autorisée";
        } else {
            errorMessage = (root instanceof PSQLException || root instanceof ConstraintViolationException)
                    ? "Action déjà effectuée ou clé dupliquée"
                    : "Erreur interne";
        }
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.CONFLICT.value());
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, errorMessage);
        request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, ex);
        response.setStatus(HttpStatus.CONFLICT.value());
        return "forward:/error";
    }

    private boolean isValueTooLong(Throwable throwable) {
        return throwable instanceof PSQLException psqlException
                && "22001".equals(psqlException.getSQLState());
    }

    private Integer getInvalidValueLength(Object invalidValue) {
        if (invalidValue instanceof String value) {
            return value.length();
        }
        return null;
    }

}
