package org.esupportail.esupsignature.web.controller.user;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.EmailAlertFrequency;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.ldap.PersonLdap;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RequestMapping("user/users")
@Controller
@Transactional
public class UserController {

	private static final Logger logger = LoggerFactory.getLogger(UserController.class);

	@ModelAttribute("paramMenu")
	public String getActiveMenu() {
		return "active";
	}

	@Resource
	private DocumentService documentService;

	@Resource
	private BigFileService bigFileService;

	@Resource
	private FileService fileService;

	@Resource
	private FormService formService;

	@Resource
	private WorkflowService workflowService;

	@Autowired(required=false)
	private UserKeystoreService userKeystoreService;
	
	@Resource
	private UserService userService;

	@Resource
	private UserShareService userShareService;

	@Resource
	private UserPropertieService userPropertieService;

    @GetMapping
    public String createForm(@ModelAttribute("authUser") User authUser, Model model, @RequestParam(value = "referer", required=false) String referer, HttpServletRequest request) {
		model.addAttribute("signTypes", Arrays.asList(SignType.values()));
		model.addAttribute("emailAlertFrequencies", Arrays.asList(EmailAlertFrequency.values()));
		model.addAttribute("daysOfWeek", Arrays.asList(DayOfWeek.values()));
		if(referer != null && !"".equals(referer) && !"null".equals(referer)) {
			model.addAttribute("referer", request.getHeader("referer"));
		}
		model.addAttribute("activeMenu", "settings");
		return "user/users/update";
    }
    
    @PostMapping
    public String create(@ModelAttribute("authUser") User authUser, @RequestParam(value = "signImageBase64", required=false) String signImageBase64,
    		@RequestParam(value = "emailAlertFrequency", required=false) EmailAlertFrequency emailAlertFrequency,
    		@RequestParam(value = "emailAlertHour", required=false) Integer emailAlertHour,
    		@RequestParam(value = "emailAlertDay", required=false) DayOfWeek emailAlertDay,
    		@RequestParam(value = "multipartKeystore", required=false) MultipartFile multipartKeystore, RedirectAttributes redirectAttributes) throws Exception {
        if(multipartKeystore != null && !multipartKeystore.isEmpty()) {

            if(authUser.getKeystore() != null) {
            	bigFileService.delete(authUser.getKeystore().getBigFile().getId());
            	documentService.delete(authUser.getKeystore());
            }
            authUser.setKeystore(documentService.createDocument(multipartKeystore.getInputStream(), authUser.getEppn() + "_" + multipartKeystore.getOriginalFilename().split("\\.")[0] + ".p12", multipartKeystore.getContentType()));
        }
        if(signImageBase64 != null && !signImageBase64.isEmpty()) {
        	authUser.getSignImages().add(documentService.createDocument(fileService.base64Transparence(signImageBase64), authUser.getEppn() + "_sign.png", "image/png"));
        }
    	authUser.setEmailAlertFrequency(emailAlertFrequency);
    	authUser.setEmailAlertHour(emailAlertHour);
    	authUser.setEmailAlertDay(emailAlertDay);
    	redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Vos paramètres ont été enregistrés"));
		return "redirect:/user/users";
    }

	@GetMapping("/delete-sign/{id}")
	public String deleteSign(@ModelAttribute("authUser") User authUser, @PathVariable long id, RedirectAttributes redirectAttributes) {
    	Document signDocument = documentService.getById(id);
		authUser.getSignImages().remove(signDocument);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Signature supprimée"));
		return "redirect:/user/users";
	}


	@GetMapping(value = "/view-cert")
    public String viewCert(@ModelAttribute("authUser") User authUser, @RequestParam(value =  "password", required = false) String password, RedirectAttributes redirectAttributes) {
		try {
			logger.info(authUser.getKeystore().getInputStream().read() + "");
        	redirectAttributes.addFlashAttribute("message", new JsonMessage("custom", userKeystoreService.checkKeystore(authUser.getKeystore().getInputStream(), password)));
        } catch (Exception e) {
        	logger.error("open keystore fail", e);
        	redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Mauvais mot de passe"));
		}
        return "redirect:/user/users/?form";
    }

	@GetMapping(value = "/remove-keystore")
	public String removeKeystore(@ModelAttribute("authUser") User authUser, RedirectAttributes redirectAttributes) {
		authUser.setKeystore(null);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Le magasin de clés à bien été supprimé"));
		return "redirect:/user/users/?form";
	}

	@GetMapping(value="/search-user")
	@ResponseBody
	public List<PersonLdap> searchLdap(@RequestParam(value="searchString") String searchString) {
		logger.debug("ldap search for : " + searchString);
		return userService.getPersonLdaps(searchString).stream().sorted(Comparator.comparing(PersonLdap::getDisplayName)).collect(Collectors.toList());
   }

	@GetMapping("/properties")
	public String properties(Model model) {
		User user = userService.getUserFromAuthentication();
		List<UserPropertie> userProperties = userPropertieService.getUserPropertiesByUser(user);
		model.addAttribute("userProperties", userProperties);
		model.addAttribute("forms", formService.getFormsByUser(user, user));
		model.addAttribute("users", userService.getAllUsers());
		model.addAttribute("activeMenu", "properties");
		return "user/users/properties";
	}

	@GetMapping("/shares")
	public String params(@ModelAttribute("authUser") User authUser, Model model) {
		List<UserShare> userShares = userShareService.getUserShareByUser(authUser);
		model.addAttribute("userShares", userShares);
		model.addAttribute("shareTypes", ShareType.values());
		model.addAttribute("forms", formService. getAuthorizedToShareForms());
		model.addAttribute("workflows", workflowService.getAuthorizedToShareWorkflows());
		model.addAttribute("users", userService.getAllUsers());
		model.addAttribute("activeMenu", "shares");
		return "user/users/shares/list";
	}

	@GetMapping("/shares/update/{id}")
	public String params(@ModelAttribute("authUser") User authUser, @PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
		model.addAttribute("activeMenu", "shares");
		UserShare userShare = userShareService.getUserShareById(id);
		if(userShare.getUser().equals(authUser)) {
			model.addAttribute("shareTypes", ShareType.values());
			model.addAttribute("userShare", userShare);
			return "user/users/shares/update";
		} else {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Accès refusé"));
			return "redirect:/user/users/shares";
		}
	}

	@PostMapping("/add-share")
	public String addShare(@ModelAttribute("authUser") User authUser,
						   @RequestParam(value = "form", required = false) Long[] form,
						   @RequestParam(value = "workflow", required = false) Long[] workflow,
						   @RequestParam("types") String[] types,
						   @RequestParam("userIds") String[] userEmails,
						   @RequestParam("beginDate") String beginDate,
						   @RequestParam("endDate") String endDate,
						   RedirectAttributes redirectAttributes) {
    	if(form == null) form = new Long[] {};
		if(workflow == null) workflow = new Long[] {};
		try {
			userShareService.addUserShare(authUser, form, workflow, types, userEmails, beginDate, endDate);
		} catch (EsupSignatureUserException e) {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
		}
		return "redirect:/user/users/shares";
	}

	@PostMapping("/update-share/{id}")
	public String updateShare(@ModelAttribute("authUser") User authUser,
							  @PathVariable("id") Long id,
							  @RequestParam("types") String[] types,
							  @RequestParam("userIds") String[] userEmails,
							  @RequestParam("beginDate") String beginDate,
							  @RequestParam("endDate") String endDate) {
		UserShare userShare = userShareService.getUserShareById(id);
		userShareService.updateUserShare(authUser, types, userEmails, beginDate, endDate, userShare);
		return "redirect:/user/users/shares";
	}

	@DeleteMapping("/del-share/{id}")
	public String delShare(@ModelAttribute("authUser") User authUser, @PathVariable long id, RedirectAttributes redirectAttributes) {
		UserShare userShare = userShareService.getUserShareById(id);
		if (userShare.getUser().equals(authUser)) {
			userShareService.delete(userShare);
		}
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Élément supprimé"));
		return "redirect:/user/users/shares";
	}

	@GetMapping("/change-share")
	public String change(@ModelAttribute("authUser") User authUser, @RequestParam(required = false) String eppn, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) {
		if(userShareService.switchToShareUser(eppn)) {
			if(eppn == null || eppn.isEmpty()) {
				redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Délégation désactivée"));
			} else {
				redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Délégation activée : " + eppn));
			}
		} else {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Aucune délégation active en ce moment"));
		}
		String referer = httpServletRequest.getHeader("Referer");
		return "redirect:"+ referer;
	}

	@GetMapping("/mark-intro-as-read/{name}")
	public String markIntroAsRead(@ModelAttribute(value = "authUser" , binding = false) User authUser, @PathVariable String name, HttpServletRequest httpServletRequest) {
		logger.info(authUser.getEppn() + " mark " + name + " as read");
		userService.disableIntro(authUser, name);
		String referer = httpServletRequest.getHeader("Referer");
		return "redirect:"+ referer;
	}

	@GetMapping("/mark-as-read/{id}")
	public String markAsRead(@ModelAttribute(value = "authUser" , binding = false) User authUser, @PathVariable long id, HttpServletRequest httpServletRequest) {
    	logger.info(authUser.getEppn() + " mark " + id + " as read");
		userService.disableMessageForUser(authUser, id);
		String referer = httpServletRequest.getHeader("Referer");
		return "redirect:"+ referer;
	}

	@GetMapping("/mark-help-as-read/{id}")
	public String markHelpAsRead(@ModelAttribute(value = "authUser" , binding = false) User authUser, @PathVariable long id, HttpServletRequest httpServletRequest) {
		logger.info(authUser.getEppn() + " mark " + id + " as read");
		Form form = formService.getFormById(id);
		authUser.setFormMessages(authUser.getFormMessages() + " " + form.getId());
		String referer = httpServletRequest.getHeader("Referer");
		return "redirect:"+ referer;
	}

	@GetMapping(value = "/get-keystore")
	public ResponseEntity<Void> getKeystore(@ModelAttribute("user") User user, @ModelAttribute("authUser") User authUser, @PathVariable("id") Long id, HttpServletResponse response) throws IOException {
		return getDocumentResponseEntity(response, user.getKeystore());
	}

	private ResponseEntity<Void> getDocumentResponseEntity(HttpServletResponse response, Document document) throws IOException {
		response.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(document.getFileName(), StandardCharsets.UTF_8.toString()));
		response.setContentType(document.getContentType());
		IOUtils.copy(document.getInputStream(), response.getOutputStream());
		return new ResponseEntity<>(HttpStatus.OK);
	}

}
