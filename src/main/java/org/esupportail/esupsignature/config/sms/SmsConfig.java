package org.esupportail.esupsignature.config.sms;

import org.esupportail.esupsignature.service.sms.SmsService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.List;

@Configuration
@ConditionalOnProperty(prefix = "sms", name = "service-name")
@EnableConfigurationProperties(SmsProperties.class)
public class SmsConfig {

	private SmsProperties smsProperties;

    public SmsConfig(SmsProperties smsProperties) {
        this.smsProperties = smsProperties;
    }

    @Resource
    List<SmsService> smsServices;

    @Bean
    public SmsService smsService() {
        for(SmsService smsService : smsServices) {
            if(smsService.getName().equals(smsProperties.getServiceName())) {
                return smsService;
            }
        }
        return null;
    }

}
