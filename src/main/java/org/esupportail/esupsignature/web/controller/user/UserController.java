package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.User.EmailAlertFrequency;
import org.esupportail.esupsignature.entity.UserPropertie;
import org.esupportail.esupsignature.entity.UserShare;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.FormService;
import org.esupportail.esupsignature.service.UserKeystoreService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.pdf.PdfService;
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
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.util.Arrays;
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

	@ModelAttribute("user")
	public User getUser() {
		return userService.getCurrentUser();
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
	private PdfService pdfService;

	@Resource
	private FormService formService;

	@Resource
	private UserKeystoreService userKeystoreService;
	
	@Resource
	private UserService userService;

	@Resource
	private FormRepository formRepository;

	@Resource
	private UserShareRepository userShareRepository;

	@Resource
	private UserPropertieRepository userPropertieRepository;

    @GetMapping
    public String createForm(Model model, @RequestParam(value = "referer", required=false) String referer, HttpServletRequest request) throws IOException, SQLException {
		User user = userService.getUserFromAuthentication();
		if(user != null) {
	        model.addAttribute("user", user);
        	model.addAttribute("signTypes", Arrays.asList(SignType.values()));
        	model.addAttribute("emailAlertFrequencies", Arrays.asList(EmailAlertFrequency.values()));
        	model.addAttribute("daysOfWeek", Arrays.asList(DayOfWeek.values()));
        	if(referer != null && !"".equals(referer) && !"null".equals(referer)) {
				model.addAttribute("referer", request.getHeader("referer"));
			}
			if(user.getSignImage() != null) {
				model.addAttribute("signFile", fileService.getBase64Image(user.getSignImage()));
				int[] size = pdfService.getSignSize(user.getSignImage().getInputStream());
				model.addAttribute("signWidth", size[0]);
				model.addAttribute("signHeight", size[1]);
			}
			return "user/users/update";
		} else {
			user = new User();
			model.addAttribute("user", user);
			return "user/users/create";
		}

    }
    
    @PostMapping
    public String create(
    		@RequestParam(value = "signImageBase64", required=false) String signImageBase64,
    		@RequestParam(value = "emailAlertFrequency", required=false) EmailAlertFrequency emailAlertFrequency,
    		@RequestParam(value = "emailAlertHour", required=false) String emailAlertHour,
    		@RequestParam(value = "emailAlertDay", required=false) DayOfWeek emailAlertDay,
    		@RequestParam(value = "multipartKeystore", required=false) MultipartFile multipartKeystore, RedirectAttributes redirectAttributes) throws Exception {
		User user = userService.getUserFromAuthentication();
        if(multipartKeystore != null && !multipartKeystore.isEmpty()) {
            if(user.getKeystore() != null) {
            	bigFileRepository.delete(user.getKeystore().getBigFile());
            	documentRepository.delete(user.getKeystore());
            }
            user.setKeystore(documentService.createDocument(multipartKeystore.getInputStream(), user.getEppn() + "_cert.p12", multipartKeystore.getContentType()));
        }
        Document oldSignImage = user.getSignImage();
        if(signImageBase64 != null && !signImageBase64.isEmpty()) {

        	user.setSignImage(documentService.createDocument(fileService.base64Transparence(signImageBase64), user.getEppn() + "_sign.png", "image/png"));
            if(oldSignImage != null) {
            	oldSignImage.getBigFile().getBinaryFile().getBinaryStream();
            	bigFileRepository.delete(oldSignImage.getBigFile());
            	documentRepository.delete(oldSignImage);
        	}
        }
    	user.setEmailAlertFrequency(emailAlertFrequency);
    	user.setEmailAlertHour(emailAlertHour);
    	user.setEmailAlertDay(emailAlertDay);
    	redirectAttributes.addFlashAttribute("messageSuccess", "Vos paramètres on été enregistrés");
		return "redirect:/user/users/?form";
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
		model.addAttribute("forms", formService.getFormsByUser(user));
		model.addAttribute("users", userRepository.findAll());
		model.addAttribute("activeMenu", "params");
		return "user/users/properties";
	}

	@GetMapping("/shares")
	public String params(Model model) {
		User user = userService.getUserFromAuthentication();
		List<UserShare> userShares = userShareRepository.findByUser(user);
		model.addAttribute("userShares", userShares);
		model.addAttribute("forms", formService.getFormsByUser(user));
		model.addAttribute("users", userRepository.findAll());
		model.addAttribute("activeMenu", "params");
		return "user/users/shares";
	}

	@PostMapping("/add-share")
	public String addShare(@RequestParam("service") Long service, @RequestParam("type") String type, @RequestParam("userIds") String[] userEmails, @RequestParam("beginDate") String beginDate, @RequestParam("endDate") String endDate) {
		User user = userService.getUserFromAuthentication();
		UserShare userShare = new UserShare();
		userShare.setUser(user);
		userShare.setShareType(UserShare.ShareType.valueOf(type));
		userShare.setForm(formRepository.findById(service).get());
		for (String userEmail : userEmails) {
			userShare.getToUsers().add(userService.createUser(userEmail));
		}
		if (beginDate != null && endDate != null) {
			try {
				userShare.setBeginDate(new SimpleDateFormat("dd/MM/yyyy").parse(beginDate));
				userShare.setEndDate(new SimpleDateFormat("dd/MM/yyyy").parse(endDate));
			} catch (ParseException e) {
				logger.error("error on parsing dates");
			}
		}
		userShareRepository.save(userShare);
		return "redirect:/user/users/shares";
	}

	@DeleteMapping("/del-share/{id}")
	public String addShare(@PathVariable long id, RedirectAttributes redirectAttributes) {
		User user = userService.getUserFromAuthentication();
		UserShare userShare = userShareRepository.findById(id).get();
		if (userShare.getUser().equals(user)) {
			userShareRepository.delete(userShare);
		}
		redirectAttributes.addFlashAttribute("messageInfo", "Élément supprimé");
		return "redirect:/user/users/shares";
	}

	@PostMapping("/change")
	public String change(@RequestParam("suEppn") String suEppn, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) {
		if(userService.switchUser(suEppn)) {
			redirectAttributes.addFlashAttribute("messageSuccess", "Délégation activée : " + suEppn);
		}
		String referer = httpServletRequest.getHeader("Referer");
		return "redirect:"+ referer;
	}

}
