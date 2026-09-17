package org.esupportail.esupsignature.dto.projection.jpa;

import java.util.Date;

public interface SignBookListMetadataProjection {

    Long getSignBookId();

    Long getPrimarySignRequestId();

    Long getSignRequestCount();

    String getPrimarySignRequestTitle();

    String getPrimarySignRequestStatus();

    Date getPrimarySignRequestCreateDate();

    Boolean getPrimarySignRequestDeleted();

    String getPrimarySignRequestCreateByEppn();

    Boolean getPrimarySignRequestViewedByCurrentUser();

    Boolean getPrimarySignRequestHasAttachments();

    String getPrimarySignRequestFirstOriginalFileName();

    Date getPrimarySignRequestLastSignedDocumentDate();

    String getPrimarySignRequestLastComment();

}
