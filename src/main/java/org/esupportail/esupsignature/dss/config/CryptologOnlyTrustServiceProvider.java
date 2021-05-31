package org.esupportail.esupsignature.dss.config;

import eu.europa.esig.dss.tsl.function.TrustServiceProviderPredicate;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.trustedlist.jaxb.tsl.InternationalNamesType;
import eu.europa.esig.trustedlist.jaxb.tsl.MultiLangNormStringType;
import eu.europa.esig.trustedlist.jaxb.tsl.TSPInformationType;
import eu.europa.esig.trustedlist.jaxb.tsl.TSPType;

public class CryptologOnlyTrustServiceProvider implements TrustServiceProviderPredicate {

    @Override
    public boolean test(TSPType t) {

        TSPInformationType tspInformation = t.getTSPInformation();
        if (tspInformation != null) {
            InternationalNamesType tspName = tspInformation.getTSPName();
            if (tspName != null && Utils.isCollectionNotEmpty(tspName.getName())) {
                for (MultiLangNormStringType langAndValue : tspName.getName()) {
                    if ("Cryptolog International".equals(langAndValue.getValue())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
