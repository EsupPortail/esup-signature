package org.esupportail.esupsignature.web.user;

import java.io.IOException;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignBook.SignBookType;
import org.esupportail.esupsignature.domain.SignRequestParams.NewPageType;
import org.esupportail.esupsignature.domain.SignRequestParams.SignType;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.domain.User.EmailAlertFrequency;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.UserKeystoreService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RequestMapping("/user/users")
@Controller
@Scope(value="session")
@Transactional
@EnableScheduling
public class UserController {

	private static final Logger logger = LoggerFactory.getLogger(UserController.class);
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return "user/users";
	}
	
	@ModelAttribute("user")
	public User getUser() {
		return userService.getUserFromAuthentication();
	}
	
	
	@Autowired(required = false)
	private LdapPersonService ldapPersonService;
	
	@Resource
	private DocumentService documentService;
	
	@Resource
	private FileService fileService;

	@Resource
	private UserKeystoreService userKeystoreService;
	
	@Resource
	private UserService userService;

	@Resource
	private SignBookService signBookService;
	
	@Value("${sign.passwordTimeout}")
	private long passwordTimeout;
	
	private String password;
	
	long startTime;
	
	public void setPassword(String password) {
		startTime = System.currentTimeMillis();
		this.password = password;
	}
	
    @RequestMapping(produces = "text/html")
    public String show(Model uiModel) throws Exception {
    	User user = userService.getUserFromAuthentication();
    	if(!user.isReady()) {
			return "redirect:/user/users/?form";
		}    	
    	populateEditForm(uiModel, user);
    	if(user.getSignImage() != null) {
        	uiModel.addAttribute("signFile", fileService.getBase64Image(user.getSignImage()));
        }
        if(user.getKeystore() != null) {
        	uiModel.addAttribute("keystore", user.getKeystore().getFileName());
        }        
        List<String> recipientEmails = new ArrayList<>();
		recipientEmails.add(user.getEmail());
        SignBook defaultSignBook = SignBook.findSignBooksByRecipientEmailsAndSignBookTypeEquals(recipientEmails, SignBookType.user).getSingleResult();
        uiModel.addAttribute("defaultSignBook", defaultSignBook);
        uiModel.addAttribute("isPasswordSet", (password != null && password != ""));
        return "user/users/show";
    }
    
    @RequestMapping(params = "form", produces = "text/html")
    public String createForm(Model uiModel) throws IOException, SQLException {
    	//TODO : choix automatique du mode de signature
		User user = userService.getUserFromAuthentication();
		if(user != null) {
	        uiModel.addAttribute("user", user);
	        
	        List<String> recipientEmails = new ArrayList<>();
			recipientEmails.add(user.getEmail());
        	SignBook signBook = SignBook.findSignBooksByRecipientEmailsAndSignBookTypeEquals(recipientEmails, SignBookType.user).getSingleResult();
        	uiModel.addAttribute("signBook", signBook);
        	uiModel.addAttribute("signTypes", Arrays.asList(SignType.values()));
        	uiModel.addAttribute("newPageTypes", Arrays.asList(NewPageType.values()));
        	uiModel.addAttribute("emailAlertFrequencies", Arrays.asList(EmailAlertFrequency.values()));
        	uiModel.addAttribute("daysOfWeek", Arrays.asList(DayOfWeek.values()));

        	if(user.isReady()) {
        		if(user.getSignImage() != null) {
        			uiModel.addAttribute("signFile", fileService.getBase64Image(user.getSignImage()));
        		}
	        	return "user/users/update";
	        } else {
				return "user/users/create";
	        }
		} else {
			user = new User();
			return "user/users/create";
		}

    }
    
    @RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(@Valid User user, @RequestParam(value = "newPageType", required=false) String newPageType, @RequestParam(value = "signType", required=false) String signType, @RequestParam(value = "multipartKeystore", required=false) MultipartFile multipartKeystore, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttrs) throws Exception {
    	if (bindingResult.hasErrors()) {
        	populateEditForm(uiModel, user);
            return "user/users/update";
        }
        uiModel.asMap().clear();
		User userToUpdate = userService.getUserFromAuthentication();
        if(!multipartKeystore.isEmpty()) {
            if(userToUpdate.getKeystore() != null) {
            	userToUpdate.getKeystore().remove();
            }
            userToUpdate.setKeystore(documentService.createDocument(multipartKeystore, multipartKeystore.getOriginalFilename()));
        }
        Document oldSignImage = userToUpdate.getSignImage();
        if(!user.getSignImageBase64().isEmpty()) {
        	userToUpdate.setSignImage(documentService.createDocument(user.getSignImageBase64(), userToUpdate.getEppn() + "_sign", "application/png"));
        }
        if(oldSignImage != null) {
        	oldSignImage.getBigFile().getBinaryFile().free();
    		oldSignImage.getBigFile().remove();
    		oldSignImage.remove();
    	}
        List<String> recipientEmails = new ArrayList<>();
		recipientEmails.add(user.getEmail());
    	SignBook signBook = SignBook.findSignBooksByRecipientEmailsAndSignBookTypeEquals(recipientEmails, SignBookType.user).getSingleResult();
    	if(signType != null) {
    		signBook.getSignRequestParams().setSignType(SignType.valueOf(signType));
    	}
    	userToUpdate.setReady(true);
    	return "redirect:/user/users/";
    }
    
    @RequestMapping(value = "/viewCert", method = RequestMethod.GET, produces = "text/html")
    public String viewCert(@RequestParam(value =  "password", required = false) String password, RedirectAttributes redirectAttrs) throws Exception {
		User user = userService.getUserFromAuthentication();
		if (password != null && !"".equals(password)) {
        	setPassword(password);
        }
		try {
        	redirectAttrs.addFlashAttribute("messageCustom", userKeystoreService.checkKeystore(user.getKeystore().getJavaIoFile(), this.password));
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
		Document file = user.getKeystore();
		try {
			response.setHeader("Content-Disposition", "inline;filename=\"" + file.getFileName() + "\"");
			response.setContentType(file.getContentType());
			IOUtils.copy(file.getBigFile().getBinaryFile().getBinaryStream(), response.getOutputStream());
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
		List<SignBook> signBooks = SignBook.findSignBooksBySignBookTypeEquals(SignBookType.group).getResultList();
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
        uiModel.addAttribute("files", Document.findAllDocuments());
    }

	@Scheduled(fixedDelay = 5000)
	public void clearPassword () {
		if(startTime > 0) {
			if(System.currentTimeMillis() - startTime > passwordTimeout) {
				password = "";
				startTime = 0;
			}
		}
	}
	
}
