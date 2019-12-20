package org.esupportail.esupsignature.service.fs;

import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.service.fs.opencmis.CmisAccessImpl;
import org.esupportail.esupsignature.service.fs.smb.SmbAccessImpl;
import org.esupportail.esupsignature.service.fs.vfs.VfsAccessImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class FsAccessFactory {

	@Resource
	private SmbAccessImpl smbAccessImpl;

	@Resource
	private VfsAccessImpl vfsAccessImpl;

	@Resource
	private CmisAccessImpl cmisAccessImpl;
	
	
	public FsAccessService getFsAccessService(DocumentIOType type) {
		FsAccessService fsAccessService = null;
		switch (type) {
		case smb:
			fsAccessService = smbAccessImpl;
			break;
		case vfs:
			fsAccessService = vfsAccessImpl;
			break;
		case cmis:
			fsAccessService = cmisAccessImpl;
			break;
		default:
			break;
		}
		return fsAccessService;
	}
}
