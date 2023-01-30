package org.esupportail.esupsignature.web.controller.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.FieldType;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.service.FieldService;
import org.esupportail.esupsignature.service.FormService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFill;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFillService;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Controller
@RequestMapping("/manager/forms")
public class FormManagerController {

    private static final Logger logger = LoggerFactory.getLogger(FormManagerController.class);

    @ModelAttribute("managerMenu")
    public String getmanagersMenu() {
        return "active";
    }

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "forms";
    }

    @Resource
    private FormService formService;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private UserService userService;

    @Resource
    private PreFillService preFillService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private FieldService fieldService;

    @GetMapping()
    @PreAuthorize("@preAuthorizeService.isManager(#authUserEppn)")
    public String list(@ModelAttribute("authUserEppn") String authUserEppn, Model model) {
        Set<Form> forms = new HashSet<>();
        User manager = userService.getByEppn(authUserEppn);
        for (String role : manager.getManagersRoles()) {
            forms.addAll(formService.getManagerForms(authUserEppn));
        }
        model.addAttribute("forms", forms);
        model.addAttribute("roles", userService.getManagersRoles(authUserEppn));
        model.addAttribute("targetTypes", DocumentIOType.values());
        model.addAttribute("workflowTypes", workflowService.getManagerWorkflows(authUserEppn));
        model.addAttribute("preFillTypes", preFillService.getPreFillValues());
        return "managers/forms/list";
    }

    @GetMapping("{id}")
    @PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn)")
    public String show(@PathVariable("id") Long id, Model model, @ModelAttribute("authUserEppn") String authUserEppn) {
        Form form = formService.getById(id);
        model.addAttribute("form", form);
        model.addAttribute("workflow", form.getWorkflow());
        PreFill preFill = preFillService.getPreFillServiceByName(form.getPreFillType());
        if(preFill != null) {
            model.addAttribute("preFillTypes", preFill.getTypes());
        } else {
            model.addAttribute("preFillTypes", new HashMap<>());
        }
        model.addAttribute("document", form.getDocument());
        return "managers/forms/show";
    }

    @PostMapping()
    @PreAuthorize("@preAuthorizeService.isManager(#authUserEppn)")
    public String postForm(@RequestParam("name") String name,
                           @RequestParam(name = "managerRole") String managerRole,
                           @RequestParam("fieldNames[]") String[] fieldNames,
                           @RequestParam(required = false) Boolean publicUsage, RedirectAttributes redirectAttributes) throws IOException {
        try {
            Form form = formService.createForm(null, name, null, null, null, null, publicUsage, fieldNames, null);
            form.setManagerRole(managerRole);
            return "redirect:/manager/forms/" + form.getId();
        } catch (EsupSignatureException e) {
            logger.error(e.getMessage());
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
            return "redirect:/manager/forms/";
        }
    }

    @PostMapping("generate")
    @PreAuthorize("@preAuthorizeService.isManager(#authUserEppn)")
    public String generateForm(
            @RequestParam("multipartFile") MultipartFile multipartFile,
            @ModelAttribute("authUserEppn") String authUserEppn,
            @RequestParam String name,
            @RequestParam String title,
            @RequestParam Long workflowId,
            @RequestParam String prefillType,
            @RequestParam(name = "managerRole") String managerRole,
            @RequestParam(required = false) List<String> roleNames,
            @RequestParam(required = false) Boolean publicUsage,
            RedirectAttributes redirectAttributes) throws IOException {
        try {
            Form form = formService.generateForm(multipartFile, name, title, workflowId, prefillType, roleNames, publicUsage);
            form.setManagerRole(managerRole);
            return "redirect:/manager/forms/" + form.getId();
        } catch (EsupSignatureException e) {
            logger.error(e.getMessage());
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
            return "redirect:/manager/forms/";
        }
    }

    @GetMapping("update/{id}")
    @PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn)")
    public String updateForm(@PathVariable("id") long id, @ModelAttribute("authUserEppn") String authUserEppn, Model model) {
        User manager = userService.getByEppn(authUserEppn);
        Form form = formService.getById(id);
        model.addAttribute("form", form);
        model.addAttribute("fields", form.getFields());
        model.addAttribute("roles", manager.getManagersRoles());
        model.addAttribute("document", form.getDocument());
        List<Workflow> workflows = workflowService.getSystemWorkflows();
        workflows.add(form.getWorkflow());
        model.addAttribute("workflowTypes", workflows);
        List<PreFill> preFillTypes = preFillService.getPreFillValues();
        model.addAttribute("preFillTypes", preFillTypes);
        model.addAttribute("shareTypes", ShareType.values());
        model.addAttribute("targetTypes", DocumentIOType.values());
        model.addAttribute("model", form.getDocument());
        return "managers/forms/update";
    }

    @GetMapping("create")
    public String createForm(Model model) {
        model.addAttribute("form", new Form());
        return "managers/forms/create";
    }

    @PutMapping
    @PreAuthorize("@preAuthorizeService.formManager(#updateForm.id, #authUserEppn)")
    public String updateForm(@ModelAttribute Form updateForm,
                             @RequestParam(value = "types", required = false) String[] types,
                             @ModelAttribute("authUserEppn") String authUserEppn,
                             RedirectAttributes redirectAttributes) {
        updateForm.setPublicUsage(false);
        updateForm.setAction("");
        formService.updateForm(updateForm.getId(), updateForm, types, true);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Modifications enregistrées"));
        return "redirect:/manager/forms/update/" + updateForm.getId();
    }

    @PostMapping("/update-model/{id}")
    @PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn)")
    public String updateFormModel(@PathVariable("id") Long id,
                                  @ModelAttribute("authUserEppn") String authUserEppn,
                                  @RequestParam(value = "multipartModel", required=false) MultipartFile multipartModel, RedirectAttributes redirectAttributes) {
        try {
            if(multipartModel.getSize() > 0) {
                formService.updateFormModel(id, multipartModel);
            }
        } catch (EsupSignatureException e) {
            logger.error(e.getMessage());
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
            return "redirect:/manager/forms/";
        }
        redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Modifications enregistrées"));
        return "redirect:/manager/forms/update/" + id;
    }

    @DeleteMapping("{id}")
    @PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn)")
    public String deleteForm(@PathVariable("id") Long id, @ModelAttribute("authUserEppn") String authUserEppn, RedirectAttributes redirectAttributes) {
        formService.deleteForm(id);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Le formulaire à bien été supprimé"));
        return "redirect:/manager/forms";
    }

//    @GetMapping(value = "/{name}/datas/csv", produces="text/csv")
//    @PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn)")
//    public ResponseEntity<Void> getFormDatasCsv(@PathVariable String name, HttpServletResponse response) {
//        List<Form> forms = formService.getFormByName(name);
//        if (forms.size() > 0) {
//            try {
//                response.setContentType("text/csv; charset=utf-8");
//                response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(forms.get(0).getName(), StandardCharsets.UTF_8.toString()) + ".csv");
//                InputStream csvInputStream = dataExportService.getCsvDatasFromForms(forms);
//                IOUtils.copy(csvInputStream, response.getOutputStream());
//                return new ResponseEntity<>(HttpStatus.OK);
//            } catch (Exception e) {
//                logger.error("get file error", e);
//            }
//        } else {
//            logger.warn("form " + name + " not found");
//            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//        }
//        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
//    }

    @GetMapping("{id}/fields")
    @PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn)")
    public String fields(@PathVariable("id") Long id, @ModelAttribute("authUserEppn") String authUserEppn, Model model) {
        Form form = formService.getById(id);
        model.addAttribute("form", form);
        model.addAttribute("workflow", form.getWorkflow());
        PreFill preFill = preFillService.getPreFillServiceByName(form.getPreFillType());
        if(preFill != null) {
            model.addAttribute("preFillTypes", preFill.getTypes());
        } else {
            model.addAttribute("preFillTypes", new HashMap<>());
        }
        model.addAttribute("document", form.getDocument());
        return "managers/forms/fields";
    }

    @ResponseBody
    @PostMapping("/fields/{id}/update")
    @PreAuthorize("@preAuthorizeService.isManager(#authUserEppn)")
    public ResponseEntity<String> updateField(@PathVariable("id") Long id,
                                              @ModelAttribute("authUserEppn") String authUserEppn,
                                              @RequestParam(value = "description", required = false) String description,
                                              @RequestParam(value = "fieldType", required = false, defaultValue = "text") FieldType fieldType,
                                              @RequestParam(value = "required", required = false, defaultValue = "false") Boolean required,
                                              @RequestParam(value = "favorisable", required = false, defaultValue = "false") Boolean favorisable,
                                              @RequestParam(value = "readOnly", required = false, defaultValue = "false") Boolean readOnly,
                                              @RequestParam(value = "prefill", required = false, defaultValue = "false") Boolean prefill,
                                              @RequestParam(value = "search", required = false, defaultValue = "false") Boolean search,
                                              @RequestParam(value = "valueServiceName", required = false) String valueServiceName,
                                              @RequestParam(value = "valueType", required = false) String valueType,
                                              @RequestParam(value = "valueReturn", required = false) String valueReturn,
                                              @RequestParam(value = "stepZero", required = false, defaultValue = "false") Boolean stepZero,
                                              @RequestParam(value = "workflowStepsIds", required = false) List<Long> workflowStepsIds) {

        String extValueServiceName = "";
        String extValueType = "";
        String extValueReturn = "";
        String searchServiceName = "";
        String searchType = "";
        String searchReturn = "";
        if(prefill) {
            extValueServiceName = valueServiceName;
            extValueType = valueType;
            extValueReturn = valueReturn;
        }
        if(search) {
            searchServiceName = valueServiceName;
            searchType = valueType;
            searchReturn = valueReturn;
        }
        fieldService.updateField(id, description, fieldType, favorisable, required, readOnly, extValueServiceName, extValueType, extValueReturn, searchServiceName, searchType, searchReturn, stepZero, workflowStepsIds);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(value = "/get-file/{id}")
    @PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn)")
    public void getFile(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse httpServletResponse, RedirectAttributes redirectAttributes) throws IOException {
        try {
            if(!formService.getModel(id, httpServletResponse)) {
                redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Modèle non trouvée ..."));
                httpServletResponse.sendRedirect("/manager/forms/update/" + id);
            }
        } catch (Exception e) {
            logger.error("get file error", e);
        }
    }

    @GetMapping("{id}/signs")
    @PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn)")
    public String signs(@PathVariable("id") Long id, @ModelAttribute("authUserEppn") String authUserEppn, Model model) throws EsupSignatureIOException {
        Form form = formService.getById(id);
        Map<Long, Integer> srpMap = new HashMap<>();
        for(WorkflowStep workflowStep : form.getWorkflow().getWorkflowSteps()) {
            for(SignRequestParams signRequestParams : workflowStep.getSignRequestParams()) {
                srpMap.put(signRequestParams.getId(), form.getWorkflow().getWorkflowSteps().indexOf(workflowStep) + 1);
            }
        }
        if(form.getDocument() != null) {
            form.setTotalPageCount(formService.getTotalPagesCount(id));
        }
        model.addAttribute("form", form);
        model.addAttribute("srpMap", srpMap);
        model.addAttribute("workflow", form.getWorkflow());
        model.addAttribute("document", form.getDocument());
        return "managers/forms/signs";
    }

    @PostMapping("/update-signs-order/{id}")
    @PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn)")
    public ResponseEntity<String> updateSignsOrder(@PathVariable("id") Long id,
                                   @ModelAttribute("authUserEppn") String authUserEppn,
                                   @RequestParam Map<String, String> values) throws JsonProcessingException {
        String[] stringStringMap = objectMapper.readValue(values.get("srpMap"), String[].class);
        Map<Long, Integer> signRequestParamsSteps = new HashMap<>();
        for (int i = 0; i < stringStringMap.length; i = i + 2) {
            signRequestParamsSteps.put(Long.valueOf(stringStringMap[i]), Integer.valueOf(stringStringMap[i + 1]));
        }
        formService.setSignRequestParamsSteps(id, signRequestParamsSteps);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}