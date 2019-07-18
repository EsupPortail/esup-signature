package org.esupportail.esupsignature.config;

import java.util.Properties;

import org.esupportail.esupsignature.service.fs.cifs.CifsAccessImpl;
import org.esupportail.esupsignature.service.fs.opencmis.CmisAccessImpl;
import org.esupportail.esupsignature.service.fs.vfs.VfsAccessImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@Configuration
public class FsConfig {

	@Value("${fs.cifs.uri}")
	private String cifsUri;
	@Value("${fs.cifs.login}")
	private String cifsLogin;
	@Value("${fs.cifs.password}")
	private String cifsPassword;

	@Value("${fs.vfs.uri}")
	private String vfsUri;

	@Value("${fs.cmis.uri}")
	private String cmisUri;
	@Value("${fs.cmis.login}")
	private String cmisLogin;
	@Value("${fs.cmis.password}")
	private String cmisPassword;
	@Value("${fs.cmis.respositoryId}")
	private String cmisRespositoryId;
	@Value("${fs.cmis.rootPath}")
	private String cmisRootPath;
	
	@Bean
	public CifsAccessImpl cifsAccessImpl(){
		CifsAccessImpl cifsAccessImpl = new CifsAccessImpl();
		cifsAccessImpl.setDriveName("CIFS");
		cifsAccessImpl.setUri(cifsUri);
		cifsAccessImpl.setJcifsConfigProperties(cifsProperties());
		cifsAccessImpl.setLogin(cifsLogin);
		cifsAccessImpl.setPassword(cifsPassword);
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
	
	@Bean
	public VfsAccessImpl vfsAccessImpl(){
		VfsAccessImpl vfsAccessImpl = new VfsAccessImpl();
		vfsAccessImpl.setDriveName("VFS");
		vfsAccessImpl.setUri(vfsUri);
		//vfsAccessImpl.setLogin(login);
		//vfsAccessImpl.setPassword(password);
		return vfsAccessImpl;
	}

	@Bean
	public CmisAccessImpl cmisAccessImpl(){
		CmisAccessImpl cmisAccessImpl = new CmisAccessImpl();
		cmisAccessImpl.setDriveName("CMIS");
		cmisAccessImpl.setUri(cmisUri);
		cmisAccessImpl.setLogin(cmisLogin);
		cmisAccessImpl.setPassword(cmisPassword);
		cmisAccessImpl.setRespositoryId(cmisRespositoryId);
		cmisAccessImpl.setRootPath(cmisRootPath);
		return cmisAccessImpl;
	}
	
}
