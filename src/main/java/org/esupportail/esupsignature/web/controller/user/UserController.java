package org.esupportail.esupsignature.web.controller.user;

import org.apache.commons.io.IOUtils;
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
import org.esupportail.esupsignature.service.ldap.LdapPersonService;
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
import javax.servlet.http.HttpServletResponse;
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
		return userService.getUserFromAuthentication();
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
		    @RequestParam(value = "referer", required=false) String referer,
    		@RequestParam(value = "signImageBase64", required=false) String signImageBase64, 
    		@RequestParam(value = "emailAlertFrequency", required=false) EmailAlertFrequency emailAlertFrequency,
    		@RequestParam(value = "emailAlertHour", required=false) String emailAlertHour,
    		@RequestParam(value = "emailAlertDay", required=false) DayOfWeek emailAlertDay,
    		@RequestParam(value = "multipartKeystore", required=false) MultipartFile multipartKeystore, Model model) throws Exception {
        model.asMap().clear();
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
    	if(referer != null && !"".equals(referer)) {
			return "redirect:" + referer;
		} else {
			return "redirect:/user/users/?form";
		}
    }
    
    @RequestMapping(value = "/view-cert", method = RequestMethod.GET, produces = "text/html")
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
    
	@RequestMapping(value = "/get-keystore-file", method = RequestMethod.GET)
	public void getSignedFile(HttpServletResponse response, Model model) {
		User user = userService.getUserFromAuthentication();
		Document userKeystore = user.getKeystore();
		try {
			response.setHeader("Content-Disposition", "inline;filename=\"" + userKeystore.getFileName() + "\"");
			response.setContentType(userKeystore.getContentType());
			IOUtils.copy(userKeystore.getInputStream(), response.getOutputStream());
		} catch (Exception e) {
			logger.error("get file error", e);
		}
	}

	@RequestMapping(value="/search-user")
	@ResponseBody
	public List<PersonLdap> searchLdap(@RequestParam(value="searchString") String searchString, @RequestParam(required=false) String ldapTemplateName) {
		logger.debug("ldap search for : " + searchString);
		return userService.getPersonLdaps(searchString, ldapTemplateName);
   }

	@GetMapping("/properties")
	public String properties(Model model) {
		User user = userService.getUserFromAuthentication();
		List<UserShare> userShares = userShareRepository.findByUser(user);
		List<UserPropertie> userProperties = userPropertieRepository.findByUser(user);
		model.addAttribute("userProperties", userProperties);
		model.addAttribute("forms", formService.getFormsByUser(user, true));
		model.addAttribute("users", userRepository.findAll());
		model.addAttribute("activeMenu", "params");
		return "user/users/properties";
	}

	@GetMapping("/shares")
	public String params(Model model) {
		User user = userService.getUserFromAuthentication();
		List<UserShare> userShares = userShareRepository.findByUser(user);
		model.addAttribute("userShares", userShares);
		model.addAttribute("forms", formService.getFormsByUser(user, true));
		model.addAttribute("users", userRepository.findAll());
		model.addAttribute("activeMenu", "params");
		return "user/users/shares";
	}

	@PostMapping("/add-share")
	public String addShare(@RequestParam("formId") long formId,
						   @RequestParam("userIds") long[] userIds,
						   @RequestParam("beginDate") String beginDate,
						   @RequestParam("endDate") String endDate,
						   @PathVariable String eppn) {
		User user = userService.getUserFromSu(eppn);
		UserShare userShare = new UserShare();
		userShare.setUser(user);
		userShare.setForm(formRepository.findById(formId).get());
		for (long toUserId : userIds) {
			userShare.getToUsers().add(userRepository.findById(toUserId).get());
		}
		try {
			userShare.setBeginDate(new SimpleDateFormat("dd/MM/yyyy").parse(beginDate));
			userShare.setEndDate(new SimpleDateFormat("dd/MM/yyyy").parse(endDate));
		} catch (ParseException e) {
			logger.error("error on parsing dates", e);
		}
		userShareRepository.save(userShare);
		return "redirect:/user/params";
	}

	@DeleteMapping("/del-share/{id}")
	public String addShare(@PathVariable long id, @PathVariable String eppn) {
		User user = userService.getUserFromSu(eppn);
		UserShare userShare = userShareRepository.findById(id).get();
		if (userShare.getUser().equals(user)) {
			userShareRepository.delete(userShare);
		}
		return "redirect:/user/params";
	}

}
