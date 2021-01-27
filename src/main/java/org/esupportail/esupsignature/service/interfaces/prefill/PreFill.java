package org.esupportail.esupsignature.service.interfaces.prefill;

import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.User;

import java.util.List;
import java.util.Map;

public interface PreFill {

	String name = null;
	String getName();
	Map<String, List<String>> getTypes();
	String getDescription();
	List<Field> preFillFields(List<Field> fields, User user);

}
