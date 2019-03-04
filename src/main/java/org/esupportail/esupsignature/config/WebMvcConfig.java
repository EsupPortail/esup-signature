package org.esupportail.esupsignature.config;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.esupportail.esupsignature.web.ApplicationConversionServiceFactoryBean;
import org.esupportail.esupsignature.web.LoggingExceptionResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.CacheControl;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.ui.context.support.ResourceBundleThemeSource;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.theme.CookieThemeResolver;
import org.springframework.web.servlet.theme.ThemeChangeInterceptor;
import org.springframework.web.servlet.view.UrlBasedViewResolver;
import org.springframework.web.servlet.view.tiles3.TilesConfigurer;
import org.springframework.web.servlet.view.tiles3.TilesView;

@Configuration
@EnableWebMvc
@EnableScheduling
@ComponentScan(basePackages = {"org.esupportail.esupsignature"}, includeFilters= {
		@ComponentScan.Filter(type=FilterType.ANNOTATION, classes={org.springframework.stereotype.Controller.class})
		})
public class WebMvcConfig implements WebMvcConfigurer {

	@Override
	  public void addViewControllers(ViewControllerRegistry registry) {
	    registry.addViewController("/").setViewName("index");
	    registry.addViewController("/uncaughtException");
	    registry.addViewController("/resourceNotFound");
	    registry.addViewController("/dataAccessFailure");
	  }
	
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
    	LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
    	localeChangeInterceptor.setParamName("lang");
        registry.addInterceptor(localeChangeInterceptor);
        registry.addInterceptor(new ThemeChangeInterceptor());
    }
	
	@Override
	public void addResourceHandlers(final ResourceHandlerRegistry registry) {
	    registry.addResourceHandler("/resources/**").addResourceLocations("/", "classpath:/META-INF/web-resources/").setCacheControl(CacheControl.maxAge(3600, TimeUnit.SECONDS));
	    
	}
	
	@Bean
	public ApplicationConversionServiceFactoryBean applicationConversionService() {
		return new ApplicationConversionServiceFactoryBean();
	}
	
	@Bean
	public TilesConfigurer tilesConfigurer(){
		TilesConfigurer tilesConfigurer = new TilesConfigurer();
		tilesConfigurer.setDefinitions("/WEB-INF/layouts/layouts.xml", "/WEB-INF/views/**/views.xml");
		return tilesConfigurer;
	}

	@Bean
	public UrlBasedViewResolver tilesViewResolver() {
		UrlBasedViewResolver basedViewResolver = new UrlBasedViewResolver();
		basedViewResolver.setViewClass(TilesView.class);
		return basedViewResolver;
	}
	
	@Bean
	public CommonsMultipartResolver multipartResolver() {
		CommonsMultipartResolver commonsMultipartResolver = new CommonsMultipartResolver();
		commonsMultipartResolver.setMaxInMemorySize(52428800);
		commonsMultipartResolver.setMaxUploadSize(52428800);
		return commonsMultipartResolver;
	}

	@Bean
	public LoggingExceptionResolver uncaughtException() {
		LoggingExceptionResolver exceptionResolver = new LoggingExceptionResolver();
		Properties properties = new Properties();
		properties.setProperty(".DataAccessException", "dataAccessFailure");
		properties.setProperty(".NoSuchRequestHandlingMethodException", "resourceNotFound");
		properties.setProperty(".TypeMismatchException", "resourceNotFound");
		properties.setProperty(".MissingServletRequestParameterException", "resourceNotFound");
		exceptionResolver.setExceptionMappings(properties);
		return exceptionResolver;
	}

	@Bean
	public CookieThemeResolver themeResolver() {
		CookieThemeResolver cookieThemeResolver = new CookieThemeResolver();
		cookieThemeResolver.setCookieName("theme");
		cookieThemeResolver.setDefaultThemeName("standard");
		return cookieThemeResolver;
	}
	
	@Bean
	public ResourceBundleThemeSource themeSource() {
		return new ResourceBundleThemeSource();
	}

	@Bean
	public ReloadableResourceBundleMessageSource messageSource() {
		ReloadableResourceBundleMessageSource bundleMessageSource = new ReloadableResourceBundleMessageSource();
		bundleMessageSource.setBasenames("WEB-INF/i18n/messages", "WEB-INF/i18n/application");
		bundleMessageSource.setFallbackToSystemLocale(false);
		return bundleMessageSource;
	}
	
}
