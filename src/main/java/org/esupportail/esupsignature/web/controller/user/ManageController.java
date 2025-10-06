package org.esupportail.esupsignature.web.controller.user;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.export.DataExportService;
import org.esupportail.esupsignature.service.export.WorkflowExportService;
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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RequestMapping("user/manage")
@Controller
public class ManageController {

    private static final Logger logger = LoggerFactory.getLogger(ManageController.class);

    private static final byte[] EXCEL_UTF8_HACK = new byte[] { (byte)0xEF, (byte)0xBB, (byte)0xBF};

    private final DataExportService dataExportService;

    private final DataService dataService;

    private final SignBookService signBookService;

    private final FormService formService;

    private final UserService userService;

    private final WorkflowService workflowService;

    private final WorkflowExportService workflowExportService;

    private final SignRequestService signRequestService;

    private final ChartsService chartsService;

    public ManageController(DataExportService dataExportService, DataService dataService, SignBookService signBookService, FormService formService, UserService userService, WorkflowService workflowService, WorkflowExportService workflowExportService, SignRequestService signRequestService, ChartsService chartsService) {
        this.dataExportService = dataExportService;
        this.dataService = dataService;
        this.signBookService = signBookService;
        this.formService = formService;
        this.userService = userService;
        this.workflowService = workflowService;
        this.workflowExportService = workflowExportService;
        this.signRequestService = signRequestService;
        this.chartsService = chartsService;
    }


    @GetMapping
    public String index(@ModelAttribute("authUserEppn") String authUserEppn, Model model) {
        List<Form> managedForms = formService.getFormByManagersContains(authUserEppn);
        model.addAttribute("managedForms", managedForms.stream().map(Form::getWorkflow).collect(Collectors.toList()));
        List<Workflow> workflows = workflowService.getWorkflowByManagersContains(authUserEppn);
        model.addAttribute("managedWorkflows", workflows);
        List<String> workflowsCharts = new ArrayList<>();
        for(Workflow workflow : workflows) {
            workflowsCharts.add(chartsService.getWorkflowSignBooksStatus(workflow.getId()));
        }
        model.addAttribute("chartWorkflowSignBooksStatus", workflowsCharts);
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
        SignRequestStatus signRequestStatus = null;
        if(StringUtils.hasText(statusFilter) && !statusFilter.equals("all")) {
            signRequestStatus = SignRequestStatus.valueOf(statusFilter);
        }
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
        model.addAttribute("statuses", SignRequestStatus.activeValues());
        model.addAttribute("docTitleFilter", docTitleFilter);
        model.addAttribute("dateFilter", dateFilter);
        model.addAttribute("recipientsFilter", recipientsFilter);
        model.addAttribute("creatorFilter", creatorFilter);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("workflow", workflow);
        Page<SignBook> signBooks = signBookService.getSignBooksForManagers(signRequestStatus, recipientsFilter, workflow.getId(), docTitleFilter, creatorFilter, dateFilter, pageable);
        model.addAttribute("signBooks", signBooks);
//        model.addAttribute("creators", signBookService.getSignBooksForManagersCreators(workflow.getId()));
        return "user/manage/details";
    }

    @GetMapping(value = "/workflow/{id}/search-doc-titles")
    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn)")
    @ResponseBody
    public List<String> searchDocTitles(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                @PathVariable Long id, @RequestParam(value = "searchString", required = false) String searchString) {
        return signBookService.getSignBooksForManagersSubjects(id, searchString);
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
            signBookService.sendForSign(data.getId(), null, null, null,  creator.getEppn(), creator.getEppn(), true, datas, null, null, true, null);
        } catch (EsupSignatureRuntimeException e) {
            logger.error("error on create form instance", e);
        }
        redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Nouveau formulaire envoyé"));
        return "redirect:/user/manage";
    }

    @PreAuthorize("@preAuthorizeService.workflowManage(#workflowId, #authUserEppn)")
    @GetMapping(value = "/workflow/{workflowId}/get-last-file/{signRequestId}")
    public ResponseEntity<Void> getLastFile(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("workflowId") Long workflowId, @PathVariable("signRequestId") Long signRequestId, HttpServletResponse httpServletResponse) {
        try {
            signRequestService.getToSignFileResponse(signRequestId, "attachment", httpServletResponse, true);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("get file error", e);
        }
        return ResponseEntity.notFound().build();
    }

}
