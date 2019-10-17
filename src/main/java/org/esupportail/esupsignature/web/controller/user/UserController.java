package org.esupportail.esupsignature.web.controller.user;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.entity.SignRequestParams.NewPageType;
import org.esupportail.esupsignature.entity.SignRequestParams.SignType;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.User.EmailAlertFrequency;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.repository.BigFileRepository;
import org.esupportail.esupsignature.repository.DocumentRepository;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.esupportail.esupsignature.service.sign.SignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
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
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RequestMapping("user/users")
@Controller
@Scope(value="session")
@Transactional
public class UserController {

	private static final Logger logger = LoggerFactory.getLogger(UserController.class);
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return "user/users";
	}

	@Resource
	private UserRepository userRepository;
	
	@Resource
	private LdapPersonService ldapPersonService;

	@Resource
	private DocumentRepository documentRepository;
	
	@Resource
	private DocumentService documentService;
	
	@Resource
	private BigFileRepository bigFileRepository;
	
	@Resource
	private FileService fileService;

	@Resource
	private UserKeystoreService userKeystoreService;
	
	@Resource
	private UserService userService;

	@Resource
	private SignService signService;

	@Resource
	private SignBookRepository signBookRepository;
	
	@Resource
	private SignBookService signBookService;

	private String password;
	
	long startTime;
	
	public void setPassword(String password) {
		startTime = System.currentTimeMillis();
		this.password = password;
	}
	
    @GetMapping
    public String show(Model uiModel) throws Exception {
    	User user = userService.getUserFromAuthentication();
    	if(!userService.isUserReady(user) || signBookService.getUserSignBook(user) == null) {
			return "redirect:/user/users/?form";
		}    	
    	populateEditForm(uiModel, user);
    	if(user.getSignImage() != null) {
        	uiModel.addAttribute("signFile", fileService.getBase64Image(user.getSignImage()));
        }
        if(user.getKeystore() != null) {
        	uiModel.addAttribute("keystore", user.getKeystore().getFileName());
        }        
        uiModel.addAttribute("defaultSignBook", signBookService.getUserSignBook(user));
        uiModel.addAttribute("isPasswordSet", (password != null && password != ""));
        return "user/users/show";
    }
    
    @GetMapping(params = "form")
    public String createForm(Model model, @RequestParam(value = "referer", required=false) String referer, HttpServletRequest request) throws IOException, SQLException {
    	//TODO : choix automatique du mode de signature
		User user = userService.getUserFromAuthentication();
		if(user != null) {
	        model.addAttribute("user", user);
        	model.addAttribute("signBook", signBookService.getUserSignBook(user));
        	model.addAttribute("signTypes", Arrays.asList(SignType.values()));
        	model.addAttribute("newPageTypes", Arrays.asList(NewPageType.values()));
        	model.addAttribute("emailAlertFrequencies", Arrays.asList(EmailAlertFrequency.values()));
        	model.addAttribute("daysOfWeek", Arrays.asList(DayOfWeek.values()));
        	if(referer != null && !"".equals(referer) && !"null".equals(referer)) {
				model.addAttribute("referer", request.getHeader("referer"));
			}

        	if(userService.isUserReady(user)) {
        		if(user.getSignImage() != null) {
        			model.addAttribute("signFile", fileService.getBase64Image(user.getSignImage()));
        		}
	        	return "user/users/update";
	        } else {
				return "user/users/create";
	        }
		} else {
			user = new User();
			model.addAttribute("user", user);
			return "user/users/create";
		}

    }
    
    @PostMapping
    public String create(Long id,
		    @RequestParam(value = "referer", required=false) String referer,
    		@RequestParam(value = "signImageBase64", required=false) String signImageBase64, 
    		@RequestParam(value = "newPageType", required=false) NewPageType newPageType, 
    		@RequestParam(value = "signType", required=false) SignType signType,
    		@RequestParam(value = "emailAlertFrequency", required=false) EmailAlertFrequency emailAlertFrequency,
    		@RequestParam(value = "emailAlertHour", required=false) String emailAlertHour,
    		@RequestParam(value = "emailAlertDay", required=false) DayOfWeek emailAlertDay,
    		@RequestParam(value = "multipartKeystore", required=false) MultipartFile multipartKeystore, Model model) throws Exception {
        model.asMap().clear();
        User user = userRepository.findById(id).get();
		User userToUpdate = userService.getUserFromAuthentication();
        if(!multipartKeystore.isEmpty()) {
            if(userToUpdate.getKeystore() != null) {
            	bigFileRepository.delete(userToUpdate.getKeystore().getBigFile());
            	documentRepository.delete(userToUpdate.getKeystore());
            }
            userToUpdate.setKeystore(documentService.createDocument(multipartKeystore, multipartKeystore.getOriginalFilename()));
        }
        Document oldSignImage = userToUpdate.getSignImage();
        if(signImageBase64 != null && !signImageBase64.isEmpty()) {
        	userToUpdate.setSignImage(documentService.createDocument(fileService.base64Transparence(signImageBase64), userToUpdate.getEppn() + "_sign"));
            if(oldSignImage != null) {
            	oldSignImage.getBigFile().getBinaryFile().free();
            	bigFileRepository.delete(oldSignImage.getBigFile());
            	documentRepository.delete(oldSignImage);
        	}
        }

    	SignBook signBook = signBookService.getUserSignBook(user);
    	if(signBook != null) {
	    	if(signType != null) {
	    		signBook.getSignRequestParams().get(0).setSignType(signType);
	    		signBook.getSignRequestParams().get(0).setNewPageType(newPageType);
	    	}
    	}else {
    		signBookService.createUserSignBook(user);
    	}
    	userToUpdate.setEmailAlertFrequency(emailAlertFrequency);
    	userToUpdate.setEmailAlertHour(emailAlertHour);
    	userToUpdate.setEmailAlertDay(emailAlertDay);
    	if(referer != null && !"".equals(referer)) {
			return "redirect:" + referer;
		} else {
			return "redirect:/user/users/";
		}
    }
    
    @RequestMapping(value = "/viewCert", method = RequestMethod.GET, produces = "text/html")
    public String viewCert(@RequestParam(value =  "password", required = false) String password, RedirectAttributes redirectAttrs) throws Exception {
		User user = userService.getUserFromAuthentication();
		if (password != null && !"".equals(password)) {
        	setPassword(password);
        }
		try {
        	redirectAttrs.addFlashAttribute("messageCustom", userKeystoreService.checkKeystore(user.getKeystore().getInputStream(), this.password));
        } catch (Exception e) {
        	logger.error("open keystore fail", e);
        	this.password = "";
			startTime = 0;
        	redirectAttrs.addFlashAttribute("messageError", "security_bad_password");
		}
        return "redirect:/user/users/";
    }
    
	@RequestMapping(value = "/get-keystore-file", method = RequestMethod.GET)
	public void getSignedFile(HttpServletResponse response, Model model) {
		User user = userService.getUserFromAuthentication();
		Document userKeystore = user.getKeystore();
		try {
			response.setHeader("Content-Disposition", "inline;filename=\"" + userKeystore.getFileName() + "\"");
			response.setContentType(userKeystore.getContentType());
			IOUtils.copy(userKeystore.getBigFile().getBinaryFile().getBinaryStream(), response.getOutputStream());
		} catch (Exception e) {
			logger.error("get file error", e);
		}
	}
    
	@RequestMapping(value="/searchLdap")
	@ResponseBody
	public List<PersonLdap> searchLdap(@RequestParam(value="searchString") String searchString, @RequestParam(required=false) String ldapTemplateName) {

		logger.info("ldap search for : " + searchString);
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
		List<PersonLdap> ldapList = new ArrayList<PersonLdap>();
		List<SignBook> signBooks = signBookRepository.findBySignBookType(SignBookType.group);
		for(SignBook signBook : signBooks) {
			PersonLdap personLdap = new PersonLdap();
			personLdap.setUid("parapheur");
			personLdap.setMail(signBook.getName());
			personLdap.setDisplayName(signBook.getName());
			ldapList.add(personLdap);
		}
		if(ldapPersonService != null && !searchString.trim().isEmpty() && searchString.length() > 3) {
			List<PersonLdap> ldapSearchList = new ArrayList<PersonLdap>();
			ldapSearchList = ldapPersonService.search(searchString, ldapTemplateName);
			ldapList.addAll(ldapSearchList.stream().sorted(Comparator.comparing(PersonLdap::getDisplayName)).collect(Collectors.toList()));

		}


		return ldapList;
   }
	
    void populateEditForm(Model uiModel, User user) {
        uiModel.addAttribute("user", user);
        uiModel.addAttribute("files", documentRepository.findAll());
    }

	@Scheduled(fixedDelay = 5000)
	public void clearPassword () {
		if(startTime > 0) {
			if(System.currentTimeMillis() - startTime > signService.getPasswordTimeout()) {
				password = "";
				startTime = 0;
			}
		}
	}
	
}
