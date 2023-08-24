package org.esupportail.esupsignature.config.sms;

import org.esupportail.esupsignature.service.interfaces.sms.SmsService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.Resource;
import java.util.List;

@Configuration
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
        if(smsProperties.getEnableSms()) {
            for (SmsService smsService : smsServices) {
                if (smsService.getName().equals(smsProperties.getServiceName())) {
                    return smsService;
                }
            }
        }
        return null;
    }

}
