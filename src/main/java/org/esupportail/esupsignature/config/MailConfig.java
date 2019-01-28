package org.esupportail.esupsignature.config;

import org.esupportail.esupsignature.mail.SimpleMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfig {

	@Value("${mail.senderHost}")
	private String senderHost;
	@Value("${mail.senderFrom}")
	private String senderFrom;

	@Bean
	public SimpleMailSender simpleMailSender() {
		return new SimpleMailSender(senderHost, senderFrom);
	}
	
}
