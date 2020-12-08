package org.esupportail.esupsignature.service;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.interactive.action.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.PDAnnotationAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.action.PDFormFieldAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.FieldType;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.repository.FormRepository;
import org.esupportail.esupsignature.repository.UserPropertieRepository;
import org.esupportail.esupsignature.repository.UserShareRepository;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
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
	private UserShareRepository userShareRepository;

	@Resource
	private FieldService fieldService;

	@Resource
	private UserPropertieRepository userPropertiesRepository;

	@Resource
	private DataRepository dataRepository;

	@Resource
	private WorkflowService workflowService;

	public Form getFormById(Long formId) {
		Form obj = formRepository.findById(formId).get();
		return obj;
	}

	public Form getFormByDocument(Document document) {
		if(formRepository.findByDocument(document).size() > 0) {
			return formRepository.findByDocument(document).get(0);
		} else {
			return null;
		}
	}
	
	public List<Form> getFormsByUser(User user, User authUser){
		List<Form> authorizedForms = formRepository.findAuthorizedFormByUser(user);
		List<Form> forms = new ArrayList<>();
		if(user.equals(authUser)) {
			forms = authorizedForms;
		} else {
			for(UserShare userShare : userShareRepository.findByUserAndToUsersInAndShareTypesContains(user, Arrays.asList(authUser), ShareType.create)) {
				if(userShare.getForm() != null && authorizedForms.contains(userShare.getForm())){
					forms.add(userShare.getForm());
				}
			}
		}
		return forms;
	}

	public List<Form> getAllForms(){
		List<Form> list = new ArrayList<>();
		formRepository.findAll().forEach(e -> list.add(e));
		return list;
	}

	public List<Form> getAuthorizedToShareForms() {
		return formRepository.findDistinctByAuthorizedShareTypesIsNotNull();
	}

	public void updateForm(Long id, Form updateForm, List<String> managers, String[] types) {
		Form form = getFormById(id);
		form.setPdfDisplay(updateForm.getPdfDisplay());
		form.getManagers().clear();
		if(managers != null) {
			form.getManagers().addAll(managers);
		}
		form.setName(updateForm.getName());
		form.setTitle(updateForm.getTitle());
		form.setRole(updateForm.getRole());
		form.setPreFillType(updateForm.getPreFillType());
		form.setWorkflowType(updateForm.getWorkflowType());
		form.setTargetUri(updateForm.getTargetUri());
		form.setTargetType(updateForm.getTargetType());
		form.setDescription(updateForm.getDescription());
		form.setMessage(updateForm.getMessage());
		form.setPublicUsage(updateForm.getPublicUsage());
		form.setAction(updateForm.getAction());
		form.getAuthorizedShareTypes().clear();
		List<ShareType> shareTypes = new ArrayList<>();
		if(types != null) {
			for (String type : types) {
				ShareType shareType = ShareType.valueOf(type);
				form.getAuthorizedShareTypes().add(shareType);
				shareTypes.add(shareType);
			}
		}
		List<UserShare> userShares = userShareRepository.findByFormId(form.getId());
		for(UserShare userShare : userShares) {
			userShare.getShareTypes().removeIf(shareType -> !shareTypes.contains(shareType));
		}
		formRepository.save(form);
	}
	
	public void deleteForm(Long formId) {
		Form form = formRepository.findById(formId).get();
		List<UserShare> userShares = userShareRepository.findByForm(form);
		for(UserShare userShare : userShares) {
			userShareRepository.deleteById(userShare.getId());
		}
		List<Data> datas = dataRepository.findByForm(form);
		for(Data data : datas) {
			data.setForm(null);
			dataRepository.save(data);
		}
		for (WorkflowStep workflowStep : workflowService.getWorkflowByName(form.getWorkflowType()).getWorkflowSteps()) {
			List<UserPropertie> userProperties = userPropertiesRepository.findByWorkflowStep(workflowStep);
			for(UserPropertie userPropertie : userProperties) {
				userPropertiesRepository.delete(userPropertie);
			}
		}
		formRepository.delete(form);
	}

	private Map<COSDictionary, Integer> getPageNrByAnnotDict(PDDocumentCatalog docCatalog) throws IOException {
		Iterator<PDPage> pages = docCatalog.getPages().iterator();
		Map<COSDictionary, Integer> pageNrByAnnotDict = new HashMap<>();
		int i = 0;
		for (Iterator<PDPage> it = pages; it.hasNext(); ) {
			PDPage pdPage = it.next();
			for (PDAnnotation annotation : pdPage.getAnnotations()) {
				pageNrByAnnotDict.put(annotation.getCOSObject(), i + 1);
			}
			i++;
		}
		return pageNrByAnnotDict;
	}

	public Form createForm(Document document, String name, String title, String workflowType, String prefillType, String roleName, DocumentIOType targetType, String targetUri, String... fieldNames) throws IOException {
		List<Form> testForms = formRepository.findFormByNameAndActiveVersion(name, true);
		Form form = new Form();
		form.setName(name);
		form.setTitle(title);
		form.setActiveVersion(true);
		if (document == null && fieldNames.length > 0) {
			for(String fieldName : fieldNames) {
				Field field = new Field();
				field.setName(fieldName);
				field.setLabel(fieldName);
				field.setType(FieldType.text);
				form.getFields().add(field);
			}
		} else {
			if (testForms.size() == 1) {
				testForms.get(0).setActiveVersion(false);
				formRepository.save(testForms.get(0));
				form.setVersion(testForms.get(0).getVersion() + 1);
				form.getFields().addAll(testForms.get(0).getFields());
			} else {
				form.setVersion(1);
				form.setFields(getFields(document));
			}
		}
		form.setDocument(document);
		form.setTargetType(targetType);
		form.setTargetUri(targetUri);
		form.setRole(roleName);
		form.setPreFillType(prefillType);
		form.setWorkflowType(workflowType);
		formRepository.save(form);
		document.setParentId(form.getId());
		if(testForms.size() == 1) {
			List<UserShare> userShares = userShareRepository.findByForm(testForms.get(0));
			for (UserShare userShare : userShares) {
				userShare.setForm(form);
			}
		}
		return form;
	}

	private List<Field> getFields(Document document) throws IOException {
		List<Field> fields = new ArrayList<>();
		PDDocument pdDocument = PDDocument.load(document.getInputStream());
		PDFieldTree pdFields = pdfService.getFields(pdDocument);
		PDDocumentCatalog docCatalog = pdDocument.getDocumentCatalog();
		Map<COSDictionary, Integer> pageNrByAnnotDict = getPageNrByAnnotDict(docCatalog);
		for(PDField pdField : pdFields) {
			List<PDAnnotationWidget> kids = pdField.getWidgets();
			int page = 1;
			if (kids != null) {
				for (COSObjectable kid : kids) {
					COSBase kidObject = kid.getCOSObject();
					if (kidObject instanceof COSDictionary)
						page = pageNrByAnnotDict.get(kidObject);
				}
			}
			if(pdField instanceof PDTextField){
				Field field = new Field();
				field.setLabel(pdField.getAlternateFieldName());
				field.setRequired(pdField.isRequired());
				field.setReadOnly(pdField.isReadOnly());
				PDFormFieldAdditionalActions pdFormFieldAdditionalActions = pdField.getActions();
				logger.info(pdField.getFullyQualifiedName());
				String type = "text";
				if(pdFormFieldAdditionalActions != null) {
					if(pdFormFieldAdditionalActions.getK() != null) {
						type = pdFormFieldAdditionalActions.getK().getType();
					}
					PDAction pdAction = pdFormFieldAdditionalActions.getC();
					if(pdAction != null) {
						String actionsString = pdAction.getCOSObject().getString(COSName.JS);
						if (actionsString != null) {
							computeActions(field, actionsString);
						}
					}
				}

				PDAnnotationWidget pdAnnotationWidget = pdField.getWidgets().get(0);
				PDAnnotationAdditionalActions pdAnnotationAdditionalActions = pdAnnotationWidget.getActions();
				if(pdAnnotationAdditionalActions != null && pdAnnotationAdditionalActions.getCOSObject().size() > 0) {
					if(pdAnnotationAdditionalActions.getCOSObject().getCOSObject(COSName.K) != null) {
						COSString cosString = (COSString) pdAnnotationAdditionalActions.getCOSObject().getCOSObject(COSName.K).getItem(COSName.JS);
						type = cosString.toString();
					}
				}
				if(type.equals("text")) {
					field.setType(FieldType.text);
				} else if(type.contains("Time")) {
					field.setType(FieldType.time);
				} else if(type.contains("Date")) {
					field.setType(FieldType.date);
				} else if(type.contains("Number")) {
					field.setType(FieldType.number);
				}
				parseField(field, pdField, pdAnnotationWidget, page);
				fields.add(field);
	        } else if(pdField instanceof PDCheckBox) {
				Field field = new Field();
				field.setRequired(pdField.isRequired());
				field.setReadOnly(pdField.isReadOnly());
				field.setType(FieldType.checkbox);
				field.setLabel(pdField.getAlternateFieldName());
				PDAnnotationWidget pdAnnotationWidget = pdField.getWidgets().get(0);
				parseField(field, pdField, pdAnnotationWidget, page);
				fields.add(field);
			} else if(pdField instanceof PDRadioButton){
				List<PDAnnotationWidget> pdAnnotationWidgets = pdField.getWidgets();
				for(PDAnnotationWidget pdAnnotationWidget : pdAnnotationWidgets) {
					Field field = new Field();
					field.setType(FieldType.radio);
					field.setRequired(pdField.isRequired());
					field.setReadOnly(pdField.isReadOnly());
					COSName labelCOSName = (COSName) pdAnnotationWidget.getAppearance().getNormalAppearance().getSubDictionary().keySet().toArray()[0];
					field.setLabel(labelCOSName.getName());
					parseField(field, pdField, pdAnnotationWidget, page);
					fields.add(field);
				}
			} else if(pdField instanceof PDChoice) {
				Field field = new Field();
				field.setType(FieldType.select);
				field.setRequired(pdField.isRequired());
				field.setReadOnly(pdField.isReadOnly());
				PDAnnotationWidget pdAnnotationWidget = pdField.getWidgets().get(0);
				field.setLabel(pdField.getAlternateFieldName());
				parseField(field, pdField, pdAnnotationWidget, page);
				fields.add(field);
			}
		}
		fields = fields.stream().sorted(Comparator.comparingInt(Field::getLeftPos)).sorted(Comparator.comparingInt(Field::getTopPos).reversed()).collect(Collectors.toList());
		int i=0;
		for(Field field : fields) {
			i++;
			field.setFillOrder(i);
			fieldService.updateField(field);
		}
		return fields;
	}

	private void parseField(Field field, PDField pdField, PDAnnotationWidget pdAnnotationWidget, int page) {
		field.setName(pdField.getPartialName());
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
				if(nameValues[0].equals("step") && nameValues[1].equals("update")) {
					for (int i = 2; i < nameValues.length; i++) {
						field.setStepNumbers(field.getStepNumbers() + " " + nameValues[i].replace(")", "").trim());
					}
				}
				if(field.getSearchType() == null) {
					field.setStepNumbers("0");
				}
			}
		}
	}

	public List<Form> getFormByNameAndActiveVersion(String name, boolean activeVersion) {
		return formRepository.findFormByNameAndActiveVersion(name, true);
	}

	public List<Form> getFormByName(String name) {
		return formRepository.findFormByName(name);
	}
}
