package org.esupportail.esupsignature.dto.projection.jpa;

import org.esupportail.esupsignature.entity.User;

public record RoleManagerDto(String role, User user) {
}

