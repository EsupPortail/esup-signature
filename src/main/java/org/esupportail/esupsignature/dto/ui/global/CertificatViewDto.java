package org.esupportail.esupsignature.dto.ui.global;

import java.util.Date;
import java.util.Set;

public class CertificatViewDto {

    private final Long id;
    private final String fileName;
    private final Date createDate;
    private final Date expireDate;
    private final Set<String> roles;

    public CertificatViewDto(Long id, String fileName, Date createDate, Date expireDate, Set<String> roles) {
        this.id = id;
        this.fileName = fileName;
        this.createDate = createDate;
        this.expireDate = expireDate;
        this.roles = Set.copyOf(roles);
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public Date getExpireDate() {
        return expireDate;
    }

    public Set<String> getRoles() {
        return roles;
    }
}
