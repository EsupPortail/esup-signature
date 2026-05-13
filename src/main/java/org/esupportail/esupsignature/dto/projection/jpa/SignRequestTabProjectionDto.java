package org.esupportail.esupsignature.dto.projection.jpa;

import org.esupportail.esupsignature.entity.enums.SignRequestStatus;

public interface SignRequestTabProjectionDto {
    Long getId();
    String getTitle();
    SignRequestStatus getStatus();
    Boolean getDeleted();
}

