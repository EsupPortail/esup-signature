package org.esupportail.esupsignature.web.user;

import java.io.File;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.ldap.PersonLdapDao;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.UserKeystoreService;
import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
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
public class UserController {

	private static final Logger log = LoggerFactory.getLogger(UserController.class);
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return "user/users";
	}
	
	@Autowired(required = false)
	PersonLdapDao personDao;

	@Resource
	private DocumentService documentService;
	
	@Resource
	private FileService fileService;

	@Resource
	private UserKeystoreService userKeystoreService;
	
	@Resource
	private UserService userService;
    
    @RequestMapping(produces = "text/html")
    public String show(Model uiModel) throws Exception {
		String eppn = userService.getEppnFromAuthentication();
		if(User.countFindUsersByEppnEquals(eppn) == 0) {
			return "redirect:/user/users/?form";
		}
    	User user = User.findUsersByEppnEquals(eppn).getSingleResult();
    	if(user.getSignImage().getBigFile().getBinaryFile() == null) {
			return "redirect:/user/users/?form";
		}    	
    	populateEditForm(uiModel, user);
        if(user.getSignImage().getBigFile().getBinaryFile() != null) {
        	uiModel.addAttribute("signFile", fileService.getBase64Image(user.getSignImage()));
        }
        uiModel.addAttribute("keystore", user.getKeystore().getFileName());
        return "user/users/show";
    }
    
    @RequestMapping(params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
		String eppn = userService.getEppnFromAuthentication();
		User user;
		if(User.countFindUsersByEppnEquals(eppn) > 0) {
			user = User.findUsersByEppnEquals(eppn).getSingleResult();
	        uiModel.addAttribute("user", user);
		} else {
			user = new User();
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
		String eppn = userService.getEppnFromAuthentication();
		User userToUpdate = User.findUsersByEppnEquals(eppn).getSingleResult();
        if(!user.getPublicKey().isEmpty()) {
            File file = userKeystoreService.createKeystore(user.getEppn(), user.getEppn(), user.getPublicKey(), user.getPassword());
            if(userToUpdate.getKeystore().getBigFile().getBinaryFile() != null) {
            	userToUpdate.getKeystore().remove();
            }
            userToUpdate.setKeystore(documentService.addFile(file, file.getName(), "application/jks"));
        }
        if(!user.getSignImageBase64().isEmpty()) {
        	if(userToUpdate.getSignImage().getBigFile().getBinaryFile() != null) {
        		userToUpdate.getSignImage().remove();
        	}
        	userToUpdate.setSignImage(documentService.addFile(user.getSignImageBase64(), userToUpdate.getEppn() + "_sign", "application/png"));
        } else
        	if(userToUpdate.getId() == null) {
            	redirectAttrs.addFlashAttribute("messageCustom", "image is required");
        }
        if(userToUpdate.getId() == null) {
        	userToUpdate.persist();
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
