package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.repository.FieldRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class FieldService {

	@Resource
	private FieldRepository fieldRepository;
	
	public Field getById(long fieldId) {
		Field obj = fieldRepository.findById(fieldId).get();
		return obj;
	}	

	public void updateField(Field field) {
		if(field.getId() != null) {
			updateField(field.getId(), field.getRequired(), field.getReadOnly(), field.getExtValueServiceName(), field.getExtValueType(), field.getExtValueReturn(), field.getSearchServiceName(), field.getSearchType(), field.getSearchReturn(), field.getStepNumbers());
		} else {
			createField(field.getRequired(), field.getReadOnly(), field.getExtValueServiceName(), field.getExtValueType(), field.getExtValueReturn(), field.getSearchServiceName(), field.getSearchType(), field.getSearchReturn(), field.getStepNumbers());
		}
	}

	public Field updateField(Long id, Boolean required, Boolean readOnly, String extValueServiceName, String extValueType,
							 String extValueReturn, String searchServiceName, String searchType, String searchReturn, String stepNumbers) {
		Field field = fieldRepository.findById(id).get();
		setFieldValues(required, readOnly, extValueServiceName, extValueType, extValueReturn, searchServiceName, searchType, searchReturn, stepNumbers, field);
		return field;
	}

	public void setFieldValues(Boolean required, Boolean readOnly, String extValueServiceName, String extValueType, String extValueReturn, String searchServiceName, String searchType, String searchReturn, String stepNumbers, Field field) {
		field.setRequired(required);
		field.setReadOnly(readOnly);
		field.setExtValueServiceName(extValueServiceName);
		field.setExtValueType(extValueType);
		field.setExtValueReturn(extValueReturn);
		field.setSearchServiceName(searchServiceName);
		field.setSearchType(searchType);
		field.setSearchReturn(searchReturn);
		field.setStepNumbers(stepNumbers);
	}

	public Field createField(Boolean required, Boolean readOnly, String extValueServiceName, String extValueType,
								String extValueReturn, String searchServiceName, String searchType, String searchReturn, String stepNumbers) {
		Field field = new Field();
		setFieldValues(required, readOnly, extValueServiceName, extValueType, extValueReturn, searchServiceName, searchType, searchReturn, stepNumbers, field);
		fieldRepository.save(field);
		return field;
	}
	
	public void deleteField(int fieldId) {
		fieldRepository.delete(getById(fieldId));
	}
}
