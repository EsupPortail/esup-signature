package org.esupportail.esupsignature;

import ch.rasc.sse.eventbus.config.EnableSseEventBus;
import com.twelvemonkeys.servlet.image.IIOProviderContextListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

@SpringBootApplication
@EnableSseEventBus
public class EsupSignatureApplication extends SpringBootServletInitializer {

	@Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(EsupSignatureApplication.class);
    }

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		super.onStartup(servletContext);
		servletContext.addListener(IIOProviderContextListener.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(EsupSignatureApplication.class, args);
	}
	
}
