package org.esupportail.esupsignature.dss.model;

import eu.europa.esig.dss.enumerations.ASiCContainerType;
import eu.europa.esig.dss.enumerations.SignatureForm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.model.DSSDocument;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class ExtensionForm {

	private DSSDocument signedFile;

	private List<DSSDocument> originalFiles;

	private ASiCContainerType containerType;

	@NotNull(message = "{error.signature.form.mandatory}")
	private SignatureForm signatureForm;

	@NotNull(message = "{error.signature.level.mandatory}")
	private SignatureLevel signatureLevel;

    public DSSDocument getSignedFile() {
        return signedFile;
    }

    public void setSignedFile(DSSDocument signedFile) {
        this.signedFile = signedFile;
    }

    public List<DSSDocument> getOriginalFiles() {
        return originalFiles;
    }

    public void setOriginalFiles(List<DSSDocument> originalFiles) {
        this.originalFiles = originalFiles;
    }

    public ASiCContainerType getContainerType() {
		return containerType;
	}

	public void setContainerType(ASiCContainerType containerType) {
		this.containerType = containerType;
	}

	public SignatureForm getSignatureForm() {
		return signatureForm;
	}

	public void setSignatureForm(SignatureForm signatureForm) {
		this.signatureForm = signatureForm;
	}

	public SignatureLevel getSignatureLevel() {
		return signatureLevel;
	}

	public void setSignatureLevel(SignatureLevel signatureLevel) {
		this.signatureLevel = signatureLevel;
	}

	@AssertTrue(message = "{error.signed.file.mandatory}")
	public boolean isSignedFile() {
		return signedFile != null;
	}

}
