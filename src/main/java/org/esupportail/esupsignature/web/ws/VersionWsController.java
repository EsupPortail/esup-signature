package org.esupportail.esupsignature.web.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ws/version")
public class VersionWsController {

    private static final Logger logger = LoggerFactory.getLogger(VersionWsController.class);

    private final BuildProperties buildProperties;

    public VersionWsController(@Autowired(required = false) BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @GetMapping
    public String getVersion() {
        String version = "dev";
        if (buildProperties != null) {
            version = buildProperties.getVersion();
        }
        return version;
    }

}