package org.esupportail.esupsignature.service.fs;

import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.fs.opencmis.CmisAccessImpl;
import org.esupportail.esupsignature.service.fs.smb.SmbAccessImpl;
import org.esupportail.esupsignature.service.fs.vfs.VfsAccessImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Service
public class FsAccessFactory {

	@Resource
	private SmbAccessImpl smbAccessImpl;

	@Resource
	private VfsAccessImpl vfsAccessImpl;

	@Resource
	private CmisAccessImpl cmisAccessImpl;

	public FsAccessService getFsAccessService(String path) throws EsupSignatureException {
		return  getFsAccessService(getPathIOType(path));
	}

	public FsAccessService getFsAccessService(DocumentIOType type) {
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

	public List<FsAccessService> getFsAccessServices() {
		List<FsAccessService> fsAccessServices = new ArrayList<>();
		if(smbAccessImpl != null) {
			fsAccessServices.add(smbAccessImpl);
		}
		if(vfsAccessImpl != null) {
			fsAccessServices.add(vfsAccessImpl);
		}
		if(cmisAccessImpl != null) {
			fsAccessServices.add(cmisAccessImpl);
		}
		return fsAccessServices;
	}

	public DocumentIOType getPathIOType(String path) throws EsupSignatureException {
		try {
			URI uri = new URI(path);
			return DocumentIOType.valueOf(uri.getScheme());
		} catch (URISyntaxException e) {
			throw new EsupSignatureException("target Url error", e);
		}
	}
}
