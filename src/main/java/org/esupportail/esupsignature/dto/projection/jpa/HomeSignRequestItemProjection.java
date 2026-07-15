package org.esupportail.esupsignature.dto.projection.jpa;

public interface HomeSignRequestItemProjection {

    Long getSignBookId();

    Long getSignRequestId();

    String getTitle();

    String getStatus();

    Boolean getViewedByCurrentUser();

    Boolean getHasAttachments();

    Boolean getSignableByCurrentUser();

    String getFirstOriginalFileName();

    default String getFileName() {
        return getFirstOriginalFileName();
    }
}
