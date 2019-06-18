package org.esupportail.esupsignature.web;

import java.util.List;

public class JsonSignInfoMessage {

	String status;
	List<String> nextRecipientEmails;
	List<String> nextRecipientNames;
	String downloadLink;
	
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public List<String> getNextRecipientEmails() {
		return nextRecipientEmails;
	}
	public void setNextRecipientEmails(List<String> nextRecipientEmails) {
		this.nextRecipientEmails = nextRecipientEmails;
	}
	public String getDownloadLink() {
		return downloadLink;
	}
	public void setDownloadLink(String downloadLink) {
		this.downloadLink = downloadLink;
	}
	public List<String> getNextRecipientNames() {
		return nextRecipientNames;
	}
	public void setNextRecipientNames(List<String> nextRecipientNames) {
		this.nextRecipientNames = nextRecipientNames;
	}

}
