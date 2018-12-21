package eu.europa.esig.dss.web;

import java.util.Collections;

import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionTrackingMode;

import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import eu.europa.esig.dss.web.config.DSSBeanConfig;
// import eu.europa.esig.dss.web.config.WebConfig;

public class AppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

	@Value("${cookie.secure}")
	private boolean cookieSecure;

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {

		super.onStartup(servletContext);

		CXFServlet cxf = new CXFServlet();
		BusFactory.setDefaultBus(cxf.getBus());
		ServletRegistration.Dynamic cxfServlet = servletContext.addServlet("CXFServlet", cxf);
		cxfServlet.setLoadOnStartup(1);
		cxfServlet.addMapping("/services/*");

		servletContext.getSessionCookieConfig().setSecure(cookieSecure);

		// avoid urls with jsessionid param
		servletContext.setSessionTrackingModes(Collections.singleton(SessionTrackingMode.COOKIE));
	}

	@Override
	protected Class<?>[] getRootConfigClasses() {
		return new Class[] { DSSBeanConfig.class };
	}

	@Override
	protected Class<?>[] getServletConfigClasses() {
		
		return null;
	}

	@Override
	protected String[] getServletMappings() {
		return new String[] { "/*" };
	}

	@Override
	protected Filter[] getServletFilters() {
		DelegatingFilterProxy springSecurityFilterChainProxy = new DelegatingFilterProxy();
		springSecurityFilterChainProxy.setTargetBeanName("springSecurityFilterChain");
		return new Filter[] { new CharacterEncodingFilter("UTF-8"), new OpenEntityManagerInViewFilter(), springSecurityFilterChainProxy };
	}

}
