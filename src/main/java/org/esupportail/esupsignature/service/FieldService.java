package org.esupportail.esupsignature.service;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.FieldType;
import org.esupportail.esupsignature.repository.FieldRepository;
import org.esupportail.esupsignature.repository.FormRepository;
import org.esupportail.esupsignature.repository.WorkflowStepRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FieldService {

	@Resource
	private FieldRepository fieldRepository;

	@Resource
	private WorkflowStepRepository workflowStepRepository;

	@Resource
	private FormRepository formRepository;

	public Field getById(long fieldId) {
		Field obj = fieldRepository.findById(fieldId).get();
		return obj;
	}

	@Transactional
	public Field createField(String fieldName, Workflow workflow, FieldType fieldType) {
		Field field = new Field();
		field.setName(fieldName);
		field.setLabel(fieldName);
		field.setType(fieldType);
		if(workflow != null && workflow.getWorkflowSteps().size() > 0) {
			field.getWorkflowSteps().add(workflow.getWorkflowSteps().get(0));
		}
		fieldRepository.save(field);
		return field;
	}

	public void updateField(Field field) {
		if (field.getId() !=null) {
			updateField(field.getId(), field.getDescription(), field.getType(), field.getFavorisable(), field.getRequired(), field.getReadOnly(), field.getExtValueServiceName(), field.getExtValueType(), field.getExtValueReturn(), field.getSearchServiceName(), field.getSearchType(), field.getSearchReturn(), field.getStepZero(), field.getWorkflowSteps().stream().map(WorkflowStep::getId).collect(Collectors.toList()));
		}else {
			throw new RuntimeException("error on update field");
		}
	}

	@Transactional
	public void updateField(Long id, String description, FieldType fieldType, Boolean favorisable, Boolean required, Boolean readOnly, String extValueServiceName, String extValueType,
							String extValueReturn, String searchServiceName, String searchType, String searchReturn, Boolean stepZero, List<Long> workflowStepsIds) {
		Field field = fieldRepository.findById(id).get();
		List<WorkflowStep> workflowSteps = new ArrayList<>();
		if(workflowStepsIds != null) {
			for (Long workflowStepId : workflowStepsIds) {
				workflowSteps.add(workflowStepRepository.findById(workflowStepId).get());
			}
		}
		setFieldValues(description, fieldType, favorisable, required, readOnly, extValueServiceName, extValueType, extValueReturn, searchServiceName, searchType, searchReturn, stepZero, workflowSteps, field);
	}

	public void setFieldValues(String description, FieldType fieldType, Boolean favorisable, Boolean required, Boolean readOnly, String extValueServiceName, String extValueType, String extValueReturn, String searchServiceName, String searchType, String searchReturn, Boolean stepZero, List<WorkflowStep>workflowSteps, Field field) {
		field.setDescription(description);
		field.setType(fieldType);
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
		for(WorkflowStep workflowStep : workflowSteps) {
			if (!field.getWorkflowSteps().contains(workflowStep)) {
				field.getWorkflowSteps().add(workflowStep);
			}
		}
	}

	@Transactional
	public void deleteField(Long fieldId, Long formId) {
		Form form = formRepository.findById(formId).get();
		Field field = getById(fieldId);
		form.getFields().remove(field);
		field.getWorkflowSteps().clear();
		fieldRepository.delete(field);
	}

    public List<Field> getFieldsByWorkflowStep(WorkflowStep workflowStep) {
		return fieldRepository.findByWorkflowStepsContains(workflowStep);
    }
}
