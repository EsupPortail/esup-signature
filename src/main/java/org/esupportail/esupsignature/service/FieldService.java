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
		fieldRepository.save(field);
	}
	
	public void deleteField(int fieldId) {
		fieldRepository.delete(getFieldById(fieldId));
	}
}
