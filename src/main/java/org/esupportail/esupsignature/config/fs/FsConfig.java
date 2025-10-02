package org.esupportail.esupsignature.config.fs;

import org.esupportail.esupsignature.service.interfaces.fs.opencmis.CmisAccessImpl;
import org.esupportail.esupsignature.service.interfaces.fs.smb.SmbAccessImpl;
import org.esupportail.esupsignature.service.interfaces.fs.vfs.VfsAccessImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class FsConfig {

	private final FsProperties fsProperties;

	public FsConfig(FsProperties fsProperties) {
		this.fsProperties = fsProperties;
	}

	@Bean
	@ConditionalOnProperty({"fs.smb-login"})
	public SmbAccessImpl smbAccessImpl(){
		SmbAccessImpl smbAccessImpl = new SmbAccessImpl();
		smbAccessImpl.setDriveName("CIFS");
		smbAccessImpl.setUri(fsProperties.getSmbTestUri());
		smbAccessImpl.setJcifsConfigProperties(smbProperties());
		smbAccessImpl.setLogin(fsProperties.getSmbLogin());
		smbAccessImpl.setPassword(fsProperties.getSmbPassword());
		smbAccessImpl.setDomain(fsProperties.getSmbDomain());
		return smbAccessImpl;
	}

	public Properties smbProperties(){
		Properties properties = new Properties();
		properties.put("jsmb.resolveOrder", "DNS,BCAST");
		properties.put("jsmb.encoding", "UTF8");
		properties.put("jsmb.smb.client.disablePlainTextPasswords", "true");
		properties.put("jsmb.smb.client.responseTimeout", "40000");
		return properties;
	
	}
	
	@Bean
	@ConditionalOnProperty("fs.vfs-test-uri")
	public VfsAccessImpl vfsAccessImpl(){
		VfsAccessImpl vfsAccessImpl = new VfsAccessImpl();
		vfsAccessImpl.setDriveName("VFS");
		vfsAccessImpl.setUri(fsProperties.getVfsTestUri());
		return vfsAccessImpl;
	}

	@Bean
	@ConditionalOnProperty("fs.cmis-test-uri")
	public CmisAccessImpl cmisAccessImpl(){
		CmisAccessImpl cmisAccessImpl = new CmisAccessImpl();
		cmisAccessImpl.setDriveName("CMIS");
		cmisAccessImpl.setUri(fsProperties.getCmisTestUri());
		cmisAccessImpl.setLogin(fsProperties.getCmisLogin());
		cmisAccessImpl.setPassword(fsProperties.getCmisPassword());
		cmisAccessImpl.setRespositoryId(fsProperties.getCmisRespositoryId());
//		cmisAccessImpl.setRootPath(fsProperties.getCmisRootPath());
		return cmisAccessImpl;
	}

}
