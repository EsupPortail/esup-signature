package org.esupportail.esupsignature.service.utils;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class WebUtilsService {

    private HttpServletRequest request;

    @Autowired
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public void copyFileStreamToHttpResponse(String name, String contentType, String disposition, InputStream inputStream, HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.setContentType(contentType);
        httpServletResponse.setHeader("Content-Disposition", disposition + "; filename=" + URLEncoder.encode(name, StandardCharsets.UTF_8));
        IOUtils.copyLarge(inputStream, httpServletResponse.getOutputStream());
    }

    public String getClientIp() {

        String remoteAddr = "";

        if (request != null) {
            remoteAddr = request.getHeader("X-FORWARDED-FOR");
            if (remoteAddr == null || "".equals(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }

        return remoteAddr;
    }

}
