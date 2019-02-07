package org.esupportail.esupsignature.web.user;

import java.io.IOException;
import java.sql.SQLException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.io.IOUtils;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RequestMapping("/user/users")
@Controller
@Scope(value="session")
@Transactional
@EnableScheduling
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
	
	private String password = "";
	long startTime;
	
	public void setPassword(String password) {
		startTime = System.currentTimeMillis();
		this.password = password;
	}
	
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
    public String createForm(Model uiModel) throws IOException, SQLException {
		String eppn = userService.getEppnFromAuthentication();
		User user;
		if(User.countFindUsersByEppnEquals(eppn) > 0) {
			user = User.findUsersByEppnEquals(eppn).getSingleResult();
	        uiModel.addAttribute("user", user);
	        if(user.getSignImage().getBigFile().getBinaryFile() != null) {
	        	uiModel.addAttribute("signFile", fileService.getBase64Image(user.getSignImage()));
	        }
		} else {
			user = new User();
		}
        uiModel.addAttribute("user", user);
		return "user/users/update";
    }
    
    @RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(@Valid User user, @RequestParam(value = "multipartKeystore", required=false) MultipartFile multipartKeystore, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttrs) throws Exception {
    	if (bindingResult.hasErrors()) {
        	populateEditForm(uiModel, user);
            return "user/users/update";
        }
        uiModel.asMap().clear();
		String eppn = userService.getEppnFromAuthentication();
		User userToUpdate = null;
		userToUpdate = User.findUsersByEppnEquals(eppn).getSingleResult();
        if(!multipartKeystore.isEmpty()) {
            if(userToUpdate.getKeystore().getBigFile().getBinaryFile() != null) {
            	userToUpdate.getKeystore().remove();
            }
            userToUpdate.setKeystore(documentService.addFile(multipartKeystore, multipartKeystore.getOriginalFilename()));
        }
        Document oldSignImage = userToUpdate.getSignImage();
        if(!user.getSignImageBase64().isEmpty()) {
        	userToUpdate.setSignImage(documentService.addFile(user.getSignImageBase64(), userToUpdate.getEppn() + "_sign", "application/png"));
        } else
        	if(userToUpdate.getSignImage().getBigFile().getBinaryFile() == null) {
            	redirectAttrs.addFlashAttribute("messageCustom", "image is required");
        }
        if(oldSignImage.getBigFile().getBinaryFile() != null) {
        	oldSignImage.getBigFile().getBinaryFile().free();
    		oldSignImage.getBigFile().remove();
    		oldSignImage.remove();
    	}
        return "redirect:/user/users/";
    }
    
    @RequestMapping(value = "/viewCert", method = RequestMethod.GET, produces = "text/html")
    public String viewCert(@RequestParam("password") String password, RedirectAttributes redirectAttrs) throws Exception {
		String eppn = userService.getEppnFromAuthentication();
		User user = User.findUsersByEppnEquals(eppn).getSingleResult();
		if (!"".equals(password)) {
        	setPassword(password);
        }
        try {
        	redirectAttrs.addFlashAttribute("messageCustom", userKeystoreService.checkKeystore(user.getKeystore().getBigFile().toJavaIoFile(), this.password));
        } catch (Exception e) {
        	log.error("open keystore fail", e);
        	redirectAttrs.addFlashAttribute("messageCustom", "bad password");
		}
        return "redirect:/user/users/";
    }
    
	@RequestMapping(value = "/get-keystore-file", method = RequestMethod.GET)
	public void getSignedFile(HttpServletResponse response, Model model) {
		String eppn = userService.getEppnFromAuthentication();
		User user = User.findUsersByEppnEquals(eppn).getSingleResult();
		
		Document file = user.getKeystore();
		try {
			response.setHeader("Content-Disposition", "inline;filename=\"" + file.getFileName() + "\"");
			response.setContentType(file.getContentType());
			IOUtils.copy(file.getBigFile().getBinaryFile().getBinaryStream(), response.getOutputStream());
		} catch (Exception e) {
			log.error("get file error", e);
		}
	}
    
    void populateEditForm(Model uiModel, User user) {
        uiModel.addAttribute("user", user);
        uiModel.addAttribute("files", Document.findAllDocuments());
    }

	@Scheduled(fixedDelay = 5000)
	public void clearPassword () {
		if(startTime > 0) {
			if(System.currentTimeMillis() - startTime > 60000) {
				password = "";
				startTime = 0;
			}
		}
	}
	
}
