package org.esupportail.esupsignature.service.interfaces.extvalue.impl;

import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.interfaces.extvalue.ExtValue;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DefaultExtValue implements ExtValue {

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
		return "default";
	}

	@Override
	public Map<String, Object> initValues(User user, SignRequest signRequest) {
		Map<String, Object> values = new HashMap<>();
		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		values.put("day", new SimpleDateFormat("dd").format(date));
		values.put("month", new SimpleDateFormat("MM").format(date));
		values.put("year", new SimpleDateFormat("YYYY").format(date));
		values.put("signDate", new SimpleDateFormat("dd/MM/YYYY").format(date));
		values.put("date", new SimpleDateFormat("dd/MM/YYYY").format(date));
		values.put("time", new SimpleDateFormat("HH:mm").format(date));
		values.put("dateTime", new SimpleDateFormat("dd/MM/YYYY HH:mm").format(date));
		values.put("currentUser", user.getFirstname() + " " + user.getName());
		if(signRequest != null) {
			values.put("stepUsers", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getUsers().stream().map(User::getEmail).collect(Collectors.joining(",")));
			values.put("currentStepNumber", String.valueOf(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber()));
			values.put("id", String.valueOf(signRequest.getId()));
		}
		return values;
	}

}
