package org.esupportail.esupsignature.config.sms;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.service.interfaces.sms.SmsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
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
