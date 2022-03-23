package org.esupportail.esupsignature.dss.config;

import eu.europa.esig.dss.service.crl.JdbcCacheCRLSource;
import eu.europa.esig.dss.service.x509.aia.JdbcCacheAIASource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.sql.SQLException;

@Component
public class JdbcInitializer {

    @Resource
    private JdbcCacheAIASource cachedAIASource;

    @Resource
    private JdbcCacheCRLSource cachedCRLSource;

    @PostConstruct
    public void cachedAIASourceInitialization() throws SQLException {
        cachedAIASource.initTable();
    }

    @PostConstruct
    public void cachedCRLSourceInitialization() throws SQLException {
        cachedCRLSource.initTable();
    }

    @PreDestroy
    public void cachedAIASourceClean() throws SQLException {
        cachedAIASource.destroyTable();
    }

    @PreDestroy
    public void cachedCRLSourceClean() throws SQLException {
        cachedCRLSource.destroyTable();
    }

}