package org.esupportail.esupsignature.config;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.dialect.springdata.SpringDataDialect;
import org.thymeleaf.extras.springsecurity5.dialect.SpringSecurityDialect;

@Configuration 
@ComponentScan
@EnableWebMvc
@EnableAsync
@EnableAutoConfiguration
@EnableConfigurationProperties
@OpenAPIDefinition(info = @Info(
		description = "Esup Signature REST API",
		version = "V1.11.2",
		title = "Esup Signature REST API",
		contact = @Contact(
				name = "David Lemaignent",
				email = "david.lemaignent@univ-rouen.fr",
				url = "https://www.esup-portail.org/wiki/display/SIGN/Accueil"
		),
		license = @License(
				name = "Apache 2.0",
				url = "http://www.apache.org/licenses/LICENSE-2.0"
		)
) , externalDocs = @ExternalDocumentation(url = "https://www.esup-portail.org/wiki/display/SIGN/Web+services+REST") )
public class WebAppConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    	registry.addResourceHandler(
    			"/favicon.ico",
				"/downloads/**",
                "/webjars/**",
                "/images/**",
                "/css/**",
				"/doc/**",
                "/js/**",
				"/static/js/**")
                .addResourceLocations(
						"classpath:/static/images/favicon.ico",
						"classpath:/static/downloads/",
                        "classpath:/META-INF/resources/webjars/",
                        "classpath:/static/images/",
                        "classpath:/static/css/",
						"classpath:/static/doc/",
                        "classpath:/static/js/",
						"classpath:/static/js/");
		registry.addResourceHandler("swagger-ui.html")
				.addResourceLocations("classpath:/META-INF/resources/");
    }


    @Bean
    public HiddenHttpMethodFilter hiddenHttpMethodFilter() {
        return new HiddenHttpMethodFilter();
    }

    @Bean
    public SpringDataDialect springDataDialect() {
        return new SpringDataDialect();
    }
    
    @Bean
    public SpringSecurityDialect springSecurityDialect() {
        return new SpringSecurityDialect();
    }
    
	@Bean
	public CommonsMultipartResolver multipartResolver() {
		CommonsMultipartResolver commonsMultipartResolver = new CommonsMultipartResolver();
		commonsMultipartResolver.setMaxInMemorySize(52428800);
		commonsMultipartResolver.setMaxUploadSize(52428800);
		return commonsMultipartResolver;
	}

	@Bean
	public FilterRegistrationBean registerOpenEntityManagerInViewFilterBean() {
		FilterRegistrationBean registrationBean = new FilterRegistrationBean();
		OpenEntityManagerInViewFilter filter = new OpenEntityManagerInViewFilter();
		registrationBean.setFilter(filter);
		registrationBean.setOrder(5);
		registrationBean.addUrlPatterns(
				"/user/", "/user/*",
				"/admin/", "/admin/*",
				"/public/", "/public/*",
				"/ws/", "/ws/*"
		);
		return registrationBean;
	}

}