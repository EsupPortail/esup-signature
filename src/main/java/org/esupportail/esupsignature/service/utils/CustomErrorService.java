package org.esupportail.esupsignature.service.utils;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@Service
public class CustomErrorService {

    private final ServerProperties serverProperties;

    private final ErrorAttributes errorAttributes;

    public CustomErrorService(ServerProperties serverProperties, ErrorAttributes errorAttributes) {
        this.serverProperties = serverProperties;
        this.errorAttributes = errorAttributes;
    }

    public ErrorAttributeOptions getErrorAttributeOptions(HttpServletRequest request) {
        ErrorAttributeOptions options = ErrorAttributeOptions.defaults();
        if (getErrorProperties().isIncludeException()) {
            options = options.including(ErrorAttributeOptions.Include.EXCEPTION);
        }
        if (isIncludeStackTrace(request)) {
            options = options.including(ErrorAttributeOptions.Include.STACK_TRACE);
        }
        if (isIncludeMessage(request)) {
            options = options.including(ErrorAttributeOptions.Include.MESSAGE);
        }
        if (isIncludeBindingErrors(request)) {
            options = options.including(ErrorAttributeOptions.Include.BINDING_ERRORS);
        }
        return options;
    }

    public boolean isIncludeStackTrace(HttpServletRequest request) {
        switch (getErrorProperties().getIncludeStacktrace()) {
            case ALWAYS:
                return true;
            case ON_PARAM:
                return getTraceParameter(request);
            default:
                return false;
        }
    }

    public boolean isIncludeMessage(HttpServletRequest request) {
        switch (getErrorProperties().getIncludeMessage()) {
            case ALWAYS:
                return true;
            case ON_PARAM:
                return getMessageParameter(request);
            default:
                return false;
        }
    }

    public boolean isIncludeBindingErrors(HttpServletRequest request) {
        switch (getErrorProperties().getIncludeBindingErrors()) {
            case ALWAYS:
                return true;
            case ON_PARAM:
                return getErrorsParameter(request);
            default:
                return false;
        }
    }

    public ErrorProperties getErrorProperties() {
        return serverProperties.getError();
    }


    public Map<String, Object> getErrorAttributes(HttpServletRequest request, ErrorAttributeOptions options) {
        WebRequest webRequest = new ServletWebRequest(request);
        return this.errorAttributes.getErrorAttributes(webRequest, options);
    }

    public boolean getTraceParameter(HttpServletRequest request) {
        return getBooleanParameter(request, "trace");
    }

    public boolean getMessageParameter(HttpServletRequest request) {
        return getBooleanParameter(request, "message");
    }

    public boolean getErrorsParameter(HttpServletRequest request) {
        return getBooleanParameter(request, "errors");
    }

    public boolean getBooleanParameter(HttpServletRequest request, String parameterName) {
        String parameter = request.getParameter(parameterName);
        if (parameter == null) {
            return false;
        }
        return !"false".equalsIgnoreCase(parameter);
    }

    public HttpStatus getStatus(HttpServletRequest request) {
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
