package org.esupportail.esupsignature.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.ldap.repository.config.EnableLdapRepositories;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.thymeleaf.dialect.springdata.SpringDataDialect;
import org.thymeleaf.extras.springsecurity5.dialect.SpringSecurityDialect;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

@Configuration 
@ComponentScan
@EnableWebMvc
@EnableAutoConfiguration
@EnableConfigurationProperties
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
                "/js/**")
                .addResourceLocations(
						"classpath:/static/images/favicon.ico",
						"classpath:/static/downloads/",
                        "classpath:/META-INF/resources/webjars/",
                        "classpath:/static/images/",
                        "classpath:/static/css/",
						"classpath:/static/doc/",
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
	public CorsFilter corsFilter() {
	    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	    final CorsConfiguration config = new CorsConfiguration();
	    config.setAllowCredentials(true);
	    config.setAllowedOrigins(Collections.singletonList("*"));
	    config.setAllowedHeaders(Arrays.asList("Origin", "Content-Type", "Accept"));
	    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "OPTIONS", "DELETE", "PATCH"));
	    source.registerCorsConfiguration("/**", config);
	    return new CorsFilter(source);
	}

//	@Bean
//	public LocaleResolver localeResolver() {
//		SessionLocaleResolver slr = new SessionLocaleResolver();
//		slr.setDefaultLocale(Locale.FRENCH);
//		return slr;
//	}
}
