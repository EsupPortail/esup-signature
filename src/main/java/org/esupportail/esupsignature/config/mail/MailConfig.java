package org.esupportail.esupsignature.config.mail;

import org.esupportail.esupsignature.service.mail.MailService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

	private MailProperties mailProperties;

    public MailConfig(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    public String getMailFrom() {
        return this.mailProperties.getFrom();
    }

}
