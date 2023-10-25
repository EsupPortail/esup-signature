package org.esupportail.esupsignature.dss.model;

import eu.europa.esig.dss.enumerations.ASiCContainerType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.esupportail.esupsignature.dss.DssUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class SignatureMultipleDocumentsForm extends AbstractSignatureForm implements ContainerDocumentForm {

	@NotNull(message = "{error.container.type.mandatory}")
	private ASiCContainerType containerType;

	private List<MultipartFile> documentsToSign;

	@Override
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
		return DssUtils.isCollectionNotEmpty(documentsToSign);
	}

}