package org.esupportail.esupsignature.dss.service;

import eu.europa.esig.dss.model.identifier.Identifier;
import eu.europa.esig.dss.spi.tsl.LOTLInfo;
import eu.europa.esig.dss.spi.tsl.ParsingInfoRecord;
import eu.europa.esig.dss.spi.tsl.TLInfo;
import eu.europa.esig.dss.spi.tsl.TLValidationJobSummary;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource;
import eu.europa.esig.dss.tsl.job.TLValidationJob;
import eu.europa.esig.dss.utils.Utils;
import org.esupportail.esupsignature.dss.config.DSSBeanConfig;
import org.esupportail.esupsignature.dss.config.DSSProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@ConditionalOnBean(DSSBeanConfig.class)
public class DSSService {

    private static final Logger logger = LoggerFactory.getLogger(DSSService.class);

    private final DSSProperties dssProperties;

    private final TLValidationJob tlValidationJob;

    private final KeyStoreCertificateSource ojContentKeyStore;

    private final CommonTrustedCertificateSource myTrustedCertificateSource;

public DSSService(DSSProperties dssProperties, TLValidationJob tlValidationJob, KeyStoreCertificateSource ojContentKeyStore, CommonTrustedCertificateSource myTrustedCertificateSource) {
        this.dssProperties = dssProperties;
        this.tlValidationJob = tlValidationJob;
        this.ojContentKeyStore = ojContentKeyStore;
        this.myTrustedCertificateSource = myTrustedCertificateSource;
    }

    public LOTLInfo getLOTLInfoById(String lotlId) {
        TLValidationJobSummary summary = tlValidationJob.getSummary();
        List<LOTLInfo> lotlInfos = summary.getLOTLInfos();
        for (LOTLInfo lotlInfo : lotlInfos) {
            Identifier identifier = lotlInfo.getDSSId();
            String xmlId = identifier.asXmlId();
            if (xmlId.equals(lotlId)) {
                return lotlInfo;
            }
        }
        return null;
    }

    public TLInfo getTLInfoById(String tlId) {
        TLValidationJobSummary summary = tlValidationJob.getSummary();
        List<LOTLInfo> lotlInfos = summary.getLOTLInfos();
        for (LOTLInfo lotlInfo : lotlInfos) {
            TLInfo tlInfo = getTLInfoByIdFromList(tlId, lotlInfo.getTLInfos());
            if (tlInfo != null) {
                return tlInfo;
            }
        }
        List<TLInfo> otherTLInfos = summary.getOtherTLInfos();
        TLInfo tlInfo = getTLInfoByIdFromList(tlId, otherTLInfos);
        if (tlInfo != null) {
            return tlInfo;
        }
        return null;
    }

    private TLInfo getTLInfoByIdFromList(String tlId, List<TLInfo> tlInfos) {
        for (TLInfo tlInfo: tlInfos) {
            if (tlInfo.getDSSId().asXmlId().equals(tlId)) {
                return tlInfo;
            }
        }
        return null;
    }

    public String getActualOjUrl() {
        TLValidationJobSummary summary = tlValidationJob.getSummary();
        if (summary != null) {
            List<LOTLInfo> lotlInfos = summary.getLOTLInfos();
            for (LOTLInfo lotlInfo : lotlInfos) {
                if (Utils.areStringsEqual(dssProperties.getLotlUrl(), lotlInfo.getUrl())) {
                    ParsingInfoRecord parsingCacheInfo = lotlInfo.getParsingCacheInfo();
                    if (parsingCacheInfo != null) {
                        return parsingCacheInfo.getSigningCertificateAnnouncementUrl();
                    }
                }
            }
        }
        return null;
    }
    public String getCurrentOjUrl() {
        return dssProperties.getOjUrl();
    }

    public void getCertificats() throws IOException {
        logger.info("Updating DSS OJ offline...");
        ojContentKeyStore.addAllCertificatesToKeyStore(myTrustedCertificateSource.getCertificates());
        tlValidationJob.offlineRefresh();
        logger.info("Updating DSS OJ offline done.");
        if(refreshIsNeeded()) {
            logger.info("Updating DSS OJ online...");
            tlValidationJob.onlineRefresh();
            logger.info("Updating DSS OJ online done.");
        }
    }

    public boolean refreshIsNeeded() throws IOException {
        TLValidationJobSummary summary = tlValidationJob.getSummary();
        if(summary == null) return true;
        boolean checkTl = false;
        for (LOTLInfo lotlInfo : summary.getLOTLInfos()) {
            if(!lotlInfo.getValidationCacheInfo().isValid()
                    || lotlInfo.getValidationCacheInfo().isRefreshNeeded()
                    || lotlInfo.getParsingCacheInfo().isRefreshNeeded()
                    || lotlInfo.getDownloadCacheInfo().isRefreshNeeded()) {
                checkTl = true;
            }
        }
        for (TLInfo tlInfo : summary.getOtherTLInfos()) {
            if(!tlInfo.getValidationCacheInfo().isValid()
                    || tlInfo.getValidationCacheInfo().isRefreshNeeded()
                    || tlInfo.getParsingCacheInfo().isRefreshNeeded()
                    || tlInfo.getDownloadCacheInfo().isRefreshNeeded()) {
                checkTl = true;
            }
        }
        return checkTl;
    }

    @Async
    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            getCertificats();
        } catch (IOException e) {
            logger.error("Error updating certificates", e);
        }
    }
}
