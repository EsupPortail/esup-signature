package org.esupportail.esupsignature.web.controller.ws.json;

import java.util.ArrayList;
import java.util.List;

public class JsonSignRequestStatus {

	String status;
	List<String> nextRecipientEmails = new ArrayList<>();
	List<String> nextRecipientNames = new ArrayList<>();
	List<String> nextRecipientEppns = new ArrayList<>();
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

	public List<String> getNextRecipientNames() {
		return nextRecipientNames;
	}
	public void setNextRecipientNames(List<String> nextRecipientNames) {
		this.nextRecipientNames = nextRecipientNames;
	}

	public List<String> getNextRecipientEppns() {
		return nextRecipientEppns;
	}

	public void setNextRecipientEppns(List<String> nextRecipientEppns) {
		this.nextRecipientEppns = nextRecipientEppns;
	}

	public String getDownloadLink() {
		return downloadLink;
	}
	public void setDownloadLink(String downloadLink) {
		this.downloadLink = downloadLink;
	}
}
