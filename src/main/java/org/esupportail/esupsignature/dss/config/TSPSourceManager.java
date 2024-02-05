package org.esupportail.esupsignature.dss.config;

import eu.europa.esig.dss.service.http.commons.TimestampDataLoader;
import eu.europa.esig.dss.service.http.proxy.ProxyConfig;
import eu.europa.esig.dss.service.tsp.OnlineTSPSource;
import eu.europa.esig.dss.spi.x509.tsp.CompositeTSPSource;
import eu.europa.esig.dss.spi.x509.tsp.TSPSource;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
public class TSPSourceManager {
    private final CompositeTSPSource compositeTSPSource = new CompositeTSPSource();
    private final TimestampDataLoader timestampDataLoader = new TimestampDataLoader();

    public TSPSourceManager() {
        configureTimestampDataLoader();
    }

    public TSPSourceManager(ProxyConfig proxyConfig) {
        configureTimestampDataLoader(proxyConfig);
    }

    private void configureTimestampDataLoader() {
        timestampDataLoader.setTimeoutConnection(10000);
        timestampDataLoader.setTimeoutConnectionRequest(10000);
        timestampDataLoader.setTrustStrategy(TrustAllStrategy.INSTANCE);
    }

    private void configureTimestampDataLoader(ProxyConfig proxyConfig) {
        configureTimestampDataLoader();
        if (proxyConfig != null) {
            timestampDataLoader.setProxyConfig(proxyConfig);
        }
    }

    public void initializeTSPSources(List<String> tspServerUrls) {
        var tspSources = new HashMap<String, TSPSource>();
        tspServerUrls.forEach(url -> {
            var onlineTSPSource = new OnlineTSPSource(url);
            onlineTSPSource.setDataLoader(timestampDataLoader);
            tspSources.put(url, onlineTSPSource);
        });
        compositeTSPSource.setTspSources(tspSources);
    }

    public TSPSource getCompositeTSPSource() {
        return compositeTSPSource;
    }
}
