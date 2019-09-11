package org.esupportail.esupsignature.config;

import java.util.Properties;

import org.esupportail.esupsignature.service.fs.opencmis.CmisAccessImpl;
import org.esupportail.esupsignature.service.fs.smb.SmbAccessImpl;
import org.esupportail.esupsignature.service.fs.vfs.VfsAccessImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="fs")
public class FsConfig {

	private String smbUri;
	private String smbLogin;
	private String smbPassword;
	private String vfsUri;
	private String cmisUri;
	private String cmisLogin;
	private String cmisPassword;
	private String cmisRespositoryId;
	private String cmisRootPath;

	public String getSmbUri() {
		return smbUri;
	}

	public void setSmbUri(String smbUri) {
		this.smbUri = smbUri;
	}

	public String getSmbLogin() {
		return smbLogin;
	}

	public void setSmbLogin(String smbLogin) {
		this.smbLogin = smbLogin;
	}

	public String getSmbPassword() {
		return smbPassword;
	}

	public void setSmbPassword(String smbPassword) {
		this.smbPassword = smbPassword;
	}

	public String getVfsUri() {
		return vfsUri;
	}

	public void setVfsUri(String vfsUri) {
		this.vfsUri = vfsUri;
	}

	public String getCmisUri() {
		return cmisUri;
	}

	public void setCmisUri(String cmisUri) {
		this.cmisUri = cmisUri;
	}

	public String getCmisLogin() {
		return cmisLogin;
	}

	public void setCmisLogin(String cmisLogin) {
		this.cmisLogin = cmisLogin;
	}

	public String getCmisPassword() {
		return cmisPassword;
	}

	public void setCmisPassword(String cmisPassword) {
		this.cmisPassword = cmisPassword;
	}

	public String getCmisRespositoryId() {
		return cmisRespositoryId;
	}

	public void setCmisRespositoryId(String cmisRespositoryId) {
		this.cmisRespositoryId = cmisRespositoryId;
	}

	public String getCmisRootPath() {
		return cmisRootPath;
	}

	public void setCmisRootPath(String cmisRootPath) {
		this.cmisRootPath = cmisRootPath;
	}

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
