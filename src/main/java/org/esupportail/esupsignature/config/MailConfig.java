package org.esupportail.esupsignature.config;

import java.io.IOException;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.esupportail.esupsignature.service.mail.MailSenderService;
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
	public MailSenderService mailSenderService() {
		return new MailSenderService(senderHost, senderFrom);
	}
	
	@Bean
    public Template emailTemplate() throws VelocityException, IOException {
		VelocityEngine velocityEngine = new VelocityEngine();
		velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		velocityEngine.init();
        return velocityEngine.getTemplate("templates/emailTemplate.vm", "UTF-8");
    }
	
}
