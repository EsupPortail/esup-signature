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
import org.esupportail.esupsignature.entity.enums.FieldType;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
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
	private FieldPropertieService fieldPropertieService;

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
		Set<Form> forms = new HashSet<>();
		if(userEppn.equals(authUserEppn)) {
			for(String role : user.getRoles()) {
				forms.addAll(formRepository.findAuthorizedForms(role));
			}
		} else {
			List<UserShare> userShares = userShareService.getUserShares(userEppn, Collections.singletonList(authUserEppn), ShareType.create);
			for(UserShare userShare : userShares) {
				if(userShare.getForm() != null){
					forms.add(userShare.getForm());
				}
			}
		}
		return new ArrayList<>(forms).stream().sorted(Comparator.comparingLong(Form::getId)).collect(Collectors.toList());
	}

	@Transactional
	public Form generateForm(MultipartFile multipartFile, String name, String title, Long workflowId, String prefillType, List<String> roleNames, Boolean publicUsage) throws IOException, EsupSignatureException {
		Workflow workflow = workflowService.getById(workflowId);
		Document document = documentService.createDocument(multipartFile.getInputStream(), multipartFile.getOriginalFilename(), multipartFile.getContentType());
		Form form = createForm(document, name, title, workflow, prefillType, roleNames, publicUsage);
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
		form.getRoles().clear();
		form.getRoles().addAll(updateForm.getRoles());
		form.setPreFillType(updateForm.getPreFillType());
		form.setWorkflow(updateForm.getWorkflow());
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
	public void updateFormModel(Long id, MultipartFile multipartModel) throws EsupSignatureException {
		Form form = getById(id);
		if(multipartModel != null) {
			Document oldModel = form.getDocument();
			form.setDocument(null);
			documentService.delete(oldModel.getId());
			try {
				Document newModel = documentService.createDocument(multipartModel.getInputStream(), multipartModel.getOriginalFilename(), multipartModel.getContentType());
				form.setDocument(newModel);
				List<Field> fields = getFields(newModel, form.getWorkflow());
				for(Field field : fields) {
					if(form.getFields().stream().noneMatch(field1 -> field1.getName().equals(field.getName()))) {
						form.getFields().add(field);
					}
				}
			} catch (IOException e) {
				logger.error("unable to modif model", e);
			}
		}
	}

	@Transactional
	public void deleteForm(Long formId) {
		Form form = formRepository.findById(formId).get();
		List<UserShare> userShares = userShareService.getUserSharesByForm(form);
		for(UserShare userShare : userShares) {
			userShareService.delete(userShare);
		}
		dataService.nullifyForm(form);
		List<Long> fieldToDelete = form.getFields().stream().map(Field::getId).collect(Collectors.toList());
		form.getFields().clear();
		for(Long fieldId : fieldToDelete) {
			List<FieldPropertie> fieldProperties = fieldPropertieService.getFieldPropertie(fieldId);
			for(FieldPropertie fieldPropertie : fieldProperties) {
				fieldPropertieService.delete(fieldPropertie.getId());
			}
			fieldService.deleteField(fieldId);
		}
		formRepository.delete(form);
	}

	private Map<COSDictionary, Integer> getPageNumberByAnnotDict(PDDocumentCatalog docCatalog) throws IOException {
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
	public Form createForm(Document document, String name, String title, Workflow workflow, String prefillType, List<String> roleNames, Boolean publicUsage, String... fieldNames) throws IOException, EsupSignatureException {
		List<Form> testForms = formRepository.findFormByNameAndActiveVersion(name, true);
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
				form.setFields(getFields(document, workflow));
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

	private List<Field> getFields(Document document, Workflow workflow) throws IOException, EsupSignatureException {
		List<Field> fields = new ArrayList<>();
		List<Field> fieldsOrdered = new LinkedList<>();
		PDDocument pdDocument = PDDocument.load(document.getInputStream());
		PDFieldTree pdFields = pdfService.getFields(pdDocument);
		if(pdFields != null) {
			PDDocumentCatalog pdDocumentDocumentCatalog = pdDocument.getDocumentCatalog();
			Map<COSDictionary, Integer> pageNrByAnnotDict = getPageNumberByAnnotDict(pdDocumentDocumentCatalog);
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
		return formRepository.findFormByNameAndActiveVersion(name, activeVersion);
	}

	public List<Form> getFormByName(String name) {
		return formRepository.findFormByName(name);
	}


	public List<Form> getFormByManagersContains(String eppn) {
		User user = userService.getUserByEppn(eppn);
		return formRepository.findFormByManagersContains(user.getEmail());
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
