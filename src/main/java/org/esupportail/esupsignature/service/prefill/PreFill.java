package org.esupportail.esupsignature.service.prefill;

import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.User;

import java.util.List;

public interface PreFill {

	String name = null;
	String getName();
	String getDescription();
	List<Field> preFillFields(List<Field> fields, User user);

}
