package org.esupportail.esupsignature.service.mail;

import java.io.File;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

public class MailSenderService {

	private final Logger log = Logger.getLogger(getClass());

	private JavaMailSenderImpl mailSender;
	private SimpleMailMessage templateMessage;
	private String senderHost;
	private String senderFrom;

	
	
	public MailSenderService(String senderHost, String senderFrom) {
		this.senderHost = senderHost;
		this.senderFrom = senderFrom;
	}

	public void sendMail(String[] to, String subject, String text, File file) {
		mailSender = new JavaMailSenderImpl();
		mailSender.setHost(senderHost);

		templateMessage = new SimpleMailMessage();

		templateMessage.setFrom(senderFrom);

		try {
			MimeMessage message = mailSender.createMimeMessage();
			message.setSubject(subject);
			MimeMessageHelper helper;
			helper = new MimeMessageHelper(message, true, "utf8");
			helper.setFrom(senderFrom);
			helper.setTo(to);
			helper.setText(text, true);
			if (file != null) {
				helper.addAttachment(file.getName(), file);
			}
			mailSender.send(message);
			log.info("envoi du mail OK");
		} catch (MessagingException e) {
			log.warn("Erreur lors de l'envoi du mail", e);
		}
	}

	public String getSenderHost() {
		return senderHost;
	}

	public void setSenderHost(String senderHost) {
		this.senderHost = senderHost;
	}

	public String getSenderFrom() {
		return senderFrom;
	}

	public void setSenderFrom(String senderFrom) {
		this.senderFrom = senderFrom;
	}

}
