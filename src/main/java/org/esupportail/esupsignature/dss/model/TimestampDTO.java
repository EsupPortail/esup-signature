package org.esupportail.esupsignature.dss.model;

import eu.europa.esig.dss.enumerations.*;

public class TimestampDTO {

	private String base64Timestamp;
	private String canonicalizationMethod;
	private TimestampType type;

	// includes are ignored (only complete content-timestamp are supported in the demo)

	public String getBase64Timestamp() {
		return base64Timestamp;
	}

	public void setBase64Timestamp(String base64Timestamp) {
		this.base64Timestamp = base64Timestamp;
	}

	public String getCanonicalizationMethod() {
		return canonicalizationMethod;
	}

	public void setCanonicalizationMethod(String canonicalizationMethod) {
		this.canonicalizationMethod = canonicalizationMethod;
	}

	public TimestampType getType() {
		return type;
	}

	public void setType(TimestampType type) {
		this.type = type;
	}

}
