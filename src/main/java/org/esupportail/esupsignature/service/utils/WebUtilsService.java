package org.esupportail.esupsignature.service.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class WebUtilsService {

    private final HttpServletRequest request;

    public WebUtilsService(HttpServletRequest request) {
        this.request = request;
    }


    public void copyFileStreamToHttpResponse(String name, String contentType, String disposition, InputStream inputStream, HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.setContentType(contentType);
        String url = URLEncoder.encode(name, StandardCharsets.UTF_8);
        httpServletResponse.setHeader("Content-Disposition", disposition + "; filename=" + URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20"));
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

    public InputStream mapListToCSV(List<Map<String, String>> list) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputStreamWriter out = new OutputStreamWriter(outputStream);
        String[] headers = list.stream().flatMap(map -> map.keySet().stream()).distinct().toArray(String[]::new);
        CSVPrinter printer = new CSVPrinter(out, CSVFormat.Builder.create(CSVFormat.EXCEL).setHeader(headers).build());
        for (Map<String, String> map : list) {
            printer.printRecord(map.values());
        }
        out.flush();
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

}
