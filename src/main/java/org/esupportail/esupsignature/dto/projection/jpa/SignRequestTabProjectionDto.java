package org.esupportail.esupsignature.dto.projection.jpa;

public interface SignRequestTabProjectionDto {
    Long getId();
    String getTitle();
    String getStatus();
    Boolean getDeleted();
    Boolean getViewedByCurrentUser();
}
