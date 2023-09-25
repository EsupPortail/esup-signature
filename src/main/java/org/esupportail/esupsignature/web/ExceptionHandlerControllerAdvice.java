package org.esupportail.esupsignature.web;

import org.apache.commons.io.FileUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.dao.DuplicateKeyException;

@ControllerAdvice
public class ExceptionHandlerControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandlerControllerAdvice.class);

    private final GlobalProperties globalProperties;

    public ExceptionHandlerControllerAdvice(GlobalProperties globalProperties) {
        this.globalProperties = globalProperties;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseBody
    public String handleMaxSizeException(RedirectAttributes redirectAttributes,
                                         MaxUploadSizeExceededException exc) {
        logger.warn(exc.getMessage());
        redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Fichier trop volumineux. Taille supérieure à " + FileUtils.byteCountToDisplaySize(globalProperties.getMaxUploadSize())));
        return "{\"error\" : \"Fichier trop volumineux. Taille supérieure à " + FileUtils.byteCountToDisplaySize(globalProperties.getMaxUploadSize()) + "\"}";
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseBody
    public String handleDuplicateKeyException(RedirectAttributes redirectAttributes,
                                         MaxUploadSizeExceededException exc) {
        logger.warn(exc.getMessage());
        if(exc.getMessage().contains("SPRING_SESSION_ATTRIBUTES")) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Une erreur est survenue lors de la réouverture de votre session. Vous êtes redirigé vers la page d'accueil."));
            return "redirect:/user";
        } else {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", exc.getMessage()));
            return "redirect:/user";
        }
    }

}
