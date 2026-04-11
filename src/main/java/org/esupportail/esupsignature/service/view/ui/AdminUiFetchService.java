package org.esupportail.esupsignature.service.view.ui;

import org.esupportail.esupsignature.dto.view.ui.AdminUiStatusDto;
import org.esupportail.esupsignature.dss.service.DSSService;
import org.esupportail.esupsignature.repository.custom.SessionRepositoryCustom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AdminUiFetchService {

    private static final Logger logger = LoggerFactory.getLogger(AdminUiFetchService.class);

    private final DSSService dssService;
    private final SessionRepositoryCustom sessionRepositoryCustom;
    private final AdminUiFetchMapper adminUiFetchMapper;

    public AdminUiFetchService(@Autowired(required = false) DSSService dssService,
                               SessionRepositoryCustom sessionRepositoryCustom,
                               AdminUiFetchMapper adminUiFetchMapper) {
        this.dssService = dssService;
        this.sessionRepositoryCustom = sessionRepositoryCustom;
        this.adminUiFetchMapper = adminUiFetchMapper;
    }

    public AdminUiStatusDto buildAdminUiStatus() {
        Boolean dssStatus = null;
        try {
            if (dssService != null) {
                dssStatus = dssService.refreshIsNeeded();
            }
        } catch (IOException e) {
            logger.debug("enable to get dss status");
        }
        return adminUiFetchMapper.toAdminUiStatusDto(sessionRepositoryCustom.findAllSessionIds().size(), dssStatus);
    }
}

