package org.esupportail.esupsignature.service.interfaces.fs;

import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.service.interfaces.fs.opencmis.CmisAccessImpl;
import org.esupportail.esupsignature.service.interfaces.fs.smb.SmbAccessImpl;
import org.esupportail.esupsignature.service.interfaces.fs.vfs.VfsAccessImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class FsAccessFactoryService {

	private static final Logger logger = LoggerFactory.getLogger(FsAccessFactoryService.class);

	private SmbAccessImpl smbAccessImpl;

	private VfsAccessImpl vfsAccessImpl;

	private CmisAccessImpl cmisAccessImpl;

	@Autowired(required = false)
	public void setSmbAccessImpl(SmbAccessImpl smbAccessImpl) {
		this.smbAccessImpl = smbAccessImpl;
	}

	@Autowired(required = false)
	public void setVfsAccessImpl(VfsAccessImpl vfsAccessImpl) {
		this.vfsAccessImpl = vfsAccessImpl;
	}

	@Autowired(required = false)
	public void setCmisAccessImpl(CmisAccessImpl cmisAccessImpl) {
		this.cmisAccessImpl = cmisAccessImpl;
	}

	public FsAccessService getFsAccessService(String uri) throws EsupSignatureFsException {
		DocumentIOType type = getPathIOType(uri);
		switch (type) {
			case smb:
				return smbAccessImpl;
			case vfs:
				return vfsAccessImpl;
			case cmis:
				return cmisAccessImpl;
			default:
				return null;
		}
	}

	public DocumentIOType getPathIOType(String path) throws EsupSignatureFsException {
		try {
			String url = path.replaceAll(" ", "%20");
			URI uri = new URI(url);
			if(uri.getScheme() != null) {
				switch (uri.getScheme()) {
					case "mailto":
						return DocumentIOType.mail;
					case "smb":
						return DocumentIOType.smb;
					case "cmis":
						return DocumentIOType.cmis;
					case "file":
					case "sftp":
					case "ftp":
						return DocumentIOType.vfs;
					case "http":
					case "https":
						return DocumentIOType.rest;
				}
			} else {
				return DocumentIOType.vfs;
			}
			logger.error("unknown protocol for url " + path);
			throw new EsupSignatureFsException("unknown protocol for url " + path);
		} catch (java.net.URISyntaxException e) {
			logger.error("target Url error " + e.getMessage());
			throw new EsupSignatureFsException("target Url error", e);
		}
	}

}
