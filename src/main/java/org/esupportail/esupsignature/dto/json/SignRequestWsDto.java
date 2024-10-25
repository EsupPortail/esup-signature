package org.esupportail.esupsignature.dto.json;

import org.esupportail.esupsignature.entity.enums.SignRequestStatus;

import java.util.Date;

public interface SignRequestWsDto {
    Long getId();
    String getTitle();
    SignRequestStatus getStatus();
    Date getCreateDate();
    String createByEppn();
    Date getEndDate();
}
