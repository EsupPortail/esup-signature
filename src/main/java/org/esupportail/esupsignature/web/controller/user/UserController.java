package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.User.EmailAlertFrequency;
import org.esupportail.esupsignature.entity.UserPropertie;
import org.esupportail.esupsignature.entity.UserShare;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.ldap.PersonLdap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
	private UserRepository userRepository;

	@Resource
	private DocumentRepository documentRepository;
	
	@Resource
	private DocumentService documentService;
	
	@Resource
	private BigFileRepository bigFileRepository;

	@Resource
	private FileService fileService;

	@Resource
	private FormService formService;

	@Resource
	private WorkflowService workflowService;

	@Resource
	private UserKeystoreService userKeystoreService;
	
	@Resource
	private UserService userService;

	@Resource
	private UserShareRepository userShareRepository;

	@Resource
	private UserPropertieRepository userPropertieRepository;

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
    		@RequestParam(value = "emailAlertHour", required=false) String emailAlertHour,
    		@RequestParam(value = "emailAlertDay", required=false) DayOfWeek emailAlertDay,
    		@RequestParam(value = "multipartKeystore", required=false) MultipartFile multipartKeystore, RedirectAttributes redirectAttributes) throws Exception {
        if(multipartKeystore != null && !multipartKeystore.isEmpty()) {
            if(authUser.getKeystore() != null) {
            	bigFileRepository.delete(authUser.getKeystore().getBigFile());
            	documentRepository.delete(authUser.getKeystore());
            }
            authUser.setKeystore(documentService.createDocument(multipartKeystore.getInputStream(), authUser.getEppn() + "_cert.p12", multipartKeystore.getContentType()));
        }
        if(signImageBase64 != null && !signImageBase64.isEmpty()) {
        	authUser.getSignImages().add(documentService.createDocument(fileService.base64Transparence(signImageBase64), authUser.getEppn() + "_sign.png", "image/png"));
        }
    	authUser.setEmailAlertFrequency(emailAlertFrequency);
    	authUser.setEmailAlertHour(emailAlertHour);
    	authUser.setEmailAlertDay(emailAlertDay);
    	redirectAttributes.addFlashAttribute("messageSuccess", "Vos paramètres on été enregistrés");
		return "redirect:/user/users";
    }

	@GetMapping("/delete-sign/{id}")
	public String deleteSign(@ModelAttribute("authUser") User authUser, @PathVariable long id, RedirectAttributes redirectAttributes) {
    	Document signDocument = documentRepository.findById(id).get();
		authUser.getSignImages().remove(signDocument);
		redirectAttributes.addFlashAttribute("messageInfo", "Signature supprimée");
		return "redirect:/user/users";
	}


	@GetMapping(value = "/view-cert")
    public String viewCert(@RequestParam(value =  "password", required = false) String password, RedirectAttributes redirectAttrs) {
		User user = userService.getUserFromAuthentication();
		try {
			logger.info(user.getKeystore().getInputStream().read() + "");
        	redirectAttrs.addFlashAttribute("messageCustom", userKeystoreService.checkKeystore(user.getKeystore().getInputStream(), password));
        } catch (Exception e) {
        	logger.error("open keystore fail", e);
        	redirectAttrs.addFlashAttribute("messageError", "Mauvais mot de passe");
		}
        return "redirect:/user/users/?form";
    }

	@GetMapping(value="/search-user")
	@ResponseBody
	public List<PersonLdap> searchLdap(@RequestParam(value="searchString") String searchString, @RequestParam(required=false) String ldapTemplateName) {
		logger.debug("ldap search for : " + searchString);
		return userService.getPersonLdaps(searchString, ldapTemplateName);
   }

	@GetMapping("/properties")
	public String properties(Model model) {
		User user = userService.getUserFromAuthentication();
		List<UserPropertie> userProperties = userPropertieRepository.findByUser(user);
		model.addAttribute("userProperties", userProperties);
		model.addAttribute("forms", formService.getFormsByUser(user, user));
		model.addAttribute("users", userRepository.findAll());
		model.addAttribute("activeMenu", "properties");
		return "user/users/properties";
	}

	@GetMapping("/shares")
	public String params(@ModelAttribute("authUser") User authUser, Model model) {
		List<UserShare> userShares = userShareRepository.findByUser(authUser);
		model.addAttribute("userShares", userShares);
		model.addAttribute("forms", formService.getFormsByUser(authUser, authUser));
		model.addAttribute("workflows", workflowService.getWorkflowsForUser(authUser, authUser));
		model.addAttribute("users", userRepository.findAll());
		model.addAttribute("activeMenu", "shares");
		return "user/users/shares";
	}

	@PostMapping("/add-share")
	public String addShare(@ModelAttribute("authUser") User authUser,
						   @RequestParam(value = "form", required = false) Long[] form,
						   @RequestParam(value = "workflow", required = false) Long[] workflow,
						   @RequestParam("type") String type,
						   @RequestParam("userIds") String[] userEmails,
						   @RequestParam("beginDate") String beginDate,
						   @RequestParam("endDate") String endDate) {
    	if(form == null) form = new Long[] {};
		if(workflow == null) workflow = new Long[] {};
		List<User> users = new ArrayList<>();
		for (String userEmail : userEmails) {
			users.add(userService.checkUserByEmail(userEmail));
		}
		Date beginDateDate = null;
		Date endDateDate = null;
		if (beginDate != null && endDate != null) {
			try {
				beginDateDate = new SimpleDateFormat("yyyy-MM-dd").parse(beginDate);
				endDateDate = new SimpleDateFormat("yyyy-MM-dd").parse(endDate);
			} catch (ParseException e) {
				logger.error("error on parsing dates");
			}
		}
		userService.createUserShare(Arrays.asList(form), Arrays.asList(workflow), type, users, beginDateDate, endDateDate, authUser);
		return "redirect:/user/users/shares";
	}

	@DeleteMapping("/del-share/{id}")
	public String delShare(@ModelAttribute("authUser") User authUser, @PathVariable long id, RedirectAttributes redirectAttributes) {
		UserShare userShare = userShareRepository.findById(id).get();
		if (userShare.getUser().equals(authUser)) {
			userShareRepository.delete(userShare);
		}
		redirectAttributes.addFlashAttribute("messageInfo", "Élément supprimé");
		return "redirect:/user/users/shares";
	}

	@GetMapping("/change-share")
	public String change(@ModelAttribute("authUser") User authUser, @RequestParam(required = false) String eppn, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) {
		if(userService.switchToShareUser(eppn)) {
			if(eppn == null || eppn.isEmpty()) {
				redirectAttributes.addFlashAttribute("messageSuccess", "Délégation désactivée");
			} else {
				redirectAttributes.addFlashAttribute("messageSuccess", "Délégation activée : " + eppn);
			}
		} else {
			redirectAttributes.addFlashAttribute("messageError", "Aucune délégation active en ce moment");
		}
		String referer = httpServletRequest.getHeader("Referer");
		return "redirect:"+ referer;
	}

	@GetMapping("/mark-as-read/{id}")
	public String markAsRead(@ModelAttribute("authUser") User authUser, @PathVariable long id, HttpServletRequest httpServletRequest) {
    	logger.info(authUser.getEppn() + " mark " + id + " as read");
		userService.disableMessage(authUser, id);
		String referer = httpServletRequest.getHeader("Referer");
		return "redirect:"+ referer;
	}

}
