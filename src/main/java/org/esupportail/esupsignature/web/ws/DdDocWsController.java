package org.esupportail.esupsignature.web.ws;

import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.esupportail.esupsignature.service.utils.barcode.DdDocService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/ws/2d-doc")
public class DdDocWsController {

    private static final Logger logger = LoggerFactory.getLogger(DdDocWsController.class);

    @Resource
    private DdDocService ddDocService;

    @CrossOrigin
    @GetMapping(value = "{barcode}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<String> genetare2DDoc(@PathVariable("barcode") String barcode, HttpServletResponse response) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "image/svg+xml");
        headers.add("Content-Type", "image/png");
        MatrixToImageWriter.writeToStream(ddDocService.getQrCodeSvg(barcode), "PNG", response.getOutputStream());
        return null;
    }
}
