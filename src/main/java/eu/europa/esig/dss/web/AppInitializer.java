package eu.europa.esig.dss.web;

import java.util.Collections;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionTrackingMode;

import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.servlet.DispatcherServlet;

import eu.europa.esig.dss.web.config.DSSBeanConfig;


public class AppInitializer implements WebApplicationInitializer {

	@Value("${cookie.secure}")
	private boolean cookieSecure;

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
        
		AnnotationConfigWebApplicationContext rootAppContext = new AnnotationConfigWebApplicationContext();
		rootAppContext.register(DSSBeanConfig.class);
		ContextLoaderListener listener = new ContextLoaderListener(rootAppContext);
		servletContext.addListener(listener);
		
		servletContext.addFilter("CharacterEncodingFilter", new CharacterEncodingFilter("UTF-8", true)).addMappingForUrlPatterns(null, false, "/*");
		servletContext.addFilter("HttpMethodFilter", new HiddenHttpMethodFilter());
		servletContext.addFilter("Spring OpenEntityManagerInViewFilter", new OpenEntityManagerInViewFilter()).addMappingForUrlPatterns(null, false, "/*");
		servletContext.addFilter("springSecurityFilterChain", new DelegatingFilterProxy()).addMappingForUrlPatterns(null, false, "/*");
		
		XmlWebApplicationContext appContext = new XmlWebApplicationContext();
		appContext.setConfigLocation("WEB-INF/spring/webmvc-config.xml");
		ServletRegistration.Dynamic dispatcher = servletContext.addServlet("esup-signature", new DispatcherServlet(appContext));
		dispatcher.setLoadOnStartup(1);
		dispatcher.addMapping("/");

		CXFServlet cxf = new CXFServlet();
		BusFactory.setDefaultBus(cxf.getBus());
		ServletRegistration.Dynamic cxfServlet = servletContext.addServlet("CXFServlet", cxf);
		cxfServlet.setLoadOnStartup(1);
		cxfServlet.addMapping("/services/*");
		
		servletContext.getSessionCookieConfig().setSecure(cookieSecure);
		// avoid urls with jsessionid param
		servletContext.setSessionTrackingModes(Collections.singleton(SessionTrackingMode.COOKIE));
	}

}
