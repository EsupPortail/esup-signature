package org.esupportail.esupsignature.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@PropertySources({@PropertySource("classpath:META-INF/spring/custom.properties"), @PropertySource("classpath:META-INF/spring/database.properties"), @PropertySource("classpath:META-INF/spring/fs.properties"), @PropertySource("classpath:META-INF/spring/mail.properties"), @PropertySource("classpath:META-INF/spring/security.properties"), @PropertySource("classpath:META-INF/spring/dss.properties")})
public class PropertiesConfig {

	public PropertiesConfig() {
		super();
	}

	// static because return type is an instance of BeanFactoryPostProcessor (see
	// javadoc of @Bean)
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

}