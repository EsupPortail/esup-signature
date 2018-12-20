package eu.europa.esig.dss.web.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableWebMvc
@Import(PropertiesConfig.class)
@ComponentScan(basePackages = { "eu.europa.esig.dss.web.controller" })
public class WebConfig extends WebMvcConfigurerAdapter {

	
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/css/**").addResourceLocations("/css/");
		registry.addResourceHandler("/images/**").addResourceLocations("/images/");
		registry.addResourceHandler("/scripts/**").addResourceLocations("/scripts/");
		registry.addResourceHandler("/webjars/**").addResourceLocations("/webjars/");
		registry.addResourceHandler("/jar/**").addResourceLocations("/jar/");
		registry.addResourceHandler("/downloads/**").addResourceLocations("/downloads/");
		registry.addResourceHandler("/doc/**").addResourceLocations("/doc/");
		registry.addResourceHandler("/apidocs/**").addResourceLocations("/apidocs/");
	}

	@Bean
	public MultipartResolver multipartResolver() {
		CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
		multipartResolver.setMaxInMemorySize(52428800);
		multipartResolver.setMaxUploadSize(52428800);
		return multipartResolver;
	}
	@Bean
	public MessageSource messageSource() {
		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
		messageSource.setBasenames("classpath:i18n/application");
		return messageSource;
	}

}
