package eu.europa.esig.dss.web;

import java.util.Collections;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionTrackingMode;

import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

import eu.europa.esig.dss.web.config.DSSBeanConfig;


@Import({DSSBeanConfig.class})
public class AppInitializer implements WebApplicationInitializer {

	@Value("${cookie.secure}")
	private boolean cookieSecure;

	@Override
	public void onStartup(ServletContext container) throws ServletException {

		XmlWebApplicationContext rootContext = new XmlWebApplicationContext();
        rootContext.setConfigLocation("/WEB-INF/web.xml");
        container.addListener(new ContextLoaderListener(rootContext));
        
		CXFServlet cxf = new CXFServlet();
		BusFactory.setDefaultBus(cxf.getBus());
		ServletRegistration.Dynamic cxfServlet = container.addServlet("CXFServlet", cxf);
		cxfServlet.setLoadOnStartup(1);
		cxfServlet.addMapping("/services/*");

		container.getSessionCookieConfig().setSecure(cookieSecure);

		// avoid urls with jsessionid param
		container.setSessionTrackingModes(Collections.singleton(SessionTrackingMode.COOKIE));

	}
	
}
