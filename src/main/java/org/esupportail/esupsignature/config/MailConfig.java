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
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@Configuration
@PropertySources(value = {@PropertySource("classpath:mail.properties")})
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
		velocityEngine.setProperty("runtime.log", "/tmp/velocity.log");
		velocityEngine.init();
        return velocityEngine.getTemplate("templates/emailTemplate.html", "UTF-8");
    }
	
}
