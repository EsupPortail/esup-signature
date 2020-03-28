package org.esupportail.esupsignature.service;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.interactive.action.PDAnnotationAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.FieldType;
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
	private UserService userService;

	@Resource
	private UserShareRepository userShareRepository;

	@Resource
	private FieldService fieldService;

	@Resource
	private UserPropertieRepository userPropertiesRepository;

	@Resource
	private DataRepository dataRepository;

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
	
	public List<Form> getFormsByUser(User user){
		List<Form> forms = new ArrayList<>();
		if(user.equals(userService.getUserFromAuthentication())) {
			forms = formRepository.findAutorizedFormByUser(userService.getCurrentUser());
		} else {
			for(UserShare userShare : userShareRepository.findByUserAndToUsersAndShareType(user, Arrays.asList(userService.getUserFromAuthentication()), UserShare.ShareType.create)) {
				forms.add(userShare.getForm());
			}
		}
		return forms;
	}

	public List<Form> getAllForms(){
		List<Form> list = new ArrayList<Form>();
		formRepository.findAll().forEach(e -> list.add(e));
		return list;
	}

	public void updateForm(Form form) {
		formRepository.save(form);
	}
	
	public void deleteForm(Long formId) {
		Form form = formRepository.findById(formId).get();
		List<Data> datas = dataRepository.findByForm(form);
		for(Data data : datas) {
			data.setForm(null);
			dataRepository.save(data);
		}
		List<UserPropertie> userProperties = userPropertiesRepository.findByForm(form);
		for(UserPropertie userPropertie : userProperties) {
			userPropertiesRepository.delete(userPropertie);
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

	public Form createForm(Document document, String name, String workflowType, String code, DocumentIOType targetType, String targetUri) throws IOException {
		List<Form> testForms = formRepository.findFormByNameAndActiveVersion(name, true);
		PDDocument pdDocument = PDDocument.load(document.getInputStream());
		PDFieldTree pdFields = pdfService.getFields(pdDocument);
		PDDocumentCatalog docCatalog = pdDocument.getDocumentCatalog();
		Map<COSDictionary, Integer> pageNrByAnnotDict = getPageNrByAnnotDict(docCatalog);
		Form form = new Form();
		form.setName(name);
		form.setActiveVersion(true);
		if(testForms.size() == 1) {
			testForms.get(0).setActiveVersion(false);
			formRepository.save(testForms.get(0));
			form.setVersion(testForms.get(0).getVersion() + 1);
		} else {
			form.setVersion(1);
		}
		form.setDocument(document);
		form.setTargetType(targetType);
		form.setTargetUri(targetUri);
		form.setRole(code.toUpperCase());
		form.setPreFillType(code.toLowerCase());
		form.setWorkflowType(workflowType);
		List<Field> fields = new ArrayList<>();
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
				this.resolveFieldName(field, pdField.getPartialName());
				field.setLabel(pdField.getAlternateFieldName());
				field.setPage(page);
				PDTextField pdTextField = (PDTextField) pdField;
				field.setRequired(pdTextField.isRequired());
				PDAnnotationWidget pdAnnotationWidget = pdField.getWidgets().get(0);
				PDAnnotationAdditionalActions pdAnnotationAdditionalActions = pdAnnotationWidget.getActions();
				String type = "text";
				if(pdAnnotationAdditionalActions != null && pdAnnotationAdditionalActions.getCOSObject().size() > 0) {
					COSString cosString = (COSString) pdAnnotationAdditionalActions.getCOSObject().getCOSObject(COSName.K).getItem(COSName.JS);
					type = cosString.toString();
				}
				logger.info(type);
				if(type.equals("text")) {
					field.setType(FieldType.text);
				} else if(type.contains("Time")) {
					field.setType(FieldType.time);
				} else if(type.contains("Date")) {
					field.setType(FieldType.date);
				} else if(type.contains("Number")) {
					field.setType(FieldType.number);
				}

				field.setTopPos((int) (pdAnnotationWidget.getRectangle().getLowerLeftY() + pdField.getWidgets().get(0).getRectangle().getHeight()));
				field.setLeftPos((int) (pdAnnotationWidget.getRectangle().getLowerLeftX()));
				field.setWidth((int) pdAnnotationWidget.getRectangle().getWidth());
				field.setHeight((int) pdAnnotationWidget.getRectangle().getHeight());
				fieldService.updateField(field);
				fields.add(field);
	        } else if(pdField instanceof PDCheckBox){
				Field field = new Field();
				this.resolveFieldName(field, pdField.getPartialName());
				field.setLabel(pdField.getAlternateFieldName());
				field.setPage(page);
				field.setType(FieldType.checkbox);
				PDAnnotationWidget pdAnnotationWidget = pdField.getWidgets().get(0);
				field.setTopPos((int) (pdAnnotationWidget.getRectangle().getLowerLeftY() + pdField.getWidgets().get(0).getRectangle().getHeight()));
				field.setLeftPos((int) (pdAnnotationWidget.getRectangle().getLowerLeftX()));
				field.setWidth((int) pdAnnotationWidget.getRectangle().getWidth());
				field.setHeight((int) pdAnnotationWidget.getRectangle().getHeight());
				fieldService.updateField(field);
				fields.add(field);
			} else if(pdField instanceof PDRadioButton){
				List<PDAnnotationWidget> pdAnnotationWidgets = pdField.getWidgets();
				for(PDAnnotationWidget pdAnnotationWidget : pdAnnotationWidgets) {
					Field field = new Field();
					this.resolveFieldName(field, pdField.getPartialName());
					COSName labelCOSName = (COSName) pdAnnotationWidget.getAppearance().getNormalAppearance().getSubDictionary().keySet().toArray()[0];
					field.setLabel(labelCOSName.getName());
					field.setPage(page);
					field.setType(FieldType.radio);
					field.setTopPos((int) (pdAnnotationWidget.getRectangle().getLowerLeftY() + pdField.getWidgets().get(0).getRectangle().getHeight()));
					field.setLeftPos((int) (pdAnnotationWidget.getRectangle().getLowerLeftX()));
					field.setWidth((int) pdAnnotationWidget.getRectangle().getWidth());
					field.setHeight((int) pdAnnotationWidget.getRectangle().getHeight());
					fieldService.updateField(field);
					fields.add(field);
				}
			}
		}
		fields = fields.stream().sorted(Comparator.comparingInt(Field::getLeftPos)).sorted(Comparator.comparingInt(Field::getTopPos).reversed()).collect(Collectors.toList());
		int i=0;
		for(Field field : fields) {
			i++;
			field.setFillOrder(i);
			fieldService.updateField(field);
		}
		form.setFields(fields);
		formRepository.save(form);
		document.setParentId(form.getId());
		return form;
	}

	private void resolveFieldName(Field field, String name) {
		String[] nameValues = name.split("(?=>|\\$|#|!)");
		field.setName(nameValues[0]);
		if(nameValues.length > 1) {
			field.setStepNumbers("");
			for (int i = 1; i < nameValues.length; i++) {
				if (nameValues[i].contains("$")) {
					field.setExtValue(nameValues[i].replace("$", ""));
				} else if (nameValues[i].contains("#")) {
					field.setStepNumbers(field.getStepNumbers() + nameValues[i]);
				} else if (nameValues[i].contains("!")) {
					field.setEppnEditRight(nameValues[i].replace("!", ""));
				}
			}
		}
	}
}
