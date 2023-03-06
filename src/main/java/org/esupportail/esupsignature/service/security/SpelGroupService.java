package org.esupportail.esupsignature.service.security;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.*;
import java.util.stream.Collectors;

public class SpelGroupService implements GroupService {

	private final GlobalProperties globalProperties;

	private Map<String, String> groups4eppnSpel = new HashMap<>();

	public void setGroups4eppnSpel(Map<String, String> groups4eppnSpel) {
		this.groups4eppnSpel = groups4eppnSpel;
	}

	public SpelGroupService(GlobalProperties globalProperties) {
		this.globalProperties = globalProperties;
	}

	@Override
	public List<Map.Entry<String, String>> getAllGroups(String search) {
		return null;
	}

	@Override
	public List<String> getGroups(String userName) {
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