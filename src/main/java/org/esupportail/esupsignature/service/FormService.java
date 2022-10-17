package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.action.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.PDAnnotationAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.action.PDFormFieldAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.FieldType;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.repository.FormRepository;
import org.esupportail.esupsignature.repository.WorkflowRepository;
import org.esupportail.esupsignature.service.utils.WebUtilsService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FormService {

	private static final Logger logger = LoggerFactory.getLogger(FormService.class);

	@Resource
	private FormRepository formRepository;

	@Resource
	private PdfService pdfService;

	@Resource
	private UserShareService userShareService;

	@Resource
	private FieldService fieldService;

	@Resource
	private WorkflowRepository workflowRepository;

	@Resource
	private DocumentService documentService;

	@Resource
	private FieldPropertieService fieldPropertieService;

	@Resource
	private UserService userService;

	@Resource
	private SignRequestParamsService signRequestParamsService;

	@Resource
	private DataRepository dataRepository;

	@Resource
	private WebUtilsService webUtilsService;

	public Form getById(Long formId) {
		Form obj = formRepository.findById(formId).get();
		return obj;
	}

	public List<Form> getFormsByUser(String userEppn, String authUserEppn){
		User user = userService.getByEppn(userEppn);
		Set<Form> forms = new HashSet<>();
		if(userEppn.equals(authUserEppn)) {
			for(String role : user.getRoles()) {
				forms.addAll(formRepository.findAuthorizedForms(role));
			}
		} else {
			List<UserShare> userShares = userShareService.getUserShares(userEppn, Collections.singletonList(authUserEppn), ShareType.create);
			for(UserShare userShare : userShares) {
				if(userShare.getForm() != null && !userShare.getForm().getDeleted()){
					forms.add(userShare.getForm());
				}
			}
		}
		for(Form form : forms) {
			form.setMessageToDisplay(getHelpMessage(userEppn, form));
		}
		return new ArrayList<>(forms).stream().sorted(Comparator.comparingLong(Form::getId)).collect(Collectors.toList());
	}

	@Transactional
	public Form generateForm(MultipartFile multipartFile, String name, String title, Long workflowId, String prefillType, List<String> roleNames, Boolean publicUsage) throws IOException, EsupSignatureException {
		byte[] bytes = multipartFile.getInputStream().readAllBytes();
		Document document = documentService.createDocument(new ByteArrayInputStream(bytes), multipartFile.getOriginalFilename(), multipartFile.getContentType());
		Form form = createForm(document, name, title, workflowId, prefillType, roleNames, publicUsage, null, null);
		try {
			updateSignRequestParams(form.getId(), new ByteArrayInputStream(bytes));
		} catch (EsupSignatureIOException e) {
			logger.error(e.getMessage(), e);
		}
		return form;
	}

	@Transactional
	public Boolean isFormAuthorized(String userEppn, String authUserEppn, Long id) {
		Form form = getById(id);
		return getFormsByUser(userEppn, authUserEppn).contains(form) && userShareService.checkFormShare(userEppn, authUserEppn, ShareType.create, form);
	}

	public List<Form> getAllForms(){
		List<Form> list = new ArrayList<>();
		formRepository.findAll().forEach(e -> list.add(e));
		return list;
	}

	public List<Form> getAuthorizedToShareForms() {
		List<Form> forms = formRepository.findDistinctByAuthorizedShareTypesIsNotNullAndDeletedIsNullOrDeletedIsFalse();
		forms = forms.stream().filter(form -> form.getAuthorizedShareTypes().size() > 0).collect(Collectors.toList());
		return forms;
	}

	@Transactional
	public void updateForm(Long id, Form updateForm, String[] types, boolean updateWorkflow) {
		Form form = getById(id);
		form.setPdfDisplay(updateForm.getPdfDisplay());
		form.setName(updateForm.getName());
		form.setTitle(updateForm.getTitle());
		form.getRoles().clear();
		form.getRoles().addAll(updateForm.getRoles());
		form.setPreFillType(updateForm.getPreFillType());
		if(updateWorkflow) {
			if(updateForm.getWorkflow() == null) {
				for(Field field : form.getFields()) {
					field.getWorkflowSteps().clear();
				}
			}
			form.setWorkflow(updateForm.getWorkflow());
		}
		form.setDescription(updateForm.getDescription());
		form.setMessage(updateForm.getMessage());
		form.setPublicUsage(updateForm.getPublicUsage());
		form.setAction(updateForm.getAction());
		form.getAuthorizedShareTypes().clear();
		form.setActiveVersion(updateForm.getActiveVersion());
		List<ShareType> shareTypes = new ArrayList<>();
		if(types != null) {
			for (String type : types) {
				ShareType shareType = ShareType.valueOf(type);
				form.getAuthorizedShareTypes().add(shareType);
				shareTypes.add(shareType);
			}
		}

		List<UserShare> userShares = userShareService.getUserSharesByForm(form);
		for(UserShare userShare : userShares) {
			userShare.getShareTypes().removeIf(shareType -> !shareTypes.contains(shareType));
		}
//		formRepository.save(form);
	}

	@Transactional
	public void updateFormModel(Long id, MultipartFile multipartModel) throws EsupSignatureException {
		Form form = getById(id);
		if(multipartModel != null) {
			Document oldModel = form.getDocument();
			form.setDocument(null);
			if(oldModel != null) {
				documentService.delete(oldModel.getId());
			}
			try {
				byte[] tempDocument = multipartModel.getInputStream().readAllBytes();
				List<Field> fields = getFields(new ByteArrayInputStream(tempDocument), form.getWorkflow());
				for(Field field : fields) {
					if(form.getFields().stream().noneMatch(field1 -> field1.getName().equals(field.getName()))) {
						form.getFields().add(field);
					}
				}
				updateSignRequestParams(id, new ByteArrayInputStream(tempDocument));
				Document newModel = documentService.createDocument(pdfService.removeSignField(new ByteArrayInputStream(tempDocument)), multipartModel.getOriginalFilename(), multipartModel.getContentType());
				form.setDocument(newModel);

			} catch (IOException | EsupSignatureIOException e) {
				logger.error("unable to modif model", e);
			}
		}
	}

	@Transactional
	public void deleteForm(Long formId) {
		Form form = formRepository.findById(formId).get();
		if(form.getDeleted() != null && form.getDeleted()) {
			List<UserShare> userShares = userShareService.getUserSharesByForm(form);
			for (UserShare userShare : userShares) {
				userShareService.delete(userShare);
			}
			nullifyForm(form);
			List<Long> fieldToDelete = form.getFields().stream().map(Field::getId).collect(Collectors.toList());
			form.getFields().clear();
			for (Long fieldId : fieldToDelete) {
				List<FieldPropertie> fieldProperties = fieldPropertieService.getFieldPropertie(fieldId);
				for (FieldPropertie fieldPropertie : fieldProperties) {
					fieldPropertieService.delete(fieldPropertie.getId());
				}
				List<Form> forms = formRepository.findByFieldsContaining(fieldService.getById(fieldId));
				if(forms.size() == 0) {
					fieldService.deleteField(fieldId, formId);
				}
			}
			formRepository.delete(form);
		} else {
			form.setDeleted(true);
		}
	}

	@Transactional
	public Form createForm(Document document, String name, String title, Long workflowId, String prefillType, List<String> roleNames, Boolean publicUsage, String[] fieldNames, String[] fieldTypes) throws IOException, EsupSignatureException {
		Workflow workflow = workflowRepository.findById(workflowId).get();
		Form form = new Form();
		form.setName(name);
		form.setTitle(title);
		form.setActiveVersion(true);
		form.setVersion(1);
		if (document == null) {
			if(fieldNames != null && fieldNames.length > 0) {
				int i = 0;
				for (String fieldName : fieldNames) {
					if (fieldTypes != null && fieldTypes.length > 0) {
						form.getFields().add(fieldService.createField(fieldName, workflow, FieldType.valueOf(fieldTypes[i])));
					} else {
						form.getFields().add(fieldService.createField(fieldName, workflow, FieldType.text));
					}
					i++;
				}
			}
		} else {
			form.setFields(getFields(document.getInputStream(), workflow));
		}
		form.setDocument(document);
		form.getRoles().clear();
		if(roleNames == null) roleNames = new ArrayList<>();
		form.getRoles().addAll(roleNames);
		form.setPreFillType(prefillType);
		form.setWorkflow(workflow);
		if(publicUsage == null) publicUsage = false;
		form.setPublicUsage(publicUsage);
		if(document != null) {
			document.setParentId(form.getId());
		}
		if(fieldTypes != null) {
			form.setPdfDisplay(false);
		}
		formRepository.save(form);
		return form;
	}

	@Transactional
	public void addField(Long formId, String[] fieldNames, String[] fieldTypes) {
		Form form = getById(formId);
		if(fieldNames != null && fieldNames.length > 0) {
			int i = 0;
			for (String fieldName : fieldNames) {
				if (fieldTypes != null && fieldTypes.length > 0) {
					form.getFields().add(fieldService.createField(fieldName, form.getWorkflow(), FieldType.valueOf(fieldTypes[i])));
				} else {
					form.getFields().add(fieldService.createField(fieldName, form.getWorkflow(), FieldType.text));
				}
				i++;
			}
		}
	}

	private List<Field> getFields(InputStream inputStream, Workflow workflow) throws IOException, EsupSignatureException {
		List<Field> fields = new ArrayList<>();
		List<Field> fieldsOrdered = new LinkedList<>();
		PDDocument pdDocument = PDDocument.load(inputStream);
		PDFieldTree pdFields = pdfService.getFields(pdDocument);
		if(pdFields != null) {
			Map<String, Integer> pageNrByAnnotDict = pdfService.getPageNumberByAnnotDict(pdDocument);
			for (PDField pdField : pdFields) {
				logger.info(pdField.getFullyQualifiedName() + " finded");
				int page = pageNrByAnnotDict.get(pdField.getPartialName());
				if (pdField instanceof PDTextField) {
					Field field = fieldService.createField(pdField.getPartialName(), workflow, FieldType.text);
					field.setLabel(pdField.getAlternateFieldName());
					field.setRequired(pdField.isRequired());
					field.setReadOnly(pdField.isReadOnly());
					PDFormFieldAdditionalActions pdFormFieldAdditionalActions = pdField.getActions();
					String type = "text";
					if (pdFormFieldAdditionalActions != null) {
						if (pdFormFieldAdditionalActions.getK() != null) {
							type = pdFormFieldAdditionalActions.getK().getType();
						}
						PDAction pdAction = pdFormFieldAdditionalActions.getC();
						if (pdAction != null) {
							String actionsString = pdAction.getCOSObject().getString(COSName.JS);
							if (actionsString != null) {
								computeActions(field, actionsString);
							}
						}
					}
					PDAnnotationWidget pdAnnotationWidget = pdField.getWidgets().get(0);
					PDAnnotationAdditionalActions pdAnnotationAdditionalActions = pdAnnotationWidget.getActions();
					if (pdAnnotationAdditionalActions != null && pdAnnotationAdditionalActions.getCOSObject().size() > 0) {
						if (pdAnnotationAdditionalActions.getCOSObject().getCOSObject(COSName.K) != null) {
							COSString cosString = (COSString) pdAnnotationAdditionalActions.getCOSObject().getCOSObject(COSName.K).getItem(COSName.JS);
							type = cosString.toString();
						}
					}
					if (type.equals("text")) {
						field.setType(FieldType.text);
					} else if (type.contains("Time")) {
						field.setType(FieldType.time);
					} else if (type.contains("Date")) {
						field.setType(FieldType.date);
					} else if (type.contains("Number")) {
						field.setType(FieldType.number);
					}
					parseField(field, pdField, pdAnnotationWidget, page);
					fields.add(field);
					logger.info(field.getName() + " added");
				} else if (pdField instanceof PDCheckBox) {
					Field field = fieldService.createField(pdField.getPartialName(), workflow, FieldType.checkbox);
					field.setRequired(pdField.isRequired());
					field.setReadOnly(pdField.isReadOnly());
					field.setLabel(pdField.getAlternateFieldName());
					PDAnnotationWidget pdAnnotationWidget = pdField.getWidgets().get(0);
					parseField(field, pdField, pdAnnotationWidget, page);
					fields.add(field);
					logger.info(field.getName() + " added");
				} else if (pdField instanceof PDRadioButton) {
					PDRadioButton pdRadioButton = (PDRadioButton) pdField;
					for (PDAnnotationWidget pdAnnotationWidget : pdRadioButton.getWidgets()) {
						if(pdAnnotationWidget.getRectangle() == null) {
							continue;
						}
						Field field = fieldService.createField(pdField.getPartialName(), workflow, FieldType.radio);
						field.setRequired(pdField.isRequired());
						field.setReadOnly(pdField.isReadOnly());
						field.setLabel(pdField.getAlternateFieldName());
						parseField(field, pdField, pdAnnotationWidget, page);
						fields.add(field);
						logger.info(field.getName() + " added");
						break;
					}
				} else if (pdField instanceof PDChoice) {
					Field field = fieldService.createField(pdField.getPartialName(), workflow, FieldType.select);
					field.setRequired(pdField.isRequired());
					field.setReadOnly(pdField.isReadOnly());
					PDAnnotationWidget pdAnnotationWidget = pdField.getWidgets().get(0);
					field.setLabel(pdField.getAlternateFieldName());
					parseField(field, pdField, pdAnnotationWidget, page);
					fields.add(field);
					logger.info(field.getName() + " added");
				}
			}
			fields = fields.stream().sorted(Comparator.comparingInt(Field::getLeftPos)).sorted(Comparator.comparingInt(Field::getTopPos).reversed()).sorted(Comparator.comparingInt(Field::getPage)).collect(Collectors.toList());
			int i = 0;
			for (Field field : fields) {
				i++;
				field.setFillOrder(i);
				fieldService.updateField(field);
				fieldsOrdered.add(field);
			}
		}
		pdDocument.close();
		return fieldsOrdered;
	}

	private void parseField(Field field, PDField pdField, PDAnnotationWidget pdAnnotationWidget, int page) {
		field.setPage(page);
		field.setTopPos((int) (pdAnnotationWidget.getRectangle().getLowerLeftY() + pdField.getWidgets().get(0).getRectangle().getHeight()));
		field.setLeftPos((int) (pdAnnotationWidget.getRectangle().getLowerLeftX()));
		fieldService.updateField(field);
	}

	private void computeActions(Field field, String actionsString) {
		String[] actionsStrings = actionsString.split(";");
		for(String actionString : actionsStrings) {
			String[] nameValues = actionString.trim().split("\\.|,|\\(");
			if (nameValues.length > 1) {
				if(nameValues[0].equals("prefill")) {
					field.setExtValueServiceName(nameValues[1].trim());
					field.setExtValueType(nameValues[2].trim());
					field.setExtValueReturn(nameValues[3].trim().replace(")", ""));
				} else if(nameValues[0].equals("search")) {
					field.setSearchServiceName(nameValues[1].trim());
					field.setSearchType(nameValues[2].trim());
					field.setSearchReturn(nameValues[3].trim().replace(")", ""));
				}
//				if(nameValues[0].equals("step") && nameValues[1].equals("update")) {
//					for (int i = 2; i < nameValues.length; i++) {
//						field.setStepNumbers(field.getStepNumbers() + " " + nameValues[i].replace(")", "").trim());
//					}
//				}
//				if(field.getSearchType() == null) {
//					field.setStepNumbers("0");
//				}
			}
		}
	}

	public List<Form> getFormByNameAndActiveVersion(String name, boolean activeVersion) {
		return formRepository.findFormByNameAndActiveVersionAndDeletedNot(name, activeVersion, true);
	}

	public List<Form> getFormByName(String name) {
		return formRepository.findFormByNameAndDeletedIsNullOrDeletedIsFalse(name);
	}

	@Transactional
	public List<Form> getFormByManagersContains(String eppn) {
		User user = userService.getUserByEppn(eppn);
		List<Workflow> workflows = workflowRepository.findWorkflowByManagersIn(Collections.singletonList(user.getEmail()));
		List<Form> managerForms = new ArrayList<>();
		for(Workflow workflow : workflows) {
			List<Form> form = formRepository.findByWorkflowIdEquals(workflow.getId());
			if(form != null && form.size() > 0) {
				managerForms.add(form.get(0));
			}
		}
		return managerForms;
	}

	@Transactional
	public String getHelpMessage(String userEppn, Form form) {
		User user = userService.getUserByEppn(userEppn);
		String messsage = null;
		boolean sendMessage = true;
		if(user.getFormMessages() != null) {
			String[] formMessages = user.getFormMessages().split(" ");
			if(Arrays.asList(formMessages).contains(form.getId().toString())) {
				sendMessage = false;
			}
		}
		if(sendMessage && form.getMessage() != null && !form.getMessage().isEmpty()) {
			messsage = form.getMessage();
		}
		return messsage;
	}

	@Transactional
	public boolean getModel(Long id, HttpServletResponse httpServletResponse) throws IOException {
		Form form = getById(id);
		Document attachment = documentService.getById(form.getDocument().getId());
		if (attachment != null) {
			webUtilsService.copyFileStreamToHttpResponse(attachment.getFileName(), attachment.getContentType(), attachment.getInputStream(), httpServletResponse);
			return true;
		}
		return false;
	}

	public List<Form> getByRoles(String role) {
		return formRepository.findByRolesIn(Collections.singletonList(role));
	}

	public Set<Form> getManagerForms(String userEppn) {
		User manager = userService.getByEppn(userEppn);
		Set<Form> formsManaged = new HashSet<>();
		for (String role : manager.getManagersRoles()) {
			formsManaged.addAll(formRepository.findByManagerRole(role));
		}
		return formsManaged;
	}

	@Transactional
	public void updateSignRequestParams(Long formId, InputStream inputStream) throws EsupSignatureIOException {
		Form form = getById(formId);
		List<SignRequestParams> findedSignRequestParams = signRequestParamsService.scanSignatureFields(inputStream, 0);

		form.getSignRequestParams().removeIf(signRequestParams -> findedSignRequestParams.stream().noneMatch(s -> s.getSignPageNumber().equals(signRequestParams.getSignPageNumber()) && s.getxPos().equals(signRequestParams.getxPos()) && s.getyPos().equals(signRequestParams.getyPos())));

		for(SignRequestParams signRequestParams : findedSignRequestParams) {
			if(form.getSignRequestParams().stream().noneMatch(s -> s.getSignPageNumber().equals(signRequestParams.getSignPageNumber()) && s.getxPos().equals(signRequestParams.getxPos()) && s.getyPos().equals(signRequestParams.getyPos()))) {
				form.getSignRequestParams().add(signRequestParams);
			}
		}
	}

	@Transactional
	public void setSignRequestParamsSteps(Long formId, Map<Long, Integer> signRequestParamsSteps) {
		Form form = getById(formId);
		for(WorkflowStep workflowStep : form.getWorkflow().getWorkflowSteps()) {
			workflowStep.getSignRequestParams().clear();
		}
		for (Map.Entry<Long, Integer> entry : signRequestParamsSteps.entrySet()) {
			SignRequestParams signRequestParams = signRequestParamsService.getById(entry.getKey());
			form.getWorkflow().getWorkflowSteps().get(entry.getValue() - 1).getSignRequestParams().add(signRequestParams);
		}
	}

	@Transactional
	public void addSignRequestParamsSteps(Long formId, Integer step, Integer signPageNumber, Integer xPos, Integer yPos) {
		Form form = getById(formId);
		SignRequestParams signRequestParams = signRequestParamsService.createSignRequestParams(signPageNumber, xPos, yPos);
		form.getSignRequestParams().add(signRequestParams);
		form.getWorkflow().getWorkflowSteps().get(step - 1).getSignRequestParams().add(signRequestParams);
	}

	@Transactional
	public void removeSignRequestParamsSteps(Long formId, Long id) {
		Form form = getById(formId);
		SignRequestParams signRequestParams = signRequestParamsService.getById(id);
		form.getSignRequestParams().removeIf(signRequestParams1 -> signRequestParams1.equals(signRequestParams));
		for(WorkflowStep workflowStep : form.getWorkflow().getWorkflowSteps()) {
			workflowStep.getSignRequestParams().remove(signRequestParams);
		}
		signRequestParamsService.delete(id);

	}

	@Transactional
    public InputStream getJsonFormSetup(Long id) throws IOException {
		Form form = getById(id);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.writer().writeValue(outputStream, form);
		return new ByteArrayInputStream(outputStream.toByteArray());
    }

    @Transactional
	public void setFormSetupFromJson(Long id, InputStream inputStream) throws IOException {
		Form form = getById(id);
		ObjectMapper objectMapper = new ObjectMapper();
		Form formSetup = objectMapper.readValue(inputStream.readAllBytes(), Form.class);
		for(Field field : form.getFields()) {
			Optional<Field> optFieldSetup = formSetup.getFields().stream().filter(field1 -> field1.getName().equals(field.getName())).findFirst();
			if(optFieldSetup.isPresent()) {
				Field fieldSetup = optFieldSetup.get();
				fieldService.updateField(field.getId(), fieldSetup.getDescription(), fieldSetup.getType(), fieldSetup.getFavorisable(), fieldSetup.getRequired(), fieldSetup.getReadOnly(), fieldSetup.getExtValueServiceName(), fieldSetup.getExtValueType(), fieldSetup.getExtValueReturn(), fieldSetup.getSearchServiceName(), fieldSetup.getSearchType(), fieldSetup.getSearchReturn(), fieldSetup.getStepZero(), null);
			}
		}
		formSetup.setName(form.getName());
		formSetup.setTitle(form.getTitle());
		formSetup.setWorkflow(null);
		updateForm(id, formSetup, formSetup.getAuthorizedShareTypes().stream().map(Enum::name).collect(Collectors.toList()).toArray(String[]::new), false);
	}

	public void nullifyForm(Form form) {
		List<Data> datas = dataRepository.findByFormId(form.getId());
		for(Data data : datas) {
			data.setForm(null);
		}
	}

	@Transactional
	public int getTotalPagesCount(Long id) {
		Form form = getById(id);
		return pdfService.getPdfParameters(form.getDocument().getInputStream(), 1).getTotalNumberOfPages();
	}
}
