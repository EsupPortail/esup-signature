package org.esupportail.esupsignature.domain;

import java.util.Date;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.springframework.format.annotation.DateTimeFormat;

public class Log {

	@Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy - HH:mm")
    private Date logDate;
	
	private String eppn;
	
	private long signRequestId;
	
	//TODO : Logger tout au lieu de createdate et createby
}
