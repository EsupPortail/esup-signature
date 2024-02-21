package org.esupportail.esupsignature.web.ws;

import com.google.zxing.client.j2se.MatrixToImageWriter;
import jakarta.annotation.Resource;
import org.esupportail.esupsignature.service.utils.barcode.DdDocService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;

@RestController
@RequestMapping("/ws/2d-doc")
public class DdDocWsController {

    private static final Logger logger = LoggerFactory.getLogger(DdDocWsController.class);

    @Resource
    private DdDocService ddDocService;

    @CrossOrigin
    @GetMapping(value = "{barcode}", produces = MediaType.IMAGE_PNG_VALUE)
    public  ResponseEntity<byte[]> genetare2DDoc(@PathVariable("barcode") String barcode) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(ddDocService.getQrCodeSvg(barcode), "PNG", outputStream);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(outputStream.toByteArray());
    }
}
