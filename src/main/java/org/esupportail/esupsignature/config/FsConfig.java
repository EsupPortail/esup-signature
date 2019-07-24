package org.esupportail.esupsignature.config;

import java.util.Properties;

import org.esupportail.esupsignature.service.fs.opencmis.CmisAccessImpl;
import org.esupportail.esupsignature.service.fs.smb.SmbAccessImpl;
import org.esupportail.esupsignature.service.fs.vfs.VfsAccessImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FsConfig {

	@Value("${fs.smb.uri}")
	private String smbUri;
	@Value("${fs.smb.login}")
	private String smbLogin;
	@Value("${fs.smb.password}")
	private String smbPassword;

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
	public SmbAccessImpl smbAccessImpl(){
		SmbAccessImpl smbAccessImpl = new SmbAccessImpl();
		smbAccessImpl.setDriveName("CIFS");
		smbAccessImpl.setUri(smbUri);
		smbAccessImpl.setJcifsConfigProperties(smbProperties());
		smbAccessImpl.setLogin(smbLogin);
		smbAccessImpl.setPassword(smbPassword);
		return smbAccessImpl;
	}

	@Bean
	public Properties smbProperties(){
		Properties properties = new Properties();
		properties.put("jsmb.resolveOrder", "DNS,BCAST");
		properties.put("jsmb.encoding", "UTF8");
		properties.put("jsmb.smb.client.disablePlainTextPasswords", "true");
		properties.put("jsmb.smb.client.responseTimeout", "40000");
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
