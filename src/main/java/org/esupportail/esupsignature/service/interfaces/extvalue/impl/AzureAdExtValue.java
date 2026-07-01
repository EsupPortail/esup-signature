package org.esupportail.esupsignature.service.interfaces.extvalue.impl;

import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.interfaces.extvalue.ExtValue;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AzureAdExtValue implements ExtValue {

    @Override
    public String getName() {
        return "azuread";
    }

    @Override
    public String getValueByName(String name, User user, SignRequest signRequest) {
        Object value = initValues(user, signRequest).get(name);
        return value != null ? value.toString() : "";
    }

    @Override
    public List<Map<String, Object>> search(String searchType, String searchString, String searchReturn) {
        return null;
    }

    @Override
    public Map<String, Object> initValues(User user, SignRequest signRequest) {
        Map<String, Object> values = new HashMap<>();
        if (user != null && user.getOidcAttributes() != null) {
            values.putAll(user.getOidcAttributes());
        }
        return values;
    }
}
