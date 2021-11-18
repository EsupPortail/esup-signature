package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
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
import org.esupportail.esupsignature.repository.FormRepository;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.sql.SQLException;
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
	private WorkflowService workflowService;

	@Resource
	private DataService dataService;

	@Resource
	private DocumentService documentService;

	@Resource
	private FieldPropertieService fieldPropertieService;

	@Resource
	private UserService userService;

	@Resource
	private FileService fileService;

	@Resource
	private SignRequestParamsService signRequestParamsService;

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
		return new ArrayList<>(forms).stream().sorted(Comparator.comparingLong(Form::getId)).collect(Collectors.toList());
	}

	@Transactional
	public Form generateForm(MultipartFile multipartFile, String name, String title, Long workflowId, String prefillType, List<String> roleNames, Boolean publicUsage) throws IOException, EsupSignatureException {
		Workflow workflow = workflowService.getById(workflowId);
		byte[] bytes = multipartFile.getInputStream().readAllBytes();
		Document document = documentService.createDocument(new ByteArrayInputStream(bytes), multipartFile.getOriginalFilename(), multipartFile.getContentType());
		Form form = createForm(document, name, title, workflow, prefillType, roleNames, publicUsage);
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
	public void updateForm(Long id, Form updateForm, List<String> managers, String[] types) {
		Form form = getById(id);
		form.setPdfDisplay(updateForm.getPdfDisplay());
		form.getManagers().clear();
		if(managers != null) {
			form.getManagers().addAll(managers);
		}
		form.setName(updateForm.getName());
		form.setTitle(updateForm.getTitle());
		form.getRoles().clear();
		form.getRoles().addAll(updateForm.getRoles());
		form.setPreFillType(updateForm.getPreFillType());
		if(updateForm.getWorkflow() == null) {
			for(Field field : form.getFields()) {
				field.getWorkflowSteps().clear();
			}
		}
		form.setWorkflow(updateForm.getWorkflow());
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
			documentService.delete(oldModel.getId());
			try {
				File tempDocument = fileService.inputStreamToTempFile(multipartModel.getInputStream(), multipartModel.getOriginalFilename());
				List<Field> fields = getFields(new FileInputStream(tempDocument), form.getWorkflow());
				for(Field field : fields) {
					if(form.getFields().stream().noneMatch(field1 -> field1.getName().equals(field.getName()))) {
						form.getFields().add(field);
					}
				}
				updateSignRequestParams(id, new FileInputStream(tempDocument));
				Document newModel = documentService.createDocument(pdfService.removeSignField(new FileInputStream(tempDocument)), multipartModel.getOriginalFilename(), multipartModel.getContentType());
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
			dataService.nullifyForm(form);
			List<Long> fieldToDelete = form.getFields().stream().map(Field::getId).collect(Collectors.toList());
			form.getFields().clear();
			for (Long fieldId : fieldToDelete) {
				List<FieldPropertie> fieldProperties = fieldPropertieService.getFieldPropertie(fieldId);
				for (FieldPropertie fieldPropertie : fieldProperties) {
					fieldPropertieService.delete(fieldPropertie.getId());
				}
				fieldService.deleteField(fieldId);
			}
			formRepository.delete(form);
		} else {
			form.setDeleted(true);
		}
	}

	@Transactional
	public Form createForm(Document document, String name, String title, Workflow workflow, String prefillType, List<String> roleNames, Boolean publicUsage, String... fieldNames) throws IOException, EsupSignatureException {
		List<Form> testForms = formRepository.findFormByNameAndActiveVersionAndDeletedNot(name, true, true);
		Form form = new Form();
		form.setName(name);
		form.setTitle(title);
		form.setActiveVersion(true);
		if (document == null && fieldNames.length > 0) {
			for(String fieldName : fieldNames) {
				form.getFields().add(fieldService.createField(fieldName, workflow));
			}
		} else {
			if (testForms.size() == 1) {
				testForms.get(0).setActiveVersion(false);
				formRepository.save(testForms.get(0));
				form.setVersion(testForms.get(0).getVersion() + 1);
				form.getFields().addAll(testForms.get(0).getFields());
			} else {
				form.setVersion(1);
				form.setFields(getFields(document.getInputStream(), workflow));
			}
		}
		form.setDocument(document);
		form.getRoles().clear();
		if(roleNames == null) roleNames = new ArrayList<>();
		form.getRoles().addAll(roleNames);
		form.setPreFillType(prefillType);
		form.setWorkflow(workflow);
		if(publicUsage == null) publicUsage = false;
		form.setPublicUsage(publicUsage);
		document.setParentId(form.getId());
		if(testForms.size() == 1) {
			List<UserShare> userShares = userShareService.getUserSharesByForm(testForms.get(0));
			for (UserShare userShare : userShares) {
				userShare.setForm(form);
			}
		}
		formRepository.save(form);
		return form;
	}

	private List<Field> getFields(InputStream inputStream, Workflow workflow) throws IOException, EsupSignatureException {
		List<Field> fields = new ArrayList<>();
		List<Field> fieldsOrdered = new LinkedList<>();
		PDDocument pdDocument = PDDocument.load(inputStream);
		PDFieldTree pdFields = pdfService.getFields(pdDocument);
		if(pdFields != null) {
			PDDocumentCatalog pdDocumentDocumentCatalog = pdDocument.getDocumentCatalog();
			Map<COSDictionary, Integer> pageNrByAnnotDict = pdfService.getPageNumberByAnnotDict(pdDocumentDocumentCatalog);
			for (PDField pdField : pdFields) {
				logger.info(pdField.getFullyQualifiedName());
				List<PDAnnotationWidget> kids = pdField.getWidgets();
				int page = 1;
				if (kids != null) {
					for (COSObjectable kid : kids) {
						COSBase kidObject = kid.getCOSObject();
						if (kidObject instanceof COSDictionary)
							page = pageNrByAnnotDict.get(kidObject);
					}
				}
				if (pdField instanceof PDTextField) {
					Field field = fieldService.createField(pdField.getPartialName(), workflow);
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
					logger.info("added");
				} else if (pdField instanceof PDCheckBox) {
					Field field = fieldService.createField(pdField.getPartialName(), workflow);
					field.setRequired(pdField.isRequired());
					field.setReadOnly(pdField.isReadOnly());
					field.setType(FieldType.checkbox);
					field.setLabel(pdField.getAlternateFieldName());
					PDAnnotationWidget pdAnnotationWidget = pdField.getWidgets().get(0);
					parseField(field, pdField, pdAnnotationWidget, page);
					fields.add(field);
					logger.info("added");
				} else if (pdField instanceof PDRadioButton) {
					List<PDAnnotationWidget> pdAnnotationWidgets = pdField.getWidgets();
					for (PDAnnotationWidget pdAnnotationWidget : pdAnnotationWidgets) {
						Field field = fieldService.createField(pdField.getPartialName(), workflow);
						field.setType(FieldType.radio);
						field.setRequired(pdField.isRequired());
						field.setReadOnly(pdField.isReadOnly());
//					COSName labelCOSName = (COSName) pdAnnotationWidget.getAppearance().getNormalAppearance().getSubDictionary().keySet().toArray()[0];
						field.setLabel(pdField.getAlternateFieldName());
						parseField(field, pdField, pdAnnotationWidget, page);
						fields.add(field);
						logger.info("added");
					}
				} else if (pdField instanceof PDChoice) {
					Field field = fieldService.createField(pdField.getPartialName(), workflow);
					field.setType(FieldType.select);
					field.setRequired(pdField.isRequired());
					field.setReadOnly(pdField.isReadOnly());
					PDAnnotationWidget pdAnnotationWidget = pdField.getWidgets().get(0);
					field.setLabel(pdField.getAlternateFieldName());
					parseField(field, pdField, pdAnnotationWidget, page);
					fields.add(field);
					logger.info("added");
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


	public List<Form> getFormByManagersContains(String eppn) {
		User user = userService.getUserByEppn(eppn);
		return formRepository.findFormByManagersContainsAndDeletedIsNullOrDeletedIsFalse(user.getEmail());
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
	public Map<String, Object> getModel(Long id) throws SQLException, IOException {
		Form form = getById(id);
		Document attachement = documentService.getById(form.getDocument().getId());
		if (attachement != null) {
			return fileService.getFileResponse(attachement.getBigFile().getBinaryFile().getBinaryStream().readAllBytes(), attachement.getFileName(), attachement.getContentType());
		}
		return null;
	}

	public List<Form> getByRoles(String role) {
		return formRepository.findByRolesIn(Collections.singletonList(role));
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
    public InputStream getJsonFormSetup(Long id) throws IOException {
		Form form = getById(id);
		File jsonFile = fileService.getTempFile("json");
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.writer().writeValue(jsonFile, form);
		return new FileInputStream(jsonFile);
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
		updateForm(id, formSetup, formSetup.getManagers(), formSetup.getAuthorizedShareTypes().stream().map(Enum::name).collect(Collectors.toList()).toArray(String[]::new));
		return;
	}
}
