package org.esupportail.esupsignature.web.controller.user;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.UserPropertie;
import org.esupportail.esupsignature.entity.UserShare;
import org.esupportail.esupsignature.entity.enums.EmailAlertFrequency;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.ldap.AliasLdap;
import org.esupportail.esupsignature.service.ldap.LdapAliasService;
import org.esupportail.esupsignature.service.ldap.PersonLdap;
import org.esupportail.esupsignature.service.list.UserListService;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RequestMapping("user/users")
@Controller
public class UserController {

	private static final Logger logger = LoggerFactory.getLogger(UserController.class);

	@ModelAttribute("paramMenu")
	public String getActiveMenu() {
		return "active";
	}

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

	@Resource
	private MessageService messageService;

	@Resource
	private LdapAliasService ldapAliasService;

	@Resource
	UserListService userListService;

    @GetMapping
    public String updateForm(@ModelAttribute("authUserEppn") String authUserEppn, Model model, @RequestParam(value = "referer", required=false) String referer, HttpServletRequest request) {
		model.addAttribute("signTypes", Arrays.asList(SignType.values()));
		model.addAttribute("emailAlertFrequencies", Arrays.asList(EmailAlertFrequency.values()));
		model.addAttribute("daysOfWeek", Arrays.asList(DayOfWeek.values()));
		model.addAttribute("uiParams", userService.getUiParams(authUserEppn));
		if(referer != null && !"".equals(referer) && !"null".equals(referer)) {
			model.addAttribute("referer", request.getHeader("referer"));
		}
		model.addAttribute("activeMenu", "settings");
		return "user/users/update";
    }
    
    @PostMapping
    public String update(@ModelAttribute("authUserEppn") String authUserEppn, @RequestParam(value = "signImageBase64", required=false) String signImageBase64,
    		@RequestParam(value = "emailAlertFrequency", required=false) EmailAlertFrequency emailAlertFrequency,
    		@RequestParam(value = "emailAlertHour", required=false) Integer emailAlertHour,
    		@RequestParam(value = "emailAlertDay", required=false) DayOfWeek emailAlertDay,
    		@RequestParam(value = "multipartKeystore", required=false) MultipartFile multipartKeystore, RedirectAttributes redirectAttributes) throws Exception {
		userService.updateUser(authUserEppn, signImageBase64, emailAlertFrequency, emailAlertHour, emailAlertDay, multipartKeystore);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Vos paramètres ont été enregistrés"));
		return "redirect:/user/users";
    }

	@GetMapping("/delete-sign/{id}")
	public String deleteSign(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable long id, RedirectAttributes redirectAttributes) {
		userService.deleteSign(authUserEppn, id);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Signature supprimée"));
		return "redirect:/user/users";
	}

	@PostMapping(value = "/view-cert")
    public String viewCert(@ModelAttribute("authUserEppn") String authUserEppn, @RequestParam(value =  "password", required = false) String password, RedirectAttributes redirectAttributes) {
		try {
        	redirectAttributes.addFlashAttribute("message", new JsonMessage("custom", userKeystoreService.checkKeystore(authUserEppn, password)));
        } catch (Exception e) {
        	logger.error("open keystore fail", e);
        	redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Mauvais mot de passe"));
		}
        return "redirect:/user/users";
    }

	@GetMapping(value = "/remove-keystore")
	public String removeKeystore(@ModelAttribute("authUserEppn") String authUserEppn, Model model, RedirectAttributes redirectAttributes) {
		User authUser = (User) model.getAttribute("authUser");
		authUser.setKeystore(null);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Le magasin de clés à bien été supprimé"));
		return "redirect:/user/users";
	}

	@GetMapping(value="/search-user")
	@ResponseBody
	public List<PersonLdap> searchLdap(@RequestParam(value="searchString") String searchString) {
		logger.debug("ldap search for : " + searchString);
		return userService.getPersonLdaps(searchString).stream().sorted(Comparator.comparing(PersonLdap::getDisplayName)).collect(Collectors.toList());
   }

   	@GetMapping(value = "/search-list")
	@ResponseBody
	public List<AliasLdap> searchList(@RequestParam(value="searchString") String searchString) {
    	logger.debug("ldap search for : " + searchString);
		return ldapAliasService.searchAlias(searchString);
	}

	@GetMapping(value = "/search-user-list")
	@ResponseBody
	public List<String> searchUserList(@RequestParam(value="searchString") String searchString) {
    	return userListService.getUsersEmailFromList(searchString);
	}

	@GetMapping("/properties")
	public String properties(@ModelAttribute("authUserEppn") String authUserEppn, Model model) {
		List<UserPropertie> userProperties = userPropertieService.getUserPropertiesByUserEppn(authUserEppn);
		model.addAttribute("userProperties", userProperties);
		model.addAttribute("forms", formService.getFormsByUser(authUserEppn, authUserEppn));
		model.addAttribute("users", userService.getAllUsers());
		model.addAttribute("activeMenu", "properties");
		return "user/users/properties";
	}

	@GetMapping("/shares")
	public String params(@ModelAttribute("authUserEppn") String authUserEppn, Model model) {
		List<UserShare> userShares = userShareService.getUserSharesByUser(authUserEppn);
		model.addAttribute("userShares", userShares);
		model.addAttribute("shareTypes", ShareType.values());
		model.addAttribute("forms", formService. getAuthorizedToShareForms());
		model.addAttribute("workflows", workflowService.getAuthorizedToShareWorkflows());
		model.addAttribute("users", userService.getAllUsers());
		model.addAttribute("activeMenu", "shares");
		return "user/users/shares/list";
	}

	@GetMapping("/shares/update/{id}")
	public String params(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
		model.addAttribute("activeMenu", "shares");
		UserShare userShare = userShareService.getById(id);
		if(userShare.getUser().getEppn().equals(authUserEppn)) {
			model.addAttribute("shareTypes", ShareType.values());
			model.addAttribute("userShare", userShare);
			return "user/users/shares/update";
		} else {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Accès refusé"));
			return "redirect:/user/users/shares";
		}
	}

	@PostMapping("/add-share")
	public String addShare(@ModelAttribute("authUserEppn") String authUserEppn,
						   @RequestParam(value = "signWithOwnSign", required = false) Boolean signWithOwnSign,
						   @RequestParam(value = "form", required = false) Long[] form,
						   @RequestParam(value = "workflow", required = false) Long[] workflow,
						   @RequestParam("types") String[] types,
						   @RequestParam("userIds") String[] userEmails,
						   @RequestParam("beginDate") String beginDate,
						   @RequestParam("endDate") String endDate, Model model,
						   RedirectAttributes redirectAttributes) {
		User authUser = (User) model.getAttribute("authUser");
    	if(form == null) form = new Long[] {};
		if(workflow == null) workflow = new Long[] {};
		if(signWithOwnSign == null) signWithOwnSign = false;
		try {
			userShareService.addUserShare(authUser, signWithOwnSign, form, workflow, types, userEmails, beginDate, endDate);
		} catch (EsupSignatureUserException e) {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
		}
		return "redirect:/user/users/shares";
	}

	@PostMapping("/update-share/{id}")
	public String updateShare(@ModelAttribute("authUserEppn") String authUserEppn,
							  @PathVariable("id") Long id,
							  @RequestParam(value = "signWithOwnSign", required = false) Boolean signWithOwnSign,
							  @RequestParam("types") String[] types,
							  @RequestParam("userIds") String[] userEmails,
							  @RequestParam("beginDate") String beginDate,
							  @RequestParam("endDate") String endDate, Model model) {
		userShareService.updateUserShare(authUserEppn, types, userEmails, beginDate, endDate, id, signWithOwnSign);
		return "redirect:/user/users/shares";
	}

	@DeleteMapping("/del-share/{id}")
	public String delShare(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable long id, Model model, RedirectAttributes redirectAttributes) {
		userShareService.delete(id, authUserEppn);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Élément supprimé"));
		return "redirect:/user/users/shares";
	}

	@GetMapping("/change-share")
	public String change(@ModelAttribute("authUserEppn") String authUserEppn, @RequestParam(required = false) String eppn, @RequestParam(required = false) Long userShareId, RedirectAttributes redirectAttributes, HttpSession httpSession, HttpServletRequest httpServletRequest) {
		if(eppn == null || eppn.isEmpty()) {
			httpSession.setAttribute("suEppn", null);
			redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Délégation désactivée"));
		} else {
			if(userShareService.checkShare(eppn, authUserEppn)) {
				httpSession.setAttribute("suEppn", eppn);
				httpSession.setAttribute("userShareId", userShareId);
				redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Délégation activée : " + eppn));
			} else {
				redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Aucune délégation active en ce moment"));
			}
		}
		String referer = httpServletRequest.getHeader("Referer");
		return "redirect:"+ referer;
	}

	@GetMapping("/mark-intro-as-read/{name}")
	public String markIntroAsRead(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable String name, HttpServletRequest httpServletRequest) {
		logger.info("user " + authUserEppn + " mark into " + name + " as read");
		userService.disableIntro(authUserEppn, name);
		String referer = httpServletRequest.getHeader("Referer");
		return "redirect:"+ referer;
	}

	@GetMapping("/mark-as-read/{id}")
	public String markAsRead(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable long id, HttpServletRequest httpServletRequest) {
    	logger.info("user " + authUserEppn + " mark " + id + " as read");
		messageService.disableMessageForUser(authUserEppn, id);
		String referer = httpServletRequest.getHeader("Referer");
		return "redirect:"+ referer;
	}

	@GetMapping("/mark-help-as-read/{id}")
	public String markHelpAsRead(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable long id, HttpServletRequest httpServletRequest) {
		logger.info("user " + authUserEppn + " mark help" + id + " as read");
		userService.setFormMessage(authUserEppn, id);
		String referer = httpServletRequest.getHeader("Referer");
		return "redirect:"+ referer;
	}

	@GetMapping(value = "/get-keystore")
	public ResponseEntity<Void> getKeystore(@ModelAttribute("authUserEppn") String authUserEppn, HttpServletResponse response) throws IOException {
		Map<String, Object> keystore = userService.getKeystoreByUser(authUserEppn);
		return getDocumentResponseEntity(response, (byte[]) keystore.get("bytes"), (String) keystore.get("fileName"), (String) keystore.get("contentType"));
	}

	@GetMapping(value = "/get-sign-image/{id}")
	public ResponseEntity<Void> getSignature(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, HttpServletResponse response) throws IOException {
		Map<String, Object> signature = userService.getSignatureByUserAndId(userEppn, id);
		return getDocumentResponseEntity(response, (byte[]) signature.get("bytes"), (String) signature.get("fileName"), (String) signature.get("contentType"));
	}

	private ResponseEntity<Void> getDocumentResponseEntity(HttpServletResponse response, byte[] bytes, String fileName, String contentType) throws IOException {
		response.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()));
		response.setContentType(contentType);
		IOUtils.copy(new ByteArrayInputStream(bytes), response.getOutputStream());
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@ResponseBody
	@PostMapping(value ="/check-user-certificate")
	private List<User> checkUserCertificate(@RequestBody List<String> userEmails) {
    	return userService.getUserWithoutCertificate(userEmails);
	}

}
