package org.esupportail.esupsignature.web.controller.user;

import org.apache.commons.io.IOUtils;
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
public class FormUserController {

    private static final Logger logger = LoggerFactory.getLogger(FormUserController.class);

    @Resource
    private FormRepository formRepository;

    @Resource
    private UserService userService;

    @Resource
    private DataRepository dataRepository;

    @Resource
    private PdfService pdfService;

    @ModelAttribute("user")
    public User getUser() {
        return userService.getCurrentUser();
    }

    @ModelAttribute("suUsers")
    public List<User> getSuUsers() {
        return userService.getSuUsers();
    }

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "apps";
    }

//    @GetMapping("forms/{id}")
//    public String getFormById(@PathVariable("id") Long id, Model model) {
//        Form form = formRepository.findById(id).get();
//        model.addAttribute("form", form);
//        model.addAttribute("activeForm", form.getName());
//        model.addAttribute("fields", form.getFields());
//        model.addAttribute("document", form.getDocument());
//        return "user/forms/show";
//    }
//
//    @GetMapping("forms/{id}/list")
//    public String getFormByIdList(@PathVariable("id") Long id, @SortDefault(value = "createDate", direction = Direction.DESC) @PageableDefault(size = 5) Pageable pageable, Model model) throws RuntimeException {
//        UserUi user = userService.getUserFromAuthentication();
//        Form form = formRepository.findById(id).get();
//        Form activeVersionForm = formRepository.findFormByNameAndActiveVersion(form.getName(), true).get(0);
//        model.addAttribute("form", form);
//        model.addAttribute("activeForm", form.getName());
//        model.addAttribute("activeVersionForm", activeVersionForm);
//        Page<Data> datas = dataRepository.findByFormNameAndOwner(form.getName(), user.getEppn(), pageable);
//        model.addAttribute("datas", datas);
//        return "user/forms/list";
//    }
//
//    @GetMapping("forms")
//    public String getAllForms(Model model) {
//        List<Form> forms = formRepository.findFormByActiveVersion(true);
//        model.addAttribute("forms", forms);
//        return "user/forms/list";
//    }
//
//    @PostMapping("forms/{id}")
//    public String addForm(@PathVariable("id") Long id, @RequestParam MultiValueMap<String, String> formData) {
//        UserUi user = userService.getUserFromAuthentication();
//        Form form = formRepository.findById(id).get();
//        Data data = new Data();
//        data.setName(form.getName());
//        data.setDatas(formData.toSingleValueMap());
//        data.setForm(form);
//        data.setStatus(SignRequestStatus.draft);
//        data.setCreateBy(userService.getUserFromAuthentication().getEppn());
//        data.setCreateDate(new Date());
//        data.setOwner(user.getEppn());
//        return "redirect:/user/data/" + data.getId();
//    }

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