package org.esupportail.esupsignature.service.mail;

import org.esupportail.esupsignature.config.mail.MailConfig;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.List;
import java.util.Locale;

@Service
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    private MailConfig mailConfig;
    private JavaMailSenderImpl mailSender;


    @Autowired(required = false)
    public void setMailConfig(MailConfig mailConfig) {
        this.mailConfig = mailConfig;
    }

    @Autowired(required = false)
    public void setMailConfig(JavaMailSenderImpl mailSender) {
        this.mailSender = mailSender;
    }

    @Resource
    private UserRepository userRepository;

    @Resource
    private TemplateEngine templateEngine;

    @Value("${root.url}")
    private String rootUrl;

    public void sendCompletedMail(SignRequest signRequest) {
        if(!checkMailSender()){
            return;
        }
        User user = userRepository.findByEppn(signRequest.getCreateBy()).get(0);
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("name", user.getFirstname() + " " + user.getName());
        ctx.setVariable("signRequest", signRequest);
        ctx.setVariable("rootUrl", rootUrl);

        final MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper message;
        try {
            message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            message.setSubject("Esup-Signature : demande signature complète");
            message.setFrom(mailConfig.getMailFrom());
            message.setTo(user.getEmail());
            String htmlContent = templateEngine.process("mail/email-completed.html", ctx);
            message.setText(htmlContent, true); // true = isHtml
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            logger.error("enable to sens email", e);
        }

    }

    public void sendSignRequestAlert(String recipientName, String recipientEmail, List<SignRequest> signRequests) {
        if(!checkMailSender()){
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("name", recipientName);
        ctx.setVariable("signRequests", signRequests);
        ctx.setVariable("rootUrl", rootUrl);

        final MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper message;
        try {
            message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            message.setSubject("Esup-Signature : nouveau document à signer");
            message.setFrom(mailConfig.getMailFrom());
            message.setTo(recipientEmail);
            String htmlContent = templateEngine.process("mail/email-alert.html", ctx);
            message.setText(htmlContent, true); // true = isHtml
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            logger.error("enable to sens email", e);
        }

    }

	public void sendFile(String recipientEmail, File file,  SignRequest signRequest) {
        if(!checkMailSender()){
            return;
        }
		final Context ctx = new Context(Locale.FRENCH);
		ctx.setVariable("rootUrl", rootUrl);
        ctx.setVariable("signRequest", signRequest);
        User user = userRepository.findByEppn(signRequest.getCreateBy()).get(0);
        ctx.setVariable("user", user);

		final MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper message;
		try {
			message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
			message.setSubject("Esup-Signature : nouveau document  " + signRequest.getTitle());
            message.setFrom(mailConfig.getMailFrom());
			message.setTo(recipientEmail);
			message.addAttachment(file.getName(), file);
			String htmlContent = templateEngine.process("mail/email-file.html", ctx);
			message.setText(htmlContent, true); // true = isHtml
            mailSender.send(mimeMessage);
		} catch (MessagingException e) {
			logger.error("enable to sens email", e);
		}
	}

    private boolean checkMailSender() {
        if(mailSender == null){
            logger.warn("message not sended : mail host not configured");
            return false;
        }
        return true;
    }

}
