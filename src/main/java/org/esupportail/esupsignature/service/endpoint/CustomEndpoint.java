package org.esupportail.esupsignature.service.endpoint;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "custom")
public class CustomEndpoint {

    @ReadOperation
    public String test() {
        return "test custom actuator endpoint";
    }

    @ReadOperation
    public String loggerLevels(@Selector String name) {

        if(name.equals("test")) {
            return "test1";
        }

        return "";
    }

}
