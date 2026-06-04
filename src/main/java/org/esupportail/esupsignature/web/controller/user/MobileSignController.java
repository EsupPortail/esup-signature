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

    @GetMapping("/user/users/mobile-sign/generate-token")
    @ResponseBody
    public ResponseEntity<Map<String, String>> generateToken(@ModelAttribute("authUserEppn") String authUserEppn) {
        try {
            String token = mobileSignTokenService.createToken(authUserEppn);
            String mobileSignUrl = buildMobileSignUrl(token);
            
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("url", mobileSignUrl);
            response.put("qrcodeUrl", "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + java.net.URLEncoder.encode(mobileSignUrl, "UTF-8"));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error generating mobile sign token: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String buildMobileSignUrl(String token) {
        String baseUrl = globalProperties.getRootUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "http://" + globalProperties.getDomain();
        }
        // Make sure baseUrl doesn't end with /
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/public/mobile-sign/" + token;
    }
}
