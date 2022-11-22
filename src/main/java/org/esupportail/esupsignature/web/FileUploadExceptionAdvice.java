package org.esupportail.esupsignature.web;

import org.apache.commons.io.FileUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
public class FileUploadExceptionAdvice {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadExceptionAdvice.class);

    private final GlobalProperties globalProperties;

    public FileUploadExceptionAdvice(GlobalProperties globalProperties) {
        this.globalProperties = globalProperties;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(HttpServletRequest request,
                                         RedirectAttributes redirectAttributes,
                                         MaxUploadSizeExceededException exc) {
        logger.warn(exc.getMessage());
        redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Fichier trop volumineux. Taille supérieure à " + FileUtils.byteCountToDisplaySize(globalProperties.getMaxUploadSize())));
        return "redirect:" + request.getHeader("referer");
    }

}
