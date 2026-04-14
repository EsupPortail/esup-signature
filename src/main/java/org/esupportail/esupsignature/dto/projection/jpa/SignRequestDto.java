package org.esupportail.esupsignature.dto.projection.jpa;

import org.esupportail.esupsignature.entity.enums.SignRequestStatus;

import java.util.Date;

public interface SignRequestDto {
    Long getId();
    String getTitle();
    SignRequestStatus getStatus();
    Date getCreateDate();
    String createByEppn();
    Date getEndDate();
}
