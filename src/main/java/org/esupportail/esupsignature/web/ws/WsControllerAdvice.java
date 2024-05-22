package org.esupportail.esupsignature.web.ws;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.ws"})
public class WsControllerAdvice {

    private static final List<String> API_KEY_HEADERS = Arrays.asList("x-api-key", "x-apikey", "xapikey");

    @ModelAttribute("wsAccessToken")
    public String getWsAccessToken(HttpServletRequest request) {
        return getApiKey(request);
    }

    public String getApiKey(HttpServletRequest request) {
        Optional<String> headerName = Collections.list(request.getHeaderNames()).stream().filter(name -> API_KEY_HEADERS.contains(name.toLowerCase())).findFirst();
        return headerName.map(request::getHeader).orElse(null);
    }

}
