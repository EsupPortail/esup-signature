package org.esupportail.esupsignature.config;

import java.util.Properties;

import org.esupportail.esupsignature.service.fs.cifs.CifsAccessImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FsConfig {

	@Value("${fs.cifs.uri}")
	private String uri;
	@Value("${fs.cifs.login}")
	private String login;
	@Value("${fs.cifs.password}")
	private String password;
	
	@Bean
	public CifsAccessImpl cifsAccessImpl(){
		CifsAccessImpl cifsAccessImpl = new CifsAccessImpl();
		cifsAccessImpl.setDriveName("CIFS");
		cifsAccessImpl.setUri(uri);
		cifsAccessImpl.setJcifsConfigProperties(cifsProperties());
		cifsAccessImpl.setLogin(login);
		cifsAccessImpl.setPassword(password);
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
