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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
			URI uri = new URI(path);
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

	public List<String> getTree(String path) throws EsupSignatureFsException {
		List<String> tree = new ArrayList<>();
		DocumentIOType type = getPathIOType(path);
		String[] splitPath;
		switch (type) {
			case smb:
				splitPath = path.split("//")[1].split("/");
				int i = 0;
				for (String elem : splitPath) {
					if(i > 0) tree.add(elem);
					i++;
				}
				break;
			case vfs:
				if(path.startsWith("/")) {
					splitPath = path.split("/");
					tree.addAll(Arrays.asList(splitPath));
				} else {
					splitPath = path.split("//")[1].split("/");
					int j = 0;
					for (String elem : splitPath) {
						if(j > 0) tree.add(elem);
						j++;
					}
				}
				break;
			case cmis:
				splitPath = path.split("//")[1].split("/");
				tree.addAll(Arrays.asList(splitPath));
				break;
			default:
				return null;
		}
		return tree;
	}

	public void createPathIfNotExist(String path) throws EsupSignatureFsException {
		FsAccessService fsAccessService = getFsAccessService(path);
		List<String> tree = getTree(path);
		String parent = "/";
		try {
			URI uri =new URI(path);
			if(uri.getScheme() != null) {
				DocumentIOType type = getPathIOType(path);
				if(type.equals(DocumentIOType.cmis)) {
					parent = uri.getScheme() + "://";
				} else {
					parent = uri.getScheme() + "://" + uri.getAuthority() + "/";
				}
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		for(String folder : tree) {
			if (!fsAccessService.checkFolder(path)) {
				logger.info("create non existing folders : " + folder);
				fsAccessService.createFile(parent, folder, "folder");
				parent = parent + folder + "/";
			}
		}
	}
}
