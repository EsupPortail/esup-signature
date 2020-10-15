package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.EmailAlertFrequency;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.ldap.PersonLdap;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
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
import java.util.*;
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
	private UserShareService userShareService;

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
    		@RequestParam(value = "emailAlertHour", required=false) Integer emailAlertHour,
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
    	redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Vos paramètres on été enregistrés"));
		return "redirect:/user/users";
    }

	@GetMapping("/delete-sign/{id}")
	public String deleteSign(@ModelAttribute("authUser") User authUser, @PathVariable long id, RedirectAttributes redirectAttributes) {
    	Document signDocument = documentRepository.findById(id).get();
		authUser.getSignImages().remove(signDocument);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Signature supprimée"));
		return "redirect:/user/users";
	}


	@GetMapping(value = "/view-cert")
    public String viewCert(@RequestParam(value =  "password", required = false) String password, RedirectAttributes redirectAttributes) {
		User user = userService.getUserFromAuthentication();
		try {
			logger.info(user.getKeystore().getInputStream().read() + "");
        	redirectAttributes.addFlashAttribute("message", new JsonMessage("custom", userKeystoreService.checkKeystore(user.getKeystore().getInputStream(), password)));
        } catch (Exception e) {
        	logger.error("open keystore fail", e);
        	redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Mauvais mot de passe"));
		}
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
		model.addAttribute("shareTypes", ShareType.values());
		model.addAttribute("forms", formService. getAuthorizedToShareForms());
		model.addAttribute("workflows", workflowService.getAuthorizedToShareWorkflows());
		model.addAttribute("users", userRepository.findAll());
		model.addAttribute("activeMenu", "shares");
		return "user/users/shares/list";
	}

	@GetMapping("/shares/update/{id}")
	public String params(@ModelAttribute("authUser") User authUser, @PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
		model.addAttribute("activeMenu", "shares");
		UserShare userShare = userShareRepository.findById(id).get();
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
		try {
			userShareService.createUserShare(Arrays.asList(form), Arrays.asList(workflow), types, users, beginDateDate, endDateDate, authUser);
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
		UserShare userShare = userShareRepository.findById(id).get();
		if(userShare.getUser().equals(authUser)) {
			userShare.getToUsers().clear();
			for (String userEmail : userEmails) {
				userShare.getToUsers().add(userService.checkUserByEmail(userEmail));
			}
			userShare.getShareTypes().clear();
			List<ShareType> authorizedShareTypes = new ArrayList<>();
			if(userShare.getWorkflow() != null) {
				authorizedShareTypes.addAll(userShare.getWorkflow().getAuthorizedShareTypes());
			}
			if(userShare.getForm() != null ) {
				authorizedShareTypes.addAll(userShare.getForm().getAuthorizedShareTypes());
			}
			for(String type : types) {
				if(authorizedShareTypes.contains(ShareType.valueOf(type))) {
					userShare.getShareTypes().add(ShareType.valueOf(type));
				}
			}
			if (beginDate != null && endDate != null) {
				try {
					userShare.setBeginDate(new SimpleDateFormat("yyyy-MM-dd").parse(beginDate));
					userShare.setEndDate(new SimpleDateFormat("yyyy-MM-dd").parse(endDate));
				} catch (ParseException e) {
					logger.error("error on parsing dates");
				}
			}
		}

		return "redirect:/user/users/shares";
	}

	@DeleteMapping("/del-share/{id}")
	public String delShare(@ModelAttribute("authUser") User authUser, @PathVariable long id, RedirectAttributes redirectAttributes) {
		UserShare userShare = userShareRepository.findById(id).get();
		if (userShare.getUser().equals(authUser)) {
			userShareRepository.delete(userShare);
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

	@GetMapping("/mark-as-read/{id}")
	public String markAsRead(@ModelAttribute(value = "authUser" , binding = false) User authUser, @PathVariable long id, HttpServletRequest httpServletRequest) {
    	logger.info(authUser.getEppn() + " mark " + id + " as read");
    	if(id == 0) {
    		User user = userRepository.findByEppn(authUser.getEppn()).get(0);
    		user.setSplash(true);
		} else {
			userService.disableMessage(authUser, id);
		}

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

}
