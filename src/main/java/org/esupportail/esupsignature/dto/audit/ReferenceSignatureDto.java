package org.esupportail.esupsignature.dto.audit;

import java.util.Date;

/** Public reference information, without technical signature evidence. */
public class ReferenceSignatureDto {
    private String name;
    private String firstname;
    private String email;
    private Date date;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
