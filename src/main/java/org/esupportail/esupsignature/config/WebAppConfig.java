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
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.dialect.springdata.SpringDataDialect;
import org.thymeleaf.extras.springsecurity5.dialect.SpringSecurityDialect;

@Configuration 
@ComponentScan
@EnableAsync
@EnableAutoConfiguration
@EnableConfigurationProperties(GlobalProperties.class)
public class WebAppConfig implements WebMvcConfigurer {

	private final GlobalProperties globalProperties;

	public WebAppConfig(GlobalProperties globalProperties) {
		this.globalProperties = globalProperties;
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
		commonsMultipartResolver.setMaxInMemorySize(globalProperties.getMaxUploadSize());
		commonsMultipartResolver.setMaxUploadSize(globalProperties.getMaxUploadSize());
		return commonsMultipartResolver;
	}

	@Bean
	public FilterRegistrationBean<OpenEntityManagerInViewFilter> registerOpenEntityManagerInViewFilterBean() {
		FilterRegistrationBean<OpenEntityManagerInViewFilter> registrationBean = new FilterRegistrationBean<>();
		OpenEntityManagerInViewFilter filter = new OpenEntityManagerInViewFilter();
		registrationBean.setFilter(filter);
		registrationBean.setOrder(5);
		registrationBean.addUrlPatterns(
				"/user/", "/user/*",
				"/otp/", "/otp/*",
				"/error",
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
						.contact(new Contact().name("David Lemaignent").email("esup-signature@esup-portail.org"))
						.license(new License().name("Apache 2.0").url("http://www.apache.org/licenses/LICENSE-2.0")))
				.externalDocs(new ExternalDocumentation()
						.description("Wiki Esup Signature")
						.url("https://www.esup-portail.org/wiki/display/SIGN/Accueil"));
	}

	@Bean
	HandlerExceptionResolver customExceptionResolver () {
		return new HandlerExceptionToViewResolver();
	}

}