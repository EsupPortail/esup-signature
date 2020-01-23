package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.util.*;

@Entity
public class SignRequest {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

	@Version
    private Integer version;
	
	@Column(unique=true)
	private String token;
	
	private String title;
	
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date createDate;

    private String createBy;

    @JsonIgnore
    @Transient
    transient String viewTitle;

    @JsonIgnore
    @Transient
    transient String comment;

    @JsonIgnore
    @Transient
    transient User creator;

    private String exportedDocumentURI;

    @JsonIgnore
    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Document> originalDocuments = new ArrayList<>();

    @JsonIgnore
    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Document> signedDocuments = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private SignRequestStatus status;

    @ManyToOne
    private SignBook parentSignBook;

    private Integer currentStepNumber = 0;

    @Enumerated(EnumType.STRING)
    private SignType signType;

    private Boolean allSignToComplete = false;

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<SignRequestParams> signRequestParams = new ArrayList<>();

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Recipient> recipients = new ArrayList<>();

   	public void setStatus(SignRequestStatus status) {
        this.status = status;
    }

    public Long getId() {
        return this.id;
    }

	public void setId(Long id) {
        this.id = id;
    }

	public Integer getVersion() {
        return this.version;
    }

	public void setVersion(Integer version) {
        this.version = version;
    }

	public String getToken() {
        return this.token;
    }

	public void setToken(String token) {
        this.token = token;
    }

	public String getTitle() {
        return this.title;
    }

	public void setTitle(String title) {
        this.title = title;
    }

	public Date getCreateDate() {
        return this.createDate;
    }

	public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

	public String getCreateBy() {
        return this.createBy;
    }

	public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

	public String getComment() {
        return this.comment;
    }

	public void setComment(String comment) {
        this.comment = comment;
    }

    public User getCreator() {
        return creator;
    }

    public String getViewTitle() {
        return viewTitle;
    }

    public void setViewTitle(String viewTitle) {
        this.viewTitle = viewTitle;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public SignRequestStatus getStatus() {
        return status;
    }

    public List<Document> getOriginalDocuments() {
        return this.originalDocuments;
    }

	public void setOriginalDocuments(List<Document> originalDocuments) {
        this.originalDocuments = originalDocuments;
    }

	public List<Document> getSignedDocuments() {
        return this.signedDocuments;
    }

	public void setSignedDocuments(List<Document> signedDocuments) {
        this.signedDocuments = signedDocuments;
    }

	public String getExportedDocumentURI() {
		return exportedDocumentURI;
	}

	public void setExportedDocumentURI(String exportedDocumentURI) {
		this.exportedDocumentURI = exportedDocumentURI;
	}

    public SignBook getParentSignBook() {
        return parentSignBook;
    }

    public void setParentSignBook(SignBook parentSignBook) {
        this.parentSignBook = parentSignBook;
    }

    public List<SignRequestParams> getSignRequestParams() {
        return signRequestParams;
    }

    public void setSignRequestParams(List<SignRequestParams> signRequestParams) {
        this.signRequestParams = signRequestParams;
    }

    public Integer getCurrentStepNumber() {
        return currentStepNumber;
    }

    public void setCurrentStepNumber(Integer currentStepNumber) {
        this.currentStepNumber = currentStepNumber;
    }

    public List<Recipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<Recipient> recipients) {
        this.recipients = recipients;
    }

    public SignType getSignType() {
        return signType;
    }

    public void setSignType(SignType signType) {
        this.signType = signType;
    }

    public Boolean getAllSignToComplete() {
        return allSignToComplete;
    }

    public void setAllSignToComplete(Boolean allSignToComplete) {
        this.allSignToComplete = allSignToComplete;
    }
}
