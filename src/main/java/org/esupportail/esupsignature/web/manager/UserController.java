package org.esupportail.esupsignature.web.manager;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.esupportail.esupsignature.domain.File;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.UserKeystoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/manager/users")
@Controller
@RooWebScaffold(path = "manager/users", formBackingObject = User.class)
@Transactional
public class UserController {
	
	private static final Logger log = LoggerFactory.getLogger(UserController.class);
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return "users";
	}
	@Resource
	FileService fileService;

	@Resource
	UserKeystoreService userKeystoreService;
	
    @RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(@Valid User user, @RequestParam("multipartFile") MultipartFile multipartFile, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, user);
            return "manager/users/create";
        }
        uiModel.asMap().clear();
        try {
			user.setSignImage(fileService.addFile(multipartFile));
	        java.io.File file = userKeystoreService.createKeystore(user.getEppn(), user.getPublicKey(), "password");
	        user.setKeystore(fileService.addFile(new FileInputStream(file), file.getName(), file.length(), "application/jks"));
	        user.persist();
        } catch (Exception e) {
        	log.error("Create user error", e);
		}
        return "redirect:/manager/users/" + encodeUrlPathSegment(user.getId().toString(), httpServletRequest);
    }
    
    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) throws Exception {
    	User user = User.findUser(id);
        uiModel.addAttribute("user", user);
        File signFile = user.getSignImage();
        uiModel.addAttribute("signFile", fileService.getBase64Image(signFile));
        uiModel.addAttribute("keystore", user.getKeystore().getFileName());
        uiModel.addAttribute("itemId", id);
        return "manager/users/show";
    }
    
    @RequestMapping(method = RequestMethod.PUT, produces = "text/html")
    public String update(@Valid User user, BindingResult bindingResult, @RequestParam("multipartFile") MultipartFile multipartFile, Model uiModel, HttpServletRequest httpServletRequest) throws Exception {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, user);
            return "manager/users/update";
        }
        uiModel.asMap().clear();
        User userToUdate = User.findUser(user.getId());
        java.io.File file = userKeystoreService.createKeystore(user.getEppn(), user.getPublicKey(), "password");
        InputStream inputStream = new FileInputStream(file);
        userToUdate.setKeystore(fileService.addFile(inputStream, file.getName(), file.length(), "application/jks"));
        userToUdate.merge();
        return "redirect:/manager/users/" + encodeUrlPathSegment(user.getId().toString(), httpServletRequest);
    }
    
}
