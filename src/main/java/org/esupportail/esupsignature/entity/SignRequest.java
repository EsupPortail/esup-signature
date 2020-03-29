package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    @JsonIgnore
    @Transient
    transient String viewTitle;

    @JsonIgnore
    @Transient
    transient String comment;

    @JsonIgnore
    @Transient
    transient Boolean signable = false;

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

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
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

    public Integer getCurrentStepNumber() {
        return currentStepNumber;
    }

    public void setCurrentStepNumber(Integer currentStepNumber) {
        this.currentStepNumber = currentStepNumber;
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

    public List<SignRequestParams> getSignRequestParams() {
        return signRequestParams;
    }

    public void setSignRequestParams(List<SignRequestParams> signRequestParams) {
        this.signRequestParams = signRequestParams;
    }

    public List<Recipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<Recipient> recipients) {
        this.recipients = recipients;
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

    public SignRequestParams getCurrentSignRequestParams() {
        if(getSignRequestParams().size() > currentStepNumber - 1) {
            return getSignRequestParams().get(currentStepNumber - 1);
        } else {
            SignRequestParams signRequestParams = new SignRequestParams();
            signRequestParams.setSignPageNumber(1);
            signRequestParams.setxPos(0);
            signRequestParams.setyPos(0);
            return signRequestParams;
        }
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
}
