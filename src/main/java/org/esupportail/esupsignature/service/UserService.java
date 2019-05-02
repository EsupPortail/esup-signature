package org.esupportail.esupsignature.service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignBook.SignBookType;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.domain.User.EmailAlertFrequency;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.ldap.PersonLdapDao;
import org.esupportail.esupsignature.service.mail.MailSenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

	@Autowired(required = false)
	PersonLdapDao personDao;
    
	@Resource
	private DocumentService documentService;

	@Resource
	private SignBookService signBookService;

	@Resource
	private SignRequestService signRequestService;

	@Resource
	private MailSenderService mailSenderService;
	
	@Resource
	private Template emailTemplate;
	
	@Value("${root.url}")
	private String rootUrl;
	
	public boolean isUserReady(User user) {
		return user.isReady();
	}
	
	public SignBook createUser(String email) {
		List<PersonLdap> persons =  personDao.getPersonLdaps("mail", email);
		String eppn = persons.get(0).getEduPersonPrincipalName();
        String name = persons.get(0).getSn();
        String firstName = persons.get(0).getGivenName();
        return createUser(eppn, name, firstName, email);
	}
	
	public SignBook createUser(Authentication authentication) {
		List<PersonLdap> persons =  personDao.getPersonNamesByUid(authentication.getName());
		String eppn = persons.get(0).getEduPersonPrincipalName();
        String email = persons.get(0).getMail();
        String name = persons.get(0).getSn();
        String firstName = persons.get(0).getGivenName();
        return createUser(eppn, name, firstName, email);
	}
	
	public SignBook createUser(String eppn, String name, String firstName, String email) {
		User user;
		if(User.countFindUsersByEppnEquals(eppn) > 0) {
    		user = User.findUsersByEppnEquals(eppn).getSingleResult();
    	} else {
	    	user = new User();
			user.setSignImage(null);
			user.setKeystore(null);
			user.setEmailAlertFrequency(EmailAlertFrequency.never);
    	}
		user.setName(name);
		user.setFirstname(firstName);
		user.setEppn(eppn);
		user.setEmail(email);
		if(user.getId() == null) {
			user.persist();
		} else {
			user.merge();
		}
		List<String> recipientEmails = new ArrayList<>();
		recipientEmails.add(user.getEmail());
		if(SignBook.countFindSignBooksByRecipientEmailsAndSignBookTypeEquals(recipientEmails, SignBookType.user) == 0) {
			return signBookService.createUserSignBook(user);
		} else {
			return SignBook.findSignBooksByRecipientEmailsAndSignBookTypeEquals(recipientEmails, SignBookType.user).getSingleResult();
		}
	}
	
	public boolean checkEmailAlert(User user) {
		Date date = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		long diffInMillies = Math.abs(date.getTime() - user.getLastSendAlertDate().getTime());
		long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
		if((user.getEmailAlertFrequency() == null && diff > 0)
		|| (user.getEmailAlertFrequency().equals(EmailAlertFrequency.daily) && diff > 0)
		|| (user.getEmailAlertFrequency().equals(EmailAlertFrequency.weekly) && diff > 7)) {
			return true;
		}
		return false;
	}

	public void sendEmailAlert(User user) {
		Date date = new Date();
		List<SignRequest> signRequests = signRequestService.findSignRequestByUserAndStatusEquals(user, SignRequestStatus.pending);
		if(signRequests.size() > 0) {
			String[] to = {user.getEmail()};
			VelocityContext context = new VelocityContext();
	        StringWriter writer = new StringWriter();
	        context.put("signRequests", signRequests);
	        context.put("rootUrl", rootUrl);
	        emailTemplate.merge(context, writer);
			mailSenderService.sendMail(to, "Alert esup-signature", writer.toString(), null);
		}
		user.setLastSendAlertDate(date);
		user.merge();
	}
	
    public User getUserFromAuthentication() {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	String eppn = auth.getName();
    	if(personDao != null) {
    		List<PersonLdap> persons =  personDao.getPersonNamesByUid(auth.getName());
    		if(persons.size() > 0) {
    			eppn = persons.get(0).getEduPersonPrincipalName();
    		}
    	}
		if (User.countFindUsersByEppnEquals(eppn) > 0) {
			return User.findUsersByEppnEquals(eppn).getSingleResult();
		} else {
			return null;
		}
    	
    }
	
    public User addSignImage(User user, String signImageBase64) throws IOException {
    	user.setSignImage(documentService.createDocument(user.getSignImageBase64(), user.getEppn() + "_sign", "application/png"));
    	user.merge();
    	return user;
    	
    }
    
    public User addKeystore(User user) {
    	
    	return user;
    	
    }
}
