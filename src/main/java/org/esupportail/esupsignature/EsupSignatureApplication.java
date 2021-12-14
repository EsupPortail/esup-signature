package org.esupportail.esupsignature;

import ch.rasc.sse.eventbus.config.EnableSseEventBus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SessionTrackingMode;
import java.util.Collections;

@SpringBootApplication
@EnableSseEventBus
public class EsupSignatureApplication extends SpringBootServletInitializer {

	@Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(EsupSignatureApplication.class);
    }

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		servletContext.setSessionTrackingModes(Collections.singleton(SessionTrackingMode.COOKIE));
		super.onStartup(servletContext);
	}

	public static void main(String[] args) {
		SpringApplication.run(EsupSignatureApplication.class, args);
	}
	
}
