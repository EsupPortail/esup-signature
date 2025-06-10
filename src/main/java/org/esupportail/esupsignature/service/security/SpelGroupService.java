package org.esupportail.esupsignature.service.security;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.security.WebSecurityProperties;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SpelGroupService implements GroupService {

	private final GlobalProperties globalProperties;

	private Map<String, String> groups4eppnSpel = new HashMap<>();

	public void setGroups4eppnSpel(Map<String, String> groups4eppnSpel) {
		this.groups4eppnSpel = groups4eppnSpel;
	}

	public SpelGroupService(GlobalProperties globalProperties, WebSecurityProperties webSecurityProperties) {
		this.globalProperties = globalProperties;
        Map<String, String> groups4eppnSpel = new HashMap<>();
		if (webSecurityProperties.getGroupMappingSpel() != null) {
			for (String groupName : webSecurityProperties.getGroupMappingSpel().keySet()) {
				String spelRule = webSecurityProperties.getGroupMappingSpel().get(groupName);
				groups4eppnSpel.put(groupName, spelRule);
			}
		}
		this.setGroups4eppnSpel(groups4eppnSpel);
	}

	@Override
	public List<Map.Entry<String, String>> getAllGroupsStartWith(String search) {
		return null;
	}

	@Override
	public List<String> getGroupsOfUser(String userName) {
		String eppn;
		if(!userName.contains("@")) {
			eppn = userName + "@" + globalProperties.getDomain();
		} else {
			eppn = userName;
		}
		List<String> groups = new ArrayList<>();
		for(String groupName: groups4eppnSpel.keySet()) {
			String expression = groups4eppnSpel.get(groupName);
			ExpressionParser parser = new SpelExpressionParser();
			Expression exp = parser.parseExpression(expression);
			EvaluationContext context = new StandardEvaluationContext();
			context.setVariable("eppn", eppn);
			Boolean value = (Boolean) exp.getValue(context);
			if(Boolean.TRUE.equals(value)) {
				groups.add(groupName);
			}
		}		
		return groups.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
		
	}

	@Override
	public List<String> getMembers(String groupName) {
		return null;
	}

}