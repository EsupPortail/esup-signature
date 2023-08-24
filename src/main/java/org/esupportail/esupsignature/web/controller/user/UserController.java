package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.entity.FieldPropertie;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.UserPropertie;
import org.esupportail.esupsignature.entity.enums.EmailAlertFrequency;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.interfaces.listsearch.UserListService;
import org.esupportail.esupsignature.service.ldap.entry.PersonLightLdap;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RequestMapping("/user/users")
@Controller
public class UserController {

	private static final Logger logger = LoggerFactory.getLogger(UserController.class);

	@ModelAttribute("paramMenu")
	public String getActiveMenu() {
		return "bg-secondary";
	}

	@Resource
	private FormService formService;

	private final UserKeystoreService userKeystoreService;

	public UserController(@Autowired(required=false) UserKeystoreService userKeystoreService) {
		this.userKeystoreService = userKeystoreService;
	}

	@Resource
	private UserService userService;

	@Resource
	private SignBookService signBookService;

	@Resource
	private UserPropertieService userPropertieService;

	@Resource
	private FieldPropertieService fieldPropertieService;

	@Resource
	private MessageService messageService;

	@Resource
	UserListService userListService;

    @GetMapping
	public String updateForm(@ModelAttribute("authUserEppn") String authUserEppn, Model model, @RequestParam(value = "referer", required=false) String referer, HttpServletRequest request) {
		model.addAttribute("emailAlertFrequencies", Arrays.asList(EmailAlertFrequency.values()));
		model.addAttribute("daysOfWeek", Arrays.asList(DayOfWeek.values()));
		if(referer != null && !"".equals(referer) && !"null".equals(referer)) {
			model.addAttribute("referer", request.getHeader(HttpHeaders.REFERER));
		}
		model.addAttribute("activeMenu", "settings");
		return "user/users/update";
    }

	@PostMapping
	public String update(@ModelAttribute("authUserEppn") String authUserEppn, @RequestParam(value = "signImageBase64", required=false) String signImageBase64,
						 @RequestParam(value = "returnToHomeAfterSign", required=false) Boolean returnToHomeAfterSign,
						 @RequestParam(value = "emailAlertFrequency", required=false) EmailAlertFrequency emailAlertFrequency,
						 @RequestParam(value = "emailAlertHour", required=false) Integer emailAlertHour,
						 @RequestParam(value = "emailAlertDay", required=false) DayOfWeek emailAlertDay,
						 @RequestParam(value = "multipartKeystore", required=false) MultipartFile multipartKeystore,
						 @RequestParam(value = "signRequestParamsJsonString", required=false) String signRequestParamsJsonString,
						 RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) throws Exception {
		if(returnToHomeAfterSign == null) returnToHomeAfterSign = false;
		userService.updateUser(authUserEppn, signImageBase64, emailAlertFrequency, emailAlertHour, emailAlertDay, multipartKeystore, signRequestParamsJsonString, returnToHomeAfterSign);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Vos paramètres ont été enregistrés"));
		String referer = httpServletRequest.getHeader(HttpHeaders.REFERER);
		return "redirect:" + referer;
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
        	logger.warn("open keystore fail : " + e.getMessage());
        	redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Mauvais mot de passe"));
		}
        return "redirect:/user/users";
    }

	@GetMapping(value = "/remove-keystore")
	public String removeKeystore(@ModelAttribute("authUserEppn") String authUserEppn, RedirectAttributes redirectAttributes) {
		User authUser = userService.getByEppn(authUserEppn);
		authUser.setKeystore(null);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Le magasin de clés à bien été supprimé"));
		return "redirect:/user/users";
	}

	@GetMapping(value="/search-user")
	@ResponseBody
	public List<PersonLightLdap> searchLdap(@RequestParam(value="searchString") String searchString, @ModelAttribute("authUserEppn") String authUserEppn) {
		logger.debug("ldap search for : " + searchString);
		return userService.getPersonLdapsLight(searchString, authUserEppn).stream().sorted(Comparator.comparing(PersonLightLdap::getDisplayName, Comparator.nullsLast(String::compareTo))).collect(Collectors.toList());
   }

	@GetMapping(value = "/search-user-list")
	@ResponseBody
	public List<String> searchUserList(@RequestParam(value="searchString") String searchString) {
		try {
			return userListService.getUsersEmailFromList(searchString);
		} catch (DataAccessException | EsupSignatureRuntimeException e) {
			logger.debug(e.getMessage());
		}
		return null;
	}

	@GetMapping("/properties")
	public String properties(@ModelAttribute("authUserEppn") String authUserEppn, Model model) {
		List<UserPropertie> userProperties = userPropertieService.getUserProperties(authUserEppn);
		if (userProperties != null && userProperties.size() > 0) {
			Map<User, Date> sortedMap = new LinkedHashMap<>();
			for (UserPropertie userPropertie : userProperties) {
				List<Map.Entry<User, Date>> entrySet = new ArrayList<>(userPropertie.getFavorites().entrySet());
				entrySet.sort(Map.Entry.<User, Date>comparingByValue().reversed());
				entrySet.forEach(e -> sortedMap.put(e.getKey(), e.getValue()));
			}
			model.addAttribute("userProperties", sortedMap);
		}
		List<FieldPropertie> fieldProperties = fieldPropertieService.getFieldProperties(authUserEppn);
		model.addAttribute("fieldProperties", fieldProperties);
		model.addAttribute("forms", formService.getFormsByUser(authUserEppn, authUserEppn));
		model.addAttribute("activeMenu", "properties");
		return "user/users/properties";
	}

	@DeleteMapping("/delete-user-propertie/{id}")
	public String deleteUserPropertie(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		userPropertieService.delete(authUserEppn, id);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Le favori à bien été supprimé"));
		return "redirect:/user/users/properties";
	}

	@DeleteMapping("/delete-field-propertie/{id}")
	public String deleteFieldPropertie(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @RequestParam("key") String key, RedirectAttributes redirectAttributes) {
		FieldPropertie fieldPropertie = fieldPropertieService.getById(id);
		if(fieldPropertie.getUser().getEppn().equals(authUserEppn)) {
			fieldPropertieService.delete(id, key);
			redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Le favori à bien été supprimé"));
		}
		return "redirect:/user/users/properties";
	}

	@GetMapping("/mark-intro-as-read/{name}")
	public String markIntroAsRead(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable String name, HttpServletRequest httpServletRequest) {
		logger.info("user " + authUserEppn + " mark intro " + name + " as read");
		userService.disableIntro(authUserEppn, name);
		String referer = httpServletRequest.getHeader(HttpHeaders.REFERER);
		return "redirect:" + referer;
	}

	@GetMapping("/mark-as-read/{id}")
	public String markAsRead(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable long id, HttpServletRequest httpServletRequest) {
    	logger.info("user " + authUserEppn + " mark " + id + " as read");
		messageService.disableMessageForUser(authUserEppn, id);
		String referer = httpServletRequest.getHeader(HttpHeaders.REFERER);
		return "redirect:" + referer;
	}

	@GetMapping("/mark-help-as-read/{id}")
	public String markHelpAsRead(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable long id, HttpServletRequest httpServletRequest) {
		logger.info("user " + authUserEppn + " mark help" + id + " as read");
		userService.setFormMessage(authUserEppn, id);
		String referer = httpServletRequest.getHeader(HttpHeaders.REFERER);
		return "redirect:"+ referer;
	}

	@ResponseBody
	@PostMapping(value ="/check-users-certificate")
	public List<User> checkUserCertificate(@RequestBody List<String> userEmails) {
    	return userService.getUserWithoutCertificate(userEmails);
	}

	@GetMapping("/set-default-sign-image/{signImageNumber}")
	public String setDefaultSignImage(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("signImageNumber") Integer signImageNumber) {
    	userService.setDefaultSignImage(authUserEppn, signImageNumber);
		return "redirect:/user/users";
	}

	@GetMapping("/replace")
	public String showReplace(@ModelAttribute("authUserEppn") String authUserEppn, Model model) {
		List<SignRequest> signRequests = signBookService.getSignBookForUsers(authUserEppn).stream().filter(signBook -> signBook.getStatus().equals(SignRequestStatus.pending)).flatMap(signBook -> signBook.getSignRequests().stream().distinct()).collect(Collectors.toList());
		model.addAttribute("signRequests", signRequests);
		model.addAttribute("activeMenu", "replace");
		return "user/users/replace";
	}

	@PostMapping("/replace/update")
	public String showReplace(@ModelAttribute("authUserEppn") String authUserEppn,
							  @RequestParam(value = "userIds", required = false) String[] userEmails,
							  @RequestParam(value = "beginDate", required = false) String beginDate,
							  @RequestParam(value = "endDate", required = false) String endDate,
							  RedirectAttributes redirectAttributes) {
		userService.updateReplaceUserBy(authUserEppn, userEmails, beginDate, endDate);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Le remplacement a été modifier"));
		return "redirect:/user/users/replace";
	}

	@GetMapping("/replace/transfer")
	public String transfert(@ModelAttribute("authUserEppn") String authUserEppn, RedirectAttributes redirectAttributes) {
		int result = signBookService.transfer(authUserEppn);
		if(result > 0) {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Le transfert des demandes à bien été effectué. " + result + " demande(s) transférées."));
		} else {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Aucune modification n'a été effectuée"));
		}
		return "redirect:/user/users/replace";
	}
}
