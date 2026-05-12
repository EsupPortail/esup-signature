package org.esupportail.esupsignature.dto.projection.jpa;

import org.esupportail.esupsignature.entity.User;

public class RoleManagerDto {

    private String role;
    private User user;

    public RoleManagerDto() {
    }

    public RoleManagerDto(String role, User user) {
        this.role = role;
        this.user = user;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String role() {
        return role;
    }

    public User user() {
        return user;
    }
}
