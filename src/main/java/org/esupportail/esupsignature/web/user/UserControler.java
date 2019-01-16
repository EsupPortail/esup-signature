package org.esupportail.esupsignature.web.user;

import java.io.File;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.ldap.PersonLdapDao;
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.UserKeystoreService;
import org.esupportail.esupsignature.service.UserService;
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
	
	@Autowired(required = false)
	PersonLdapDao personDao;
	
	@Resource
	FileService fileService;

	@Resource
	UserKeystoreService userKeystoreService;
	
	@Resource
	UserService userService;
    
    @RequestMapping(produces = "text/html")
    public String settings(Model uiModel) throws Exception {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User authUser = userService.getUserFromContext(auth.getName());
		if(User.countFindUsersByEppnEquals(authUser.getEppn()) == 0) {
			return "redirect:/user/users/?form";
		}
    	User user = User.findUsersByEppnEquals(authUser.getEppn()).getSingleResult();
    	
    	populateEditForm(uiModel, user);
        if(user.getSignImage().getBigFile().getBinaryFile() != null) {
        	uiModel.addAttribute("signFile", fileService.getBase64Image(user.getSignImage()));
        }
        uiModel.addAttribute("keystore", user.getKeystore().getFileName());
        return "user/users/show";
    }
    
    @RequestMapping(params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User authUser = userService.getUserFromContext(auth.getName());
		User user;
		if(User.countFindUsersByEppnEquals(authUser.getEppn()) > 0) {
			user = User.findUsersByEppnEquals(authUser.getEppn()).getSingleResult();
			user.setEmail(authUser.getEmail());
	        uiModel.addAttribute("user", user);
		} else {
			user = authUser;
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
		User authUser = userService.getUserFromContext(auth.getName());
        if(!user.getPublicKey().isEmpty()) {
            File file = userKeystoreService.createKeystore(user.getEppn(), user.getEppn(), user.getPublicKey(), user.getPassword());
            if(authUser.getKeystore().getBigFile().getBinaryFile() != null) {
            	authUser.getKeystore().remove();
            }
            authUser.setKeystore(fileService.addFile(file, "application/jks"));
        }
        if(!user.getSignImageBase64().isEmpty()) {
        	if(authUser.getSignImage().getBigFile().getBinaryFile() != null) {
        		authUser.getSignImage().remove();
        	}
        	authUser.setSignImage(fileService.addFile(user.getSignImageBase64(), authUser.getEppn() + "_sign", "application/png"));
        }
        if(authUser.getId() == null && !user.getSignImageBase64().isEmpty()) {
        	authUser.persist();
        } else {
        	redirectAttrs.addFlashAttribute("messageCustom", "image is required");
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
