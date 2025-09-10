package org.esupportail.esupsignature.dto.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.SignType;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO représentant une étape d’un workflow avec ses paramètres.
 */
@JsonIgnoreProperties
public class WorkflowStepDto {


    /**
     * Titre de l’étape.
     */
    private String title;

    /**
     * Identifiant du workflow auquel appartient cette étape.
     */
    private Long workflowId;

    /**
     * Numéro de l’étape dans le workflow (valeur par défaut : 1).
     */
    private Integer stepNumber = 1;

    /**
     * Description de l’étape.
     */
    private String description;

    /**
     * Liste des adresses e-mail des destinataires en copie carbone (CC).
     */
    private List<String> recipientsCCEmails = new ArrayList<>();

    /**
     * Liste des destinataires assignés à cette étape.
     */
    private List<RecipientWsDto> recipients = new ArrayList<>();

    /**
     * Liste des paramètres des signatures visuelle pour cette étape.
     */
    private List<SignRequestParamsWsDto> signRequestParams = new ArrayList<>();

    /**
     * Indique si l’étape peut être modifiée.
     */
    private Boolean changeable = null;

    /**
     * Niveau de signature requis pour cette étape (valeur par défaut : simple).
     */
    private SignLevel signLevel = SignLevel.simple;

    /**
     * Type de signature utilisé pour cette étape.
     */
    private SignType signType;

    /**
     * Indique si cette étape peut être modifiée par l’utilisateur.
     */
    private Boolean repeatable = null;

    /**
     * Type de signature requis pour les répétitions de l’étape (valeur par défaut : visa).
     */
    private SignType repeatableSignType = SignType.visa;

    /**
     * Indique si toutes les signatures doivent être complétées pour valider cette étape.
     */
    private Boolean allSignToComplete;

    /**
     * Indique si le créateur doit signer en premier.
     */
    private Boolean userSignFirst = false;

    /**
     * Indique si plusieurs signatures sont autorisées pour cette étape.
     */
    private Boolean multiSign;

    /**
     * Indique si une seule signature avec des annotations est autorisée.
     */
    private Boolean singleSignWithAnnotation;

    /**
     * Indique si cette étape sera automatiquement signée.
     */
    private Boolean autoSign = false;

    /**
     * Indique si cette étape sera terminée par l’apposition du cachet de l’établissement.
     */
    private Boolean sealVisa = false;

    /**
     * Force l’exigence de toutes les signatures pour finaliser cette étape.
     */
    private Boolean forceAllSign = false;

    /**
     * Commentaire associé à cette étape.
     */
    private String comment;

    /**
     * Indique si une pièce jointe est requise pour cette étape.
     */
    private Boolean attachmentRequire;

    /**
     * Indique si une alerte doit être déclenchée lorsqu’une pièce jointe est demandée.
     */
    private Boolean attachmentAlert;

    /**
     * Nombre maximal de destinataires autorisés pour cette étape (valeur par défaut : 99).
     */
    private Integer maxRecipients = 99;

    /**
     * Liste des e-mails des destinataires finaux.
     */
    private List<String> targetEmails = new ArrayList<>();

    /**
     * Indique si le document doit être converti en format PDF/A.
     */
    private Boolean convertToPDFA = true;

	public WorkflowStepDto() {
	}

	public WorkflowStepDto(RecipientWsDto recipientWsDto) {
		this.recipients.add(recipientWsDto);
	}

	public WorkflowStepDto(SignType signType, String description, List<RecipientWsDto> recipients, Boolean changeable, Integer maxRecipients, Boolean allSignToComplete, Boolean attachmentRequire) {
		this.signType = signType;
		this.description = description;
		this.changeable = changeable;
		this.maxRecipients = maxRecipients;
		this.allSignToComplete = allSignToComplete;
		this.attachmentRequire = attachmentRequire;
		if(recipients != null) {
			this.recipients.addAll(recipients);
		}
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Long getWorkflowId() {
		return workflowId;
	}

	public void setWorkflowId(Long workflowId) {
		this.workflowId = workflowId;
	}

	public Integer getStepNumber() {
		return stepNumber;
	}

	public void setStepNumber(Integer stepNumber) {
		this.stepNumber = stepNumber;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<String> getRecipientsCCEmails() {
		return recipientsCCEmails;
	}

	public void setRecipientsCCEmails(List<String> recipientsEmails) {
		this.recipientsCCEmails = recipientsEmails;
	}

	public List<RecipientWsDto> getRecipients() {
		return recipients;
	}

	public void setRecipients(List<RecipientWsDto> recipients) {
		this.recipients = recipients;
	}

	public List<SignRequestParamsWsDto> getSignRequestParams() {
		return signRequestParams;
	}

	public void setSignRequestParams(List<SignRequestParamsWsDto> signRequestParams) {
		this.signRequestParams = signRequestParams;
	}

	public Boolean getChangeable() {
		return changeable;
	}

	public void setChangeable(Boolean changeable) {
		this.changeable = changeable;
	}

	public SignLevel getSignLevel() {
		return signLevel;
	}

	public void setSignLevel(SignLevel signLevel) {
		this.signLevel = signLevel;
	}

	public SignType getSignType() {
		return signType;
	}

	public void setSignType(SignType signType) {
		this.signType = signType;
	}

	public Boolean getRepeatable() {
		return repeatable;
	}

	public void setRepeatable(Boolean repeatable) {
		this.repeatable = repeatable;
	}

	public SignType getRepeatableSignType() {
		return repeatableSignType;
	}

	public void setRepeatableSignType(SignType repeatableSignType) {
		this.repeatableSignType = repeatableSignType;
	}

	public Boolean getAllSignToComplete() {
		return allSignToComplete;
	}

	public void setAllSignToComplete(Boolean allSignToComplete) {
		this.allSignToComplete = allSignToComplete;
	}

	public Boolean getUserSignFirst() {
		return userSignFirst;
	}

	public void setUserSignFirst(Boolean userSignFirst) {
		this.userSignFirst = userSignFirst;
	}

	public Boolean getMultiSign() {
		return multiSign;
	}

	public void setMultiSign(Boolean multiSign) {
		this.multiSign = multiSign;
	}

	public Boolean getSingleSignWithAnnotation() {
		return singleSignWithAnnotation;
	}

	public void setSingleSignWithAnnotation(Boolean singleSignWithAnnotation) {
		this.singleSignWithAnnotation = singleSignWithAnnotation;
	}

	public Boolean getAutoSign() {
		return autoSign;
	}

	public void setAutoSign(Boolean autoSign) {
		this.autoSign = autoSign;
	}

	public Boolean getForceAllSign() {
		return forceAllSign;
	}

	public void setForceAllSign(Boolean forceAllSign) {
		this.forceAllSign = forceAllSign;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Boolean getAttachmentRequire() {
		return attachmentRequire;
	}

	public void setAttachmentRequire(Boolean attachmentRequire) {
		this.attachmentRequire = attachmentRequire;
	}

	public Boolean getAttachmentAlert() {
		return attachmentAlert;
	}

	public void setAttachmentAlert(Boolean attachmentAlert) {
		this.attachmentAlert = attachmentAlert;
	}

	public Integer getMaxRecipients() {
		return maxRecipients;
	}

	public void setMaxRecipients(Integer maxRecipients) {
		this.maxRecipients = maxRecipients;
	}

	public List<String> getTargetEmails() {
		return targetEmails;
	}

	public void setTargetEmails(List<String> targetEmails) {
		this.targetEmails = targetEmails;
	}

	public Boolean getSealVisa() {
		return sealVisa;
	}

	public void setSealVisa(Boolean sealVisa) {
		this.sealVisa = sealVisa;
	}

	public Boolean getConvertToPDFA() {
		return convertToPDFA;
	}

	public void setConvertToPDFA(Boolean convertToPDFA) {
		this.convertToPDFA = convertToPDFA;
	}
}
