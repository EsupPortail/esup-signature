package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.FieldType;
import org.esupportail.esupsignature.repository.FieldRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FieldService {

	@Resource
	private FieldRepository fieldRepository;

	@Resource
	private WorkflowStepService workflowStepService;

	public Field getById(long fieldId) {
		Field obj = fieldRepository.findById(fieldId).get();
		return obj;
	}

	@Transactional
	public Field createField(String fieldName) {
		Field field = new Field();
		field.setName(fieldName);
		field.setLabel(fieldName);
		field.setType(FieldType.text);
		fieldRepository.save(field);
		return field;
	}

	public void updateField(Field field) {
		if (field.getId() !=null) {
			updateField(field.getId(), field.getFavorisable(), field.getRequired(), field.getReadOnly(), field.getExtValueServiceName(), field.getExtValueType(), field.getExtValueReturn(), field.getSearchServiceName(), field.getSearchType(), field.getSearchReturn(), field.getStepZero(), field.getWorkflowSteps().stream().map(WorkflowStep::getId).collect(Collectors.toList()));
		}else {
			createField(field.getRequired(), field.getReadOnly(), field.getExtValueServiceName(), field.getExtValueType(), field.getExtValueReturn(), field.getSearchServiceName(), field.getSearchType(), field.getSearchReturn(), field.getStepZero(), field.getWorkflowSteps());
		}
	}

	@Transactional
	public Field updateField(Long id, Boolean favorisable, Boolean required, Boolean readOnly, String extValueServiceName, String extValueType,
							 String extValueReturn, String searchServiceName, String searchType, String searchReturn, Boolean stepZero, List<Long> workflowStepsIds) {
		Field field = fieldRepository.findById(id).get();
		List<WorkflowStep> workflowSteps = new ArrayList<>();
		if(workflowStepsIds != null) {
			for (Long workflowStepId : workflowStepsIds) {
				workflowSteps.add(workflowStepService.getById(workflowStepId));
			}
		}
		setFieldValues(favorisable, required, readOnly, extValueServiceName, extValueType, extValueReturn, searchServiceName, searchType, searchReturn, stepZero, workflowSteps, field);
		return field;
	}

	public void setFieldValues(Boolean favorisable, Boolean required, Boolean readOnly, String extValueServiceName, String extValueType, String extValueReturn, String searchServiceName, String searchType, String searchReturn, Boolean stepZero, List<WorkflowStep>workflowSteps, Field field) {
		field.setFavorisable(favorisable);
		field.setRequired(required);
		field.setReadOnly(readOnly);
		field.setExtValueServiceName(extValueServiceName);
		field.setExtValueType(extValueType);
		field.setExtValueReturn(extValueReturn);
		field.setSearchServiceName(searchServiceName);
		field.setSearchType(searchType);
		field.setSearchReturn(searchReturn);
		field.setStepZero(stepZero);
		field.getWorkflowSteps().clear();
		field.getWorkflowSteps().addAll(workflowSteps);
	}

	public Field createField(Boolean required, Boolean readOnly, String extValueServiceName, String extValueType,
								String extValueReturn, String searchServiceName, String searchType, String searchReturn, Boolean stepZero, List<WorkflowStep> workflowSteps) {
		Field field = new Field();
		setFieldValues(false, required, readOnly, extValueServiceName, extValueType, extValueReturn, searchServiceName, searchType, searchReturn, stepZero, workflowSteps, field);
		fieldRepository.save(field);
		return field;
	}
	
	public void deleteField(int fieldId) {
		fieldRepository.delete(getById(fieldId));
	}

    public List<Field> getFieldsByWorkflowStep(WorkflowStep workflowStep) {
		return fieldRepository.findByWorkflowStepsContains(workflowStep);
    }
}
