package org.esupportail.esupsignature.dto.projection.jpa;

import org.esupportail.esupsignature.entity.enums.SignRequestStatus;

public interface SignRequestLightProjectionDto {
    Long getId();
    Long getClonedFromId();
    SignRequestStatus getStatus();
    Boolean getDeleted();
    String getToken();
    Long getCreateById();
    String getCreateByEppn();
    String getCreateByFirstname();
    String getCreateByName();
}
