package org.esupportail.esupsignature.dto.projection.jpa;

import java.util.Date;

public interface AdminLogProjectionDto {
    Date getLogDate();
    String getEppn();
    String getAction();
    String getInitialStatus();
    String getFinalStatus();
    String getComment();
}

