package org.esupportail.esupsignature.dto.projection.jpa;

public interface AttachmentProjectionDto extends DocumentProjectionDto {
    String getCreateByEppn();
    String getCreateByFirstname();
    String getCreateByName();
}

