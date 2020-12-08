package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.repository.FieldRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class FieldService {

	@Resource
	private FieldRepository fieldRepository;
	
	public Field getFieldById(long fieldId) {
		Field obj = fieldRepository.findById(fieldId).get();
		return obj;
	}	
	
	public List<Field> getAllFields(){
		List<Field> list = new ArrayList<Field>();
		fieldRepository.findAll().forEach(e -> list.add(e));
		return list;
	}
	
	public void updateField(Field field) {
		Field daoField = fieldRepository.findById(field.getId()).get();
		daoField.setRequired(Boolean.valueOf(field.getRequired()));
		daoField.setReadOnly(Boolean.valueOf(field.getReadOnly()));
		daoField.setExtValueServiceName(field.getExtValueServiceName());
		daoField.setExtValueType(field.getExtValueType());
		daoField.setExtValueReturn(field.getExtValueReturn());
		daoField.setSearchServiceName(field.getSearchServiceName());
		daoField.setSearchType(field.getSearchType());
		daoField.setSearchReturn(field.getSearchReturn());
		daoField.setStepNumbers(field.getStepNumbers());
		fieldRepository.save(daoField);
	}

	public Field createField(Long id, String required, String readOnly, String extValueServiceName, String extValueType,
								String extValueReturn, String searchServiceName, String searchType, String searchReturn, String stepNumbers) {
		Field field = new Field();
		field.setRequired(Boolean.valueOf(required));
		field.setReadOnly(Boolean.valueOf(readOnly));
		field.setExtValueServiceName(extValueServiceName);
		field.setExtValueType(extValueType);
		field.setExtValueReturn(extValueReturn);
		field.setSearchServiceName(searchServiceName);
		field.setSearchType(searchType);
		field.setSearchReturn(searchReturn);
		field.setStepNumbers(stepNumbers);
		return field;
	}
	
	public void deleteField(int fieldId) {
		fieldRepository.delete(getFieldById(fieldId));
	}
}
