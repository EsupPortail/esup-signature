package org.esupportail.esupsignature.service.fs;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.fs.opencmis.CmisAccessImpl;
import org.esupportail.esupsignature.service.fs.smb.SmbAccessImpl;
import org.esupportail.esupsignature.service.fs.vfs.VfsAccessImpl;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FsAccessFactory {

	@Autowired
	private ObjectProvider<SmbAccessImpl> smbAccessImpl;

	@Autowired
	private ObjectProvider<VfsAccessImpl> vfsAccessImpl;

	@Autowired
	private ObjectProvider<CmisAccessImpl> cmisAccessImpl;

	public FsAccessService getFsAccessService(String path) throws EsupSignatureException {
		return  getFsAccessService(getPathIOType(path));
	}

	public FsAccessService getFsAccessService(DocumentIOType type) {
		switch (type) {
			case smb:
				return smbAccessImpl.getIfAvailable();
			case vfs:
				return vfsAccessImpl.getIfAvailable();
			case cmis:
				return cmisAccessImpl.getIfAvailable();
			default:
				return null;
		}
	}

	public List<FsAccessService> getFsAccessServices() {
		List<FsAccessService> fsAccessServices = new ArrayList<>();
		smbAccessImpl.ifAvailable(sai -> fsAccessServices.add(sai));
		vfsAccessImpl.ifAvailable(vai -> fsAccessServices.add(vai));
		cmisAccessImpl.ifAvailable(cai -> fsAccessServices.add(cai));
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
