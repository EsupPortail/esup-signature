package org.esupportail.esupsignature.web.user;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.ldap.PersonLdapDao;
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.UserKeystoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RequestMapping("/user/users")
@Controller
@Transactional
@Scope(value="session")
public class UserControler {

	private static final Logger log = LoggerFactory.getLogger(UserControler.class);
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return "user/users";
	}
	
	@Autowired(required = false)
	PersonLdapDao personDao;
	
	@Resource
	FileService fileService;

	@Resource
	UserKeystoreService userKeystoreService;
	
    @Autowired
    private HttpServletRequest request;
    
    @RequestMapping(produces = "text/html")
    public String settings(Model uiModel) throws Exception {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
		if(User.countFindUsersByEppnEquals(eppn) == 0) {
			return "redirect:/user/users/?form";
		}
    	User user = User.findUsersByEppnEquals(eppn).getSingleResult();
    	
    	populateEditForm(uiModel, user);
        Document signFile = user.getSignImage();
        uiModel.addAttribute("signFile", fileService.getBase64Image(signFile));
        uiModel.addAttribute("keystore", user.getKeystore().getFileName());
        return "user/users/show";
    }
    
    @RequestMapping(params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
		String mail = request.getHeader("mail");
		String name = request.getHeader("sn");
		String firstName = request.getHeader("givenName");
		if(personDao != null) {
			List<PersonLdap> persons =  personDao.getPersonNamesByEppn(eppn);
			mail = persons.get(0).getMail();
			name = persons.get(0).getSn();
			firstName = persons.get(0).getGivenName(); 
		}
		
		User user;
		if(User.countFindUsersByEppnEquals(eppn) > 0) {
			user = User.findUsersByEppnEquals(eppn).getSingleResult();
			user.setEmail(mail);
	        uiModel.addAttribute("user", user);
		} else {
			user = new User();
			user.setName(name);
			user.setFirstname(firstName);
			user.setEppn(eppn);
			user.setEmail(mail);
		}
        uiModel.addAttribute("user", user);
		return "user/users/update";
    }
    
    @RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(@Valid User user, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttrs) throws Exception {
        if (bindingResult.hasErrors()) {
        	populateEditForm(uiModel, user);
            return "user/users/update";
        }
        uiModel.asMap().clear();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
		String mail = request.getHeader("mail");
		String name = request.getHeader("sn");
		String firstName = request.getHeader("givenName");
		if(personDao != null) {
			List<PersonLdap> persons =  personDao.getPersonNamesByEppn(eppn);
			mail = persons.get(0).getMail();
			name = persons.get(0).getSn();
			firstName = persons.get(0).getGivenName(); 
		}
        if(User.countFindUsersByEppnEquals(eppn) > 0) {
            User userToUdate = User.findUsersByEppnEquals(eppn).getSingleResult();
            if(!user.getPublicKey().isEmpty()) {
            	userToUdate.setEmail(mail);
            	userToUdate.setName(name);
            	userToUdate.setFirstname(firstName);
	            File file = userKeystoreService.createKeystore(user.getEppn(), user.getEppn(), user.getPublicKey(), user.getPassword());
	            InputStream inputStream = new FileInputStream(file);
            	userToUdate.getKeystore().remove();
	            userToUdate.setKeystore(fileService.addFile(inputStream, file.getName(), file.length(), "application/jks"));
            }
            if(!user.getSignImageBase64().isEmpty()) {
            	userToUdate.getSignImage().remove();
            	userToUdate.setSignImage(fileService.addFile(user.getSignImageBase64(), eppn + "_sign", "application/png"));
            }
            userToUdate.merge();
        } else {
            if(user.getSignImageBase64().isEmpty()) {
            	redirectAttrs.addFlashAttribute("messageCustom", "image is required");
            }
        	try {
            	user.setEmail(mail);
            	user.setName(name);
            	user.setFirstname(firstName);
            	user.setSignImage(fileService.addFile(user.getSignImageBase64(), eppn + "_sign.png", "application/png"));
            	if(!user.getPublicKey().isEmpty()) {
            		File file = userKeystoreService.createKeystore(user.getEppn(), user.getEppn(), user.getPublicKey(), user.getPassword());
            		user.setKeystore(fileService.addFile(file, "application/jks"));
            	}
		        user.persist();
	        } catch (Exception e) {
	        	log.error("Create user error", e);
			}
        }
        return "redirect:/user/users/";
    }
    
    @RequestMapping(value = "/viewCert", method = RequestMethod.POST, produces = "text/html")
    public String viewCert(@RequestParam("id") long id, @RequestParam("password") String password, RedirectAttributes redirectAttrs) throws Exception {
        User user = User.findUser(id);
        if(password != null && !password.isEmpty()) {
        	userKeystoreService.setPassword(password);
        } else {
        	userKeystoreService.setPassword("");
        }
        try {
        	redirectAttrs.addFlashAttribute("messageCustom", userKeystoreService.checkKeystore(user.getKeystore().getBigFile().toJavaIoFile(), user.getEppn(), user.getEppn()));
        } catch (Exception e) {
        	log.error("open keystore fail", e);
        	redirectAttrs.addFlashAttribute("messageCustom", "bad password");
		}
        return "redirect:/user/users/";
    }
    
    void populateEditForm(Model uiModel, User user) {
        uiModel.addAttribute("user", user);
        uiModel.addAttribute("files", Document.findAllDocuments());
    }

}
