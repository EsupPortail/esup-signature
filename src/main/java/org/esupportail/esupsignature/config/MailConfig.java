package org.esupportail.esupsignature.config;

import org.esupportail.esupsignature.mail.SimpleMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableSpringConfigured
@EnableTransactionManagement(mode=AdviceMode.ASPECTJ)
@EnableJpaRepositories(basePackages = "org.esupportail.esupsignature.domain")
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
