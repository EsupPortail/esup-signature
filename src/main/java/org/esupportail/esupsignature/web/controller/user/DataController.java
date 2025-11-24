package org.esupportail.esupsignature.web.controller.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.dto.json.WorkflowStepDto;
import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.DataService;
import org.esupportail.esupsignature.service.FormService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user/datas")
public class DataController {

	private static final Logger logger = LoggerFactory.getLogger(DataController.class);
    @ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "datas";
	}

	private final DataService dataService;
	private final SignBookService signBookService;
	private final FormService formService;
	private final PdfService pdfService;
	private final ObjectMapper objectMapper;

    public DataController(DataService dataService, SignBookService signBookService, FormService formService, PdfService pdfService, ObjectMapper objectMapper) {
        this.dataService = dataService;
        this.signBookService = signBookService;
        this.formService = formService;
        this.pdfService = pdfService;
        this.objectMapper = objectMapper;
    }

	@PostMapping("/send-form/{id}")
	@ResponseBody
	public ResponseEntity<String> sendForm(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
						   @RequestBody List<WorkflowStepDto> steps,
						   @PathVariable("id") Long id) throws EsupSignatureRuntimeException {
		logger.info("create form " + id);
        try {
            if(formService.isFormAuthorized(userEppn, authUserEppn, id)) {
                Data data = dataService.addData(id, userEppn);
                List<String> targetEmails = steps.stream().flatMap(step -> step.getTargetEmails().stream()).distinct().toList();
                SignBook signBook = signBookService.sendForSign(data.getId(), steps, targetEmails, null, userEppn, authUserEppn, false, null, null, null, true, null);
                return ResponseEntity.ok().body(signBook.getId().toString());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
		logger.warn("form id " + id + " not autorized");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Formulaire non autorisé");
	}

	@PostMapping("/form/{id}")
	@ResponseBody
	public String addData(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
						  @PathVariable("id") Long id,
						  @RequestParam String dataId,
						  @RequestParam MultiValueMap<String, String> formData,
						  RedirectAttributes redirectAttributes) throws JsonProcessingException {
		TypeReference<HashMap<String, String>> type = new TypeReference<>(){};
		Map<String, String> datas = objectMapper.readValue(formData.getFirst("formData"), type);
		Long dataLongId = null;
		try {
			dataLongId = Long.valueOf(dataId);
		} catch (NumberFormatException e) {
			logger.debug("dataId is null");
		}
		Data data = dataService.addData(id, dataLongId , datas, userEppn, authUserEppn);
		redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Données enregistrées"));
		return data.getId().toString();
	}

	@GetMapping("/forms/{id}/get-image")
	public ResponseEntity<Void> getImagePdfAsByteArray(@PathVariable("id") Long id, HttpServletResponse httpServletResponse) throws Exception {
		Form form = formService.getById(id);
		InputStream in = pdfService.pageAsInputStream(form.getDocument().getInputStream(), 0);
		httpServletResponse.setContentType(MediaType.IMAGE_PNG_VALUE);
		IOUtils.copy(in, httpServletResponse.getOutputStream());
		in.close();
		return ResponseEntity.ok().build();
	}

}