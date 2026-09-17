package org.esupportail.esupsignature.dto.projection.jpa;

import org.esupportail.esupsignature.entity.enums.UserType;

import java.util.Date;

public interface AdminCommentProjectionDto {
    Long getId();
    Date getCreateDate();
    String getText();
    Long getCreateById();
    String getCreateByEppn();
    String getCreateByFirstname();
    String getCreateByName();
    String getCreateByEmail();
    String getCreateByPhone();
    UserType getCreateByUserType();
}

