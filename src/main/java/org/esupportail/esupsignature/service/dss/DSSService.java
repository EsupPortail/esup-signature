package org.esupportail.esupsignature.service.dss;

import eu.europa.esig.dss.model.identifier.Identifier;
import eu.europa.esig.dss.spi.tsl.LOTLInfo;
import eu.europa.esig.dss.spi.tsl.ParsingInfoRecord;
import eu.europa.esig.dss.spi.tsl.TLInfo;
import eu.europa.esig.dss.spi.tsl.TLValidationJobSummary;
import eu.europa.esig.dss.tsl.job.TLValidationJob;
import eu.europa.esig.dss.utils.Utils;
import org.esupportail.esupsignature.dss.config.DSSProperties;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DSSService {

    private final TLValidationJob job;

    private final DSSProperties dssProperties;

    public DSSService(TLValidationJob job, DSSProperties dssProperties) {
        this.job = job;
        this.dssProperties = dssProperties;
    }

    public LOTLInfo getLOTLInfoById(String lotlId) {
        TLValidationJobSummary summary = job.getSummary();
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
        TLValidationJobSummary summary = job.getSummary();
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
        TLValidationJobSummary summary = job.getSummary();
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

}
