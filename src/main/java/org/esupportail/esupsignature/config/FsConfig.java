package org.esupportail.esupsignature.config;

import java.util.Properties;

import org.esupportail.esupsignature.service.fs.cifs.CifsAccessImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FsConfig {

	@Bean
	public CifsAccessImpl cifsAccessImpl(){
		CifsAccessImpl cifsAccessImpl = new CifsAccessImpl();
		cifsAccessImpl.setDriveName("CIFS");
		cifsAccessImpl.setUri("smb://sambin.ur/dgs/dsi/");
		cifsAccessImpl.setJcifsConfigProperties(cifsProperties());
		return cifsAccessImpl;
	}

	@Bean
	public Properties cifsProperties(){
		Properties properties = new Properties();
		properties.put("jcifs.resolveOrder", "DNS,BCAST");
		properties.put("jcifs.encoding", "UTF8");
		properties.put("jcifs.smb.client.disablePlainTextPasswords", "true");
		properties.put("jcifs.smb.client.responseTimeout", "40000");
		return properties;
	
	}
}
