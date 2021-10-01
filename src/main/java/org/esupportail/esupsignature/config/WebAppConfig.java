package org.esupportail.esupsignature.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
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

import javax.annotation.Resource;

@Configuration 
@ComponentScan
@EnableWebMvc
@EnableAsync
@EnableAutoConfiguration
@EnableConfigurationProperties
public class WebAppConfig implements WebMvcConfigurer {

	@Resource
	private GlobalProperties globalProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    	registry.addResourceHandler(
    			"/favicon.ico",
				"/downloads/**",
                "/webjars/**",
                "/images/**",
                "/css/**",
				"/fonts/**",
                "/js/**",
				"/static/js/**")
                .addResourceLocations(
						"classpath:/static/images/favicon.ico",
						"classpath:/static/downloads/",
                        "classpath:/META-INF/resources/webjars/",
                        "classpath:/static/images/",
                        "classpath:/static/css/",
						"classpath:/static/doc/",
						"classpath:/static/fonts/",
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
				"/ws-secure/", "/ws-secure/*",
				"/admin/", "/admin/*",
				"/manager/", "/manager/*",
				"/public/", "/public/*",
				"/ws/", "/ws/*"
		);
		return registrationBean;
	}

	@Bean
	public OpenAPI springShopOpenAPI() {
		return new OpenAPI()
				.info(new Info().title("Esup Signature")
						.description("Esup Signature REST API")
						.version(globalProperties.getVersion())
						.contact(new Contact().name("David Lemaignent, Valentin Hagnere").email("esup-signature@esup-portail.org"))
						.license(new License().name("Apache 2.0").url("http://www.apache.org/licenses/LICENSE-2.0")))
				.externalDocs(new ExternalDocumentation()
						.description("Wiki Esup Signature")
						.url("https://www.esup-portail.org/wiki/display/SIGN/Accueil"));
	}

}