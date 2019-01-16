package org.esupportail.esupsignature.web.user;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.esupportail.esupsignature.domain.File;
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
	
	@Autowired
	PersonLdapDao personDao;
	
	@Resource
	FileService fileService;

	@Resource
	UserKeystoreService userKeystoreService;
	
    @RequestMapping(produces = "text/html")
    public String settings(Model uiModel) throws Exception {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
		if(User.countFindUsersByEppnEquals(eppn) == 0) {
			return "redirect:/user/?form";
		}
    	User user = User.findUsersByEppnEquals(eppn).getSingleResult();
    	
    	populateEditForm(uiModel, user);
        File signFile = user.getSignImage();
        uiModel.addAttribute("signFile", fileService.getBase64Image(signFile));
        uiModel.addAttribute("keystore", user.getKeystore().getFileName());
        return "user/users/show";
    }
    
    @RequestMapping(params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
		if(User.countFindUsersByEppnEquals(eppn) > 0) {
			List<PersonLdap> persons =  personDao.getPersonNamesByEppn(eppn);
			User user = User.findUsersByEppnEquals(eppn).getSingleResult();
			user.setEmail(persons.get(0).getMail());
	        uiModel.addAttribute("user", user);
		} else {
	        uiModel.addAttribute("user", new User());

		}
        return "user/users/update";
    }
    
    @RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(@Valid User user, @RequestParam("signImageBase64") String signImageBase64, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) throws Exception {
        if (bindingResult.hasErrors()) {
        	populateEditForm(uiModel, user);
            return "user/users/update";
        }
        uiModel.asMap().clear();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
		PersonLdap person =  personDao.getPersonNamesByEppn(eppn).get(0);
        if(User.countFindUsersByEppnEquals(eppn) > 0) {
            User userToUdate = User.findUsersByEppnEquals(eppn).getSingleResult();
            if(!user.getPublicKey().isEmpty()) {
            	userToUdate.setEmail(person.getMail());
            	userToUdate.setName(person.getSn());
            	userToUdate.setFirstname(person.getGivenName());
	            java.io.File file = userKeystoreService.createKeystore(user.getEppn(), user.getEppn(), user.getPublicKey(), user.getPassword());
	            InputStream inputStream = new FileInputStream(file);
            	userToUdate.getKeystore().remove();
	            userToUdate.setKeystore(fileService.addFile(inputStream, file.getName(), file.length(), "application/jks"));
            }
            if(!signImageBase64.isEmpty()) {
            	userToUdate.getSignImage().remove();
            	userToUdate.setSignImage(fileService.addFile(signImageBase64, eppn + "_sign", "application/png"));
            }
            userToUdate.merge();
        } else {
	        try {
            	user.setEmail(person.getMail());
            	user.setName(person.getSn());
            	user.setFirstname(person.getGivenName());
            	user.setSignImage(fileService.addFile(signImageBase64, eppn + "_sign.png", "application/png"));
		        java.io.File file = userKeystoreService.createKeystore(user.getEppn(), user.getEppn(), user.getPublicKey(), user.getPassword());
		        user.setKeystore(fileService.addFile(file, "application/jks"));
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
        }
        try {
        	redirectAttrs.addFlashAttribute("messageCustom", userKeystoreService.checkKeystore(user.getKeystore().getBigFile().toJavaIoFile(), user.getEppn(), user.getEppn()));
        } catch (Exception e) {
        	redirectAttrs.addFlashAttribute("messageCustom", "bad password");
		}
        return "redirect:/user/users/";
    }
    
    void populateEditForm(Model uiModel, User user) {
        uiModel.addAttribute("user", user);
        uiModel.addAttribute("files", File.findAllFiles());
    }

}
