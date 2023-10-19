package org.esupportail.esupsignature.service.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class WebUtilsService {

    private final HttpServletRequest request;

    private final GlobalProperties globalProperties;

    public WebUtilsService(HttpServletRequest request, GlobalProperties globalProperties) {
        this.request = request;
        this.globalProperties = globalProperties;
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
        CSVFormat.Builder csvFormat = CSVFormat.Builder.create(CSVFormat.EXCEL).setHeader(headers).setQuoteMode(QuoteMode.ALL);
        if (globalProperties.getCsvSeparator() != null) {
            csvFormat.setDelimiter(globalProperties.getCsvSeparator());
        }
        if (globalProperties.getCsvQuote() != null) {
            csvFormat.setQuote(globalProperties.getCsvQuote());
        }
        CSVPrinter printer = new CSVPrinter(out, csvFormat.build());
        for (Map<String, String> map : list) {
            printer.printRecord(map.values());
        }
        out.flush();
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

}
