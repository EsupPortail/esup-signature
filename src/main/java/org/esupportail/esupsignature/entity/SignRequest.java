package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
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

    @OneToOne
    @LazyCollection(LazyCollectionOption.FALSE)
    private User createBy;

    private String exportedDocumentURI;

    @JsonIgnore
    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderColumn
    private List<Document> originalDocuments = new ArrayList<>();

    @JsonIgnore
    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderColumn
    private List<Document> signedDocuments = new ArrayList<>();

    @JsonIgnore
    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderColumn
    private List<Document> attachments = new ArrayList<>();

    @JsonIgnore
    @ElementCollection
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<String> links = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private SignRequestStatus status;

    @ManyToOne
    @NotNull
    private SignBook parentSignBook;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @LazyCollection(LazyCollectionOption.FALSE)
    @OrderColumn
    private List<SignRequestParams> signRequestParams = new ArrayList<>();

    private Date endDate;

    @JsonIgnore
    @Transient
    transient String viewTitle;

    @JsonIgnore
    @Transient
    transient String comment;

    @JsonIgnore
    @Transient
    transient Boolean signable = false;

    @JsonIgnore
    @Transient
    transient Data data;
    
    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @LazyCollection(LazyCollectionOption.FALSE)
    private Map<Recipient, Action> recipientHasSigned = new HashMap<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public User getCreateBy() {
        return createBy;
    }

    public void setCreateBy(User createBy) {
        this.createBy = createBy;
    }

    public String getExportedDocumentURI() {
        return exportedDocumentURI;
    }

    public void setExportedDocumentURI(String exportedDocumentURI) {
        this.exportedDocumentURI = exportedDocumentURI;
    }

    public List<Document> getOriginalDocuments() {
        return originalDocuments;
    }

    public void setOriginalDocuments(List<Document> originalDocuments) {
        this.originalDocuments = originalDocuments;
    }

    public List<Document> getSignedDocuments() {
        return signedDocuments;
    }

    public void setSignedDocuments(List<Document> signedDocuments) {
        this.signedDocuments = signedDocuments;
    }

    public List<Document> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Document> attachments) {
        this.attachments = attachments;
    }

    public List<String> getLinks() {
        return links;
    }

    public void setLinks(List<String> links) {
        this.links = links;
    }

    public SignRequestStatus getStatus() {
        return status;
    }

    public void setStatus(SignRequestStatus status) {
        this.status = status;
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

    public String getViewTitle() {
        return viewTitle;
    }

    public void setViewTitle(String viewTitle) {
        this.viewTitle = viewTitle;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Boolean getSignable() {
        return signable;
    }

    public void setSignable(Boolean signable) {
        this.signable = signable;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Map<Recipient, Action> getRecipientHasSigned() {
        return recipientHasSigned;
    }

    public void setRecipientHasSigned(Map<Recipient, Action> recipientHasSigned) {
        this.recipientHasSigned = recipientHasSigned;
    }

    public void setCurrentSignRequestParams(SignRequestParams signRequestParam) {
        if(this.signRequestParams.size() >= parentSignBook.getLiveWorkflow().getCurrentStepNumber() && parentSignBook.getLiveWorkflow().getCurrentStepNumber() > -1) {
            this.signRequestParams.set(parentSignBook.getLiveWorkflow().getCurrentStepNumber() - 1, signRequestParam);
        }
    }

    public SignRequestParams getCurrentSignRequestParams() {
        if(signRequestParams.size() >= parentSignBook.getLiveWorkflow().getCurrentStepNumber() && parentSignBook.getLiveWorkflow().getCurrentStepNumber() > -1) {
            return signRequestParams.get(parentSignBook.getLiveWorkflow().getCurrentStepNumber() - 1);
        } else {
            return getEmptySignRequestParams();
        }
    }

    public static SignRequestParams getEmptySignRequestParams() {
        SignRequestParams signRequestParams = new SignRequestParams();
        signRequestParams.setSignImageNumber(0);
        signRequestParams.setSignPageNumber(1);
        signRequestParams.setxPos(0);
        signRequestParams.setyPos(0);
        return signRequestParams;
    }

    public List<Document> getLiteOriginalDocuments() {
        List<Document> liteDocuments = new ArrayList<>();
        for (Document document : this.originalDocuments) {
            document.setBigFile(null);
            liteDocuments.add(document);
        }
        return liteDocuments;
    }

    public List<Document> getLiteSignedDocuments() {
        List<Document> liteDocuments = new ArrayList<>();
        for (Document document : this.signedDocuments) {
            document.setBigFile(null);
            liteDocuments.add(document);
        }
        return liteDocuments;
    }

    public Document getLastSignedDocument() {
        if(this.getSignedDocuments().size() > 0) {
            return this.getSignedDocuments().get(this.getSignedDocuments().size() - 1);
        } else {
            return getLastOriginalDocument();
        }
    }

    public Document getLastOriginalDocument() {
        List<Document> documents = this.getOriginalDocuments();
        if (documents.size() != 1) {
            return null;
        } else {
            return documents.get(0);
        }
    }

    public SignType getCurrentSignType() {
        if(this.getParentSignBook().getLiveWorkflow().getWorkflowSteps() != null && this.getSignable()) {
            return this.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType();
        } else {
            return null;
        }
    }

}
