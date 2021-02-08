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
import org.esupportail.esupsignature.repository.FormRepository;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
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
	private UserPropertieService userPropertieService;

	@Resource
	private UserService userService;

	@Resource
	private FileService fileService;

	public Form getById(Long formId) {
		Form obj = formRepository.findById(formId).get();
		return obj;
	}

	public List<Form> getFormsByUser(String userEppn, String authUserEppn){
		User user = userService.getByEppn(userEppn);
		List<Form> forms = new ArrayList<>();
		if(userEppn.equals(authUserEppn)) {
			forms = formRepository.findAuthorizedFormByRoles(user.getRoles());
		} else {
			List<UserShare> userShares = userShareService.getUserShares(userEppn, Collections.singletonList(authUserEppn), ShareType.create);
			for(UserShare userShare : userShares) {
				if(userShare.getForm() != null){
					forms.add(userShare.getForm());
				}
			}
		}
		return forms;
	}

	@Transactional
	public Form generateForm(MultipartFile multipartFile, String name, String title, String workflowType, String prefillType, String roleName, DocumentIOType targetType, String targetUri, Boolean publicUsage) throws IOException {
		Document document = documentService.createDocument(multipartFile.getInputStream(), multipartFile.getOriginalFilename(), multipartFile.getContentType());
		Form form = createForm(document, name, title, workflowType, prefillType, roleName, targetType, targetUri, publicUsage);
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
		return formRepository.findDistinctByAuthorizedShareTypesIsNotNull();
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

		List<UserShare> userShares = userShareService.getUserSharesByForm(form);
		for(UserShare userShare : userShares) {
			userShare.getShareTypes().removeIf(shareType -> !shareTypes.contains(shareType));
		}
		formRepository.save(form);
	}

	@Transactional
	public void updateFormModel(Long id, MultipartFile multipartModel) {
		Form form = getById(id);
		if(multipartModel != null) {
			Document document = form.getDocument();
			form.setDocument(null);
			documentService.delete(document.getId());
			try {
				form.setDocument(documentService.createDocument(multipartModel.getInputStream(), multipartModel.getOriginalFilename(), multipartModel.getContentType()));
			} catch (IOException e) {
				logger.error("unable to modif model", e);
			}
		}
	}

	public void deleteForm(Long formId) {
		Form form = formRepository.findById(formId).get();
		List<UserShare> userShares = userShareService.getUserSharesByForm(form);
		for(UserShare userShare : userShares) {
			userShareService.delete(userShare);
		}
		dataService.nullifyForm(form);
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

	@Transactional
	public Form createForm(Document document, String name, String title, String workflowType, String prefillType, String roleName, DocumentIOType targetType, String targetUri, Boolean publicUsage, String... fieldNames) throws IOException {
		List<Form> testForms = formRepository.findFormByNameAndActiveVersion(name, true);
		Form form = new Form();
		form.setName(name);
		form.setTitle(title);
		form.setActiveVersion(true);
		if (document == null && fieldNames.length > 0) {
			for(String fieldName : fieldNames) {
				form.getFields().add(fieldService.createField(fieldName));
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
				Field field = fieldService.createField(pdField.getPartialName());
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
				Field field = fieldService.createField(pdField.getPartialName());
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
					Field field = fieldService.createField(pdField.getPartialName());
					field.setType(FieldType.radio);
					field.setRequired(pdField.isRequired());
					field.setReadOnly(pdField.isReadOnly());
					COSName labelCOSName = (COSName) pdAnnotationWidget.getAppearance().getNormalAppearance().getSubDictionary().keySet().toArray()[0];
					field.setLabel(labelCOSName.getName());
					parseField(field, pdField, pdAnnotationWidget, page);
					fields.add(field);
				}
			} else if(pdField instanceof PDChoice) {
				Field field = fieldService.createField(pdField.getPartialName());
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
		List<Field> fieldsOrdered = new LinkedList<>();
		int i=0;
		for(Field field : fields) {
			i++;
			field.setFillOrder(i);
			fieldService.updateField(field);
			fieldsOrdered.add(field);
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
		return formRepository.findFormByNameAndActiveVersion(name, activeVersion);
	}

	public List<Form> getFormByName(String name) {
		return formRepository.findFormByName(name);
	}


	public List<Form> getFormByManagersContains(String email) {
		return formRepository.findFormByManagersContains(email);
	}

	public String getHelpMessage(User user, Form form) {
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
}
