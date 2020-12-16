package org.esupportail.esupsignature.web.controller.user;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.FormService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.export.DataExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RequestMapping("user/manage")
@Controller
public class ManageController {

    private static final Logger logger = LoggerFactory.getLogger(ManageController.class);

    @Resource
    private DataExportService dataExportService;

    @Resource
    private FormService formService;

    @Resource
    private UserService userService;

    @GetMapping
    public String index(@ModelAttribute("authUserId") Long authUserId) {
        return "user/manage";
    }

    @GetMapping(value = "/form/{name}/datas/csv", produces="text/csv")
    public ResponseEntity<Void> getFormDatasCsv(@ModelAttribute("authUserId") Long authUserId, @PathVariable String name, HttpServletResponse response) {
        User authUser = userService.getById(authUserId);
        List<Form> formManaged = formService.getFormByManagersContains(authUser.getEmail());
        List<Form> forms = formService.getFormByName(name);
        if (forms.size() > 0 && formManaged.contains(forms.get(0))) {
            try {
                response.setContentType("text/csv; charset=utf-8");
                response.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(forms.get(0).getName().replace(" ", "-"), StandardCharsets.UTF_8.toString()) + ".csv");
                InputStream csvInputStream = dataExportService.getCsvDatasFromForms(forms);
                IOUtils.copy(csvInputStream, response.getOutputStream());
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (Exception e) {
                logger.error("get file error", e);
            }
        } else {
            logger.warn("form " + name + " not found");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
