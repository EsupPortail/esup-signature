package org.esupportail.esupsignature.dss.web.model;

import eu.europa.esig.dss.ASiCContainerType;
import eu.europa.esig.dss.utils.Utils;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import java.util.List;

public class SignatureMultipleDocumentsForm extends AbstractSignatureForm {

	@NotNull(message = "{error.container.type.mandatory}")
	private ASiCContainerType containerType;

	private List<MultipartFile> documentsToSign;

	public ASiCContainerType getContainerType() {
		return containerType;
	}

	public void setContainerType(ASiCContainerType containerType) {
		this.containerType = containerType;
	}

	public List<MultipartFile> getDocumentsToSign() {
		return documentsToSign;
	}

	public void setDocumentsToSign(List<MultipartFile> documentsToSign) {
		this.documentsToSign = documentsToSign;
	}

	@AssertTrue(message = "{error.to.sign.files.mandatory}")
	public boolean isDocumentsToSign() {
		return Utils.isCollectionNotEmpty(documentsToSign);
	}

}
