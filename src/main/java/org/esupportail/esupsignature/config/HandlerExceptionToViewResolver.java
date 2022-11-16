package org.esupportail.esupsignature.config;

import org.apache.commons.io.FileUtils;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HandlerExceptionToViewResolver implements HandlerExceptionResolver {

    private static final Logger logger = LoggerFactory.getLogger(HandlerExceptionToViewResolver.class);

    private final GlobalProperties globalProperties;

    public HandlerExceptionToViewResolver(GlobalProperties globalProperties) {
        this.globalProperties = globalProperties;
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex instanceof MaxUploadSizeExceededException) {
            logger.warn(ex.getMessage());
            ModelAndView modelAndView = new ModelAndView("redirect:" + request.getHeader("referer"));
            FlashMap flashMap = new FlashMap();
            flashMap.put("message", new JsonMessage("error", "Fichier trop volumineux. Taille supérieure à " + FileUtils.byteCountToDisplaySize(globalProperties.getMaxUploadSize())));
            new SessionFlashMapManager().saveOutputFlashMap(flashMap, request, response);
            return modelAndView;        }
        return null;
    }
}
