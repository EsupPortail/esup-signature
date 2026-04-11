package org.esupportail.esupsignature.service.view.ui;

import org.esupportail.esupsignature.dto.view.ui.AdminUiStatusDto;
import org.springframework.stereotype.Component;

@Component
public class AdminUiFetchMapper {

    public AdminUiStatusDto toAdminUiStatusDto(Integer nbSessions, Boolean dssStatus) {
        return new AdminUiStatusDto(nbSessions, dssStatus);
    }
}

