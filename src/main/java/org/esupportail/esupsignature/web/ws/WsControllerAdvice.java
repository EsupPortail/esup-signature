package org.esupportail.esupsignature.web.ws;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.ws"})
public class WsControllerAdvice {

    @ModelAttribute("wsAccessToken")
    public String getWsAccessToken(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if(authorizationHeader == null) return "";
        return authorizationHeader.replace("Bearer ", "");
    }


}
