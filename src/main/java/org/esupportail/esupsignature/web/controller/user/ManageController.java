package org.esupportail.esupsignature.web.controller.user;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.export.DataExportService;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("user/manage")
@Controller
public class ManageController {

    private static final Logger logger = LoggerFactory.getLogger(ManageController.class);

    private final static byte[] EXCEL_UTF8_HACK = new byte[] { (byte)0xEF, (byte)0xBB, (byte)0xBF};

    @Resource
    private DataExportService dataExportService;

    @Resource
    private DataService dataService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private FormService formService;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private UserService userService;

    @GetMapping
    public String index(@ModelAttribute("authUserEppn") String authUserEppn, Model model) {
        List<Form> managedForms = formService.getFormByManagersContains(authUserEppn);
        model.addAttribute("managedForms", managedForms);
        return "user/manage/list";
    }

    @PreAuthorize("@preAuthorizeService.formManage(#id, #authUserEppn)")
    @GetMapping(value = "/form/{id}", produces="text/csv")
    public String list(@ModelAttribute("authUserEppn") String authUserEppn, @SortDefault(value = "createDate", direction = Direction.DESC) @PageableDefault(size = 10) Pageable pageable, @PathVariable Long id, Model model) {
        Form form = formService.getById(id);
        model.addAttribute("form", form);
        model.addAttribute("listManagedSignRequests", signRequestService.getSignRequestsByForm(form, pageable));
        return "user/manage/details";
    }

    @PreAuthorize("@preAuthorizeService.formManage(#id, #authUserEppn)")
    @GetMapping(value = "/form/{id}/datas/csv", produces="text/csv")
    public ResponseEntity<Void> getFormDatasCsv(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable Long id, HttpServletResponse response) {
        Form form = formService.getById(id);
        try {
            response.setContentType("text/csv; charset=utf-8");
            response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(form.getName().replace(" ", "-"), StandardCharsets.UTF_8.toString()) + ".csv");
            response.getOutputStream().write(EXCEL_UTF8_HACK);
            InputStream csvInputStream = dataExportService.getCsvDatasFromForms(Collections.singletonList(form));
            IOUtils.copy(csvInputStream, response.getOutputStream());
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            logger.error("get file error", e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @GetMapping("/form/{id}/start")
    @PreAuthorize("@preAuthorizeService.formManage(#id, #authUserEppn)")
    public String show(@PathVariable("id") Long id, @ModelAttribute("authUserEppn") String authUserEppn, @RequestParam String createByEmail, RedirectAttributes redirectAttributes) {
        User creator = userService.getUserByEmail(createByEmail);
        Data data = dataService.addData(id, creator.getEppn());
        try {
            Map<String, String> datas = new HashMap<>();
            signBookService.sendForSign(data.getId(), null, null, null, null, null, creator.getEppn(), creator.getEppn(), true, datas, null);
        } catch (EsupSignatureException | EsupSignatureIOException | EsupSignatureFsException e) {
            logger.error("error on create form instance", e);
        }
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Nouveau formulaire envoyé"));
        return "redirect:/user/manage";
    }


}
