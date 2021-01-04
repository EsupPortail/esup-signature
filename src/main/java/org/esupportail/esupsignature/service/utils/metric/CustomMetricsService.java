package org.esupportail.esupsignature.service.utils.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Service;

@Service
public class CustomMetricsService {

    private final MeterRegistry registry;

    public CustomMetricsService(MeterRegistry registry) {
        this.registry = registry;
    }

    public void registerValue(String metricName, String valueName) {
        this.registry.counter(metricName, tags(valueName));
    }

    public void incValue(String metricName, String valueName) {
        this.registry.counter(metricName, tags(valueName)).increment();
    }

    public double getValue(String metricName, String valueName) {
        return this.registry.counter(metricName, tags(valueName)).count();
    }

    protected static Iterable<Tag> tags(String valueName) {
        return Tags.of(Tag.of("value", valueName));
    }
}
