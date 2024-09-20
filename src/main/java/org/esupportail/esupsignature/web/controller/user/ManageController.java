package org.esupportail.esupsignature.web.controller.user;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.export.DataExportService;
import org.esupportail.esupsignature.service.export.WorkflowExportService;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

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
    private UserService userService;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private WorkflowExportService workflowExportService;

    @GetMapping
    public String index(@ModelAttribute("authUserEppn") String authUserEppn, Model model) {
        List<Form> managedForms = formService.getFormByManagersContains(authUserEppn);
        model.addAttribute("managedForms", managedForms.stream().map(Form::getWorkflow).collect(Collectors.toList()));
        model.addAttribute("managedWorkflows", workflowService.getWorkflowByManagersContains(authUserEppn));
        return "user/manage/list";
    }

    @PreAuthorize("@preAuthorizeService.workflowManage(#id, #authUserEppn)")
    @GetMapping(value = "/workflow/{id}/signbooks", produces="text/csv")
    public String list(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                       @RequestParam(value = "statusFilter", required = false) String statusFilter,
                       @RequestParam(value = "recipientsFilter", required = false) String recipientsFilter,
                       @RequestParam(value = "docTitleFilter", required = false) String docTitleFilter,
                       @RequestParam(value = "creatorFilter", required = false) String creatorFilter,
                       @RequestParam(value = "dateFilter", required = false) String dateFilter,
                       @SortDefault(value = "createDate", direction = Sort.Direction.DESC) @PageableDefault(size = 10) Pageable pageable, @PathVariable Long id, Model model) {
        if(statusFilter == null || statusFilter.equals("all")) statusFilter = "";
        if(creatorFilter == null || creatorFilter.isEmpty() || creatorFilter.equals("all")) {
            creatorFilter = "%";
        }
        if(docTitleFilter != null && (docTitleFilter.isEmpty() || docTitleFilter.equals("all"))) {
            docTitleFilter = null;
        }
        if(recipientsFilter != null && (recipientsFilter.isEmpty() || recipientsFilter.equals("all"))) {
            recipientsFilter = null;
        }
        Workflow workflow = workflowService.getById(id);
        model.addAttribute("statuses", SignRequestStatus.values());
        model.addAttribute("docTitleFilter", docTitleFilter);
        model.addAttribute("dateFilter", dateFilter);
        model.addAttribute("recipientsFilter", recipientsFilter);
        model.addAttribute("creatorFilter", creatorFilter);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("workflow", workflow);
        Page<SignBook> signBooks = signBookService.getSignBooks(userEppn, authUserEppn, statusFilter, recipientsFilter, workflow.getTitle(), docTitleFilter, creatorFilter, dateFilter, pageable);
        model.addAttribute("signBooks", signBooks);
        LinkedHashSet<String> docTitles = new LinkedHashSet<>();
        if(statusFilter.isEmpty() && docTitleFilter == null && recipientsFilter == null) {
            docTitles.addAll(signBookService.getAllDocTitles(userEppn));
        } else {
            docTitles.addAll(signBooks.stream().map(SignBook::getSubject).toList());
        }
        model.addAttribute("docTitles", docTitles);
        model.addAttribute("creators", signBooks.stream().map(SignBook::getCreateBy).distinct().collect(Collectors.toList()));
        model.addAttribute("signRequestRecipients", signBookService.getRecipientsNames(userEppn).stream().filter(Objects::nonNull).collect(Collectors.toList()));
        return "user/manage/details";
    }

    @PreAuthorize("@preAuthorizeService.workflowManage(#id, #authUserEppn)")
    @GetMapping(value = "/form/{id}/datas/csv", produces="text/csv")
    public ResponseEntity<Void> getFormDatasCsv(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable Long id, HttpServletResponse response) {
        Workflow workflow = workflowService.getById(id);
        try {
            response.setContentType("text/csv; charset=utf-8");
            response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(workflow.getName().replace(" ", "-"), StandardCharsets.UTF_8.toString()) + ".csv");
            response.getOutputStream().write(EXCEL_UTF8_HACK);
            InputStream csvInputStream = dataExportService.getCsvDatasFromForms(Collections.singletonList(workflow));
            IOUtils.copy(csvInputStream, response.getOutputStream());
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            logger.error("get file error", e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PreAuthorize("@preAuthorizeService.workflowManage(#id, #authUserEppn)")
    @GetMapping(value = "/workflow/{id}/datas/csv", produces="text/csv")
    public ResponseEntity<Void> getWorkflowDatasCsv(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable Long id, HttpServletResponse response) {
        Workflow workflow = workflowService.getById(id);
        try {
            response.setContentType("text/csv; charset=utf-8");
            response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(workflow.getName().replace(" ", "-"), StandardCharsets.UTF_8.toString()) + ".csv");
            response.getOutputStream().write(EXCEL_UTF8_HACK);
            InputStream csvInputStream = workflowExportService.getCsvDatasFromWorkflow(Collections.singletonList(workflow));
            IOUtils.copy(csvInputStream, response.getOutputStream());
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            logger.error("get file error", e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping("/form/{id}/start")
    @PreAuthorize("@preAuthorizeService.workflowManage(#id, #authUserEppn)")
    public String show(@PathVariable("id") Long id, @ModelAttribute("authUserEppn") String authUserEppn, @RequestParam String createByEmail, RedirectAttributes redirectAttributes) {
        User creator = userService.getUserByEmail(createByEmail);
        Data data = dataService.addData(id, creator.getEppn());
        try {
            Map<String, String> datas = new HashMap<>();
            signBookService.sendForSign(data.getId(), null, null, null,  creator.getEppn(), creator.getEppn(), true, datas, null, null, null, true, null);
        } catch (EsupSignatureRuntimeException e) {
            logger.error("error on create form instance", e);
        }
        redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Nouveau formulaire envoyé"));
        return "redirect:/user/manage";
    }


}
