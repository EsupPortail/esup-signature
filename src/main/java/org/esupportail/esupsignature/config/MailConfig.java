package org.esupportail.esupsignature.config;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
@ConfigurationProperties(prefix="mail")
public class MailConfig {

	private String senderHost;
	private String senderFrom;

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

    @Bean
    public JavaMailSender mailSender() throws IOException {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(senderHost);
        Properties javaMailProperties = new Properties();
        mailSender.setJavaMailProperties(javaMailProperties);
        return mailSender;

    }
	
}
