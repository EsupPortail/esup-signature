package org.esupportail.esupsignature.dto.projection.jpa;

public interface DocumentProjectionDto {
    Long getId();
    String getFileName();
    Long getSize();
    String getContentType();
    String getPdfaCheck();
}

