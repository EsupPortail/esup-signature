package org.esupportail.esupsignature.service.dss;

import eu.europa.esig.dss.model.identifier.Identifier;
import eu.europa.esig.dss.spi.tsl.*;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
import eu.europa.esig.dss.tsl.source.LOTLSource;
import eu.europa.esig.dss.utils.Utils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class DSSService {

    @Resource
    private CommonTrustedCertificateSource myTrustedCertificateSource;

    @Resource
    @Qualifier("european-trusted-list-certificate-source")
    private TrustedListsCertificateSource trustedListsCertificateSource;

    @Resource
    @Qualifier("european-lotl-source")
    private LOTLSource lotlSource;

    public LOTLInfo getLOTLInfoById(String lotlId) {
        TLValidationJobSummary summary = trustedListsCertificateSource.getSummary();
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
        TLValidationJobSummary summary = trustedListsCertificateSource.getSummary();
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
        TLValidationJobSummary summary = trustedListsCertificateSource.getSummary();
        if (summary != null) {
            List<LOTLInfo> lotlInfos = summary.getLOTLInfos();
            for (LOTLInfo lotlInfo : lotlInfos) {
                if (Utils.areStringsEqual(lotlSource.getUrl(), lotlInfo.getUrl())) {
                    ParsingInfoRecord parsingCacheInfo = lotlInfo.getParsingCacheInfo();
                    if (parsingCacheInfo != null) {
                        return parsingCacheInfo.getSigningCertificateAnnouncementUrl();
                    }
                }
            }
        }
        return null;
    }

    public TrustedListsCertificateSource getTrustedListsCertificateSource() {
        return trustedListsCertificateSource;
    }

    public LOTLSource getLotlSource() {
        return lotlSource;
    }

    public CommonTrustedCertificateSource getMyTrustedCertificateSource() {
        return myTrustedCertificateSource;
    }
}
