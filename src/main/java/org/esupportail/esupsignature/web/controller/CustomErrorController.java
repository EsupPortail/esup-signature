package org.esupportail.esupsignature.web.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.Collections;
import java.util.Map;

@Controller
@RequestMapping("${server.error.path:${error.path:/error}}")
public class CustomErrorController implements ErrorController {

    private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);

    private final ServerProperties serverProperties;

    private final ErrorAttributes errorAttributes;

    public CustomErrorController(ServerProperties serverProperties, ErrorAttributes errorAttributes) {
        this.serverProperties = serverProperties;
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
    public String errorHtml(HttpServletRequest request, HttpServletResponse response, Model model) {
        HttpStatus status = getStatus(request);
        Map<String, Object> errors = new java.util.HashMap<>(Collections
                .unmodifiableMap(getErrorAttributes(request, getErrorAttributeOptions(request, MediaType.TEXT_HTML))));
        response.setStatus(status.value());
        if((int) errors.get("status") != 500) {
            if(!errors.containsKey("path")) {
                errors.put("path", "error");
            }
            logger.warn(errors.get("path").toString() + " : " + errors.get("status") + " " + errors.get("error").toString());
        }
        model.addAllAttributes(errors);
        return "error";
    }

    @RequestMapping
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        HttpStatus status = getStatus(request);
        if (status == HttpStatus.NO_CONTENT) {
            return new ResponseEntity<>(status);
        }
        Map<String, Object> body = getErrorAttributes(request, getErrorAttributeOptions(request, MediaType.ALL));
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<String> mediaTypeNotAcceptable(HttpServletRequest request) {
        HttpStatus status = getStatus(request);
        return ResponseEntity.status(status).build();
    }

    protected ErrorAttributeOptions getErrorAttributeOptions(HttpServletRequest request, MediaType mediaType) {
        ErrorAttributeOptions options = ErrorAttributeOptions.defaults();
        if (getErrorProperties().isIncludeException()) {
            options = options.including(ErrorAttributeOptions.Include.EXCEPTION);
        }
        if (isIncludeStackTrace(request, mediaType)) {
            options = options.including(ErrorAttributeOptions.Include.STACK_TRACE);
        }
        if (isIncludeMessage(request, mediaType)) {
            options = options.including(ErrorAttributeOptions.Include.MESSAGE);
        }
        if (isIncludeBindingErrors(request, mediaType)) {
            options = options.including(ErrorAttributeOptions.Include.BINDING_ERRORS);
        }
        return options;
    }

    protected boolean isIncludeStackTrace(HttpServletRequest request, MediaType produces) {
        switch (getErrorProperties().getIncludeStacktrace()) {
            case ALWAYS:
                return true;
            case ON_PARAM:
                return getTraceParameter(request);
            default:
                return false;
        }
    }

    protected boolean isIncludeMessage(HttpServletRequest request, MediaType produces) {
        switch (getErrorProperties().getIncludeMessage()) {
            case ALWAYS:
                return true;
            case ON_PARAM:
                return getMessageParameter(request);
            default:
                return false;
        }
    }

    protected boolean isIncludeBindingErrors(HttpServletRequest request, MediaType produces) {
        switch (getErrorProperties().getIncludeBindingErrors()) {
            case ALWAYS:
                return true;
            case ON_PARAM:
                return getErrorsParameter(request);
            default:
                return false;
        }
    }

    protected ErrorProperties getErrorProperties() {
        return serverProperties.getError();
    }


    protected Map<String, Object> getErrorAttributes(HttpServletRequest request, ErrorAttributeOptions options) {
        WebRequest webRequest = new ServletWebRequest(request);
        return this.errorAttributes.getErrorAttributes(webRequest, options);
    }

    protected boolean getTraceParameter(HttpServletRequest request) {
        return getBooleanParameter(request, "trace");
    }

    protected boolean getMessageParameter(HttpServletRequest request) {
        return getBooleanParameter(request, "message");
    }

    protected boolean getErrorsParameter(HttpServletRequest request) {
        return getBooleanParameter(request, "errors");
    }

    protected boolean getBooleanParameter(HttpServletRequest request, String parameterName) {
        String parameter = request.getParameter(parameterName);
        if (parameter == null) {
            return false;
        }
        return !"false".equalsIgnoreCase(parameter);
    }

    protected HttpStatus getStatus(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        try {
            return HttpStatus.valueOf(statusCode);
        }
        catch (Exception ex) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
