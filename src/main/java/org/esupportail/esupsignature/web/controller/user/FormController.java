package org.esupportail.esupsignature.web.controller.user;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.repository.FormRepository;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.List;

@Controller
@RequestMapping("/user")
public class FormController {

    @ModelAttribute(value = "globalProperties")
    public GlobalProperties getGlobalProperties() {
        return this.globalProperties;
    }

    @Resource
    private GlobalProperties globalProperties;

    @Resource
    private FormRepository formRepository;

    @Resource
    private PdfService pdfService;

    @GetMapping("forms/{id}/get-image")
    public ResponseEntity<Void> getImagePdfAsByteArray(@PathVariable("id") Long id, HttpServletResponse response) throws Exception {
        Form form = formRepository.findById(id).get();
        InputStream in = pdfService.pageAsInputStream(form.getDocument().getInputStream(), 0);
        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        IOUtils.copy(in, response.getOutputStream());
        in.close();
        return new ResponseEntity<>(HttpStatus.OK);
    }

}