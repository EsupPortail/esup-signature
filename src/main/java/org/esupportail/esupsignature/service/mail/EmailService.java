package org.esupportail.esupsignature.service.mail;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.mail.internet.MimeMessage;

import org.esupportail.esupsignature.entity.SignRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

	private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
	
	@Autowired
	private JavaMailSender mailSender;
	
	@Value("${root.url}")
	private String rootUrl;
	
	@Autowired
	private TemplateEngine templateEngine;
	
	public void sendSignRequestAlert(final String recipientName, final String recipientEmail, List<SignRequest> signRequests) {

		    final Context ctx = new Context(Locale.FRENCH);
		    ctx.setVariable("name", recipientName);
		    ctx.setVariable("subscriptionDate", new Date());
		    ctx.setVariable("signRequests", signRequests);
		    ctx.setVariable("signRequests", signRequests);
		    ctx.setVariable("rootUrl", rootUrl);

		    final MimeMessage mimeMessage = mailSender.createMimeMessage();
		    MimeMessageHelper message;
			try {
				message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
			    message.setSubject("Alerte esup-signature");
			    message.setFrom("esup-signature@univ-rouen.fr");
			    message.setTo(recipientEmail);
			    final String htmlContent = templateEngine.process("mail/email-template.html", ctx);
			    message.setText(htmlContent, true); // true = isHtml
			    mailSender.send(mimeMessage);
			} catch (javax.mail.MessagingException e) {
				logger.error("enable to sens email", e);
			}

		}

}
