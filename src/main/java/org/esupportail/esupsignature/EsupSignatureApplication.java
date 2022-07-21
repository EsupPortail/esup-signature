package org.esupportail.esupsignature;

import org.esupportail.esupsignature.service.utils.upgrade.UpgradeService;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SessionTrackingMode;
import java.util.Collections;

@SpringBootApplication
public class EsupSignatureApplication extends SpringBootServletInitializer implements CommandLineRunner {

	@Resource
	private ApplicationContext applicationContext;

	@Resource
	private GlobalProperties globalProperties;

	@Resource
	private UpgradeService upgradeService;

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

	@Override
	public void run(String... args) throws Exception {
		boolean upgrage = globalProperties.getAutoUpgrade();
		boolean close = false;
		for(String arg : args) {
			if("upgrade".equals(arg)) {
				upgrage = true;
				close = true;
				break;
			}
		}
		if(upgrage) {
			upgradeService.launch();
			if(close) System.exit(SpringApplication.exit(applicationContext));
		}
	}

}
