package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.service.MobileSignTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
public class MobileSignController {

    private static final Logger logger = LoggerFactory.getLogger(MobileSignController.class);
    
    private final MobileSignTokenService mobileSignTokenService;
    private final GlobalProperties globalProperties;

    public MobileSignController(MobileSignTokenService mobileSignTokenService,
                                  GlobalProperties globalProperties) {
        this.mobileSignTokenService = mobileSignTokenService;
        this.globalProperties = globalProperties;
    }

}
