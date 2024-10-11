package org.esupportail.esupsignature.web.ws;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.*;

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.ws"})
public class WsControllerAdvice implements ResponseBodyAdvice<Object> {

    private static final List<String> API_KEY_HEADERS = Arrays.asList("x-api-key", "x-apikey", "xapikey");

    private static final String HEADER_NAME = "X-Api-Version";

    private final BuildProperties buildProperties;

    public WsControllerAdvice(@Autowired(required = false) BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }


    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        String version = "dev";
        if (buildProperties != null) {
            version = buildProperties.getVersion();
        }
        response.getHeaders().add(HEADER_NAME, version);
        return body;
    }

    @ModelAttribute("xApiKey")
    public String getXApiKey(HttpServletRequest request) {
        List<String> possibleHeaders = new ArrayList<>(API_KEY_HEADERS);
        possibleHeaders.add("authorization");
        Optional<String> headerName = Collections.list(request.getHeaderNames()).stream()
                .filter(name -> possibleHeaders.contains(name.toLowerCase()))
                .findFirst();
        return headerName.map(request::getHeader).orElse(null);
    }

}
