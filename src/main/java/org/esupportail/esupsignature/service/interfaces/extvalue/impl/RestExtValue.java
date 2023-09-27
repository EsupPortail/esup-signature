package org.esupportail.esupsignature.service.interfaces.extvalue.impl;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.interfaces.extvalue.ExtValue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty("global.rest-ext-value-url")
public class RestExtValue implements ExtValue {

	@Resource
	private GlobalProperties globalProperties;

	@Override
	public String getValueByName(String name, User user, SignRequest signRequest) {
		return initValues(user, signRequest).get(name).toString();
	}

	@Override
	public List<Map<String, Object>> search(String searchType, String searchString, String searchReturn) {
		return null;
	}

	@Override
	public String getName() {
		return "rest";
	}

	@Override
	public Map<String, Object> initValues(User user, SignRequest signRequest) {
		RestTemplate restTemplate = new RestTemplate();
		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("eppn", user.getEppn());
		if(signRequest != null) {
			map.add("signRequestId", signRequest.getId());
		}
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);
		return restTemplate.postForObject(globalProperties.getRestExtValueUrl(), requestEntity, HashMap.class);
	}

}
