package org.esupportail.esupsignature.config;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

	@Value("${mail.senderHost}")
	private String senderHost;
	@Value("${mail.senderFrom}")
	private String senderFrom;
	
	@Bean
    public JavaMailSender mailSender() throws IOException {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(senderHost);
        Properties javaMailProperties = new Properties();
        mailSender.setJavaMailProperties(javaMailProperties);
        return mailSender;

    }
	
}
