package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.serializer.RecipientActionMapSerializer;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.*;
import java.util.stream.Collectors;

@Entity
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
@Table(indexes =  {
        @Index(name = "sign_request_create_by_create_date", columnList = "create_by_id, createDate"),
        @Index(name = "sign_request_parent_sign_book", columnList = "parent_sign_book_id"),

})
public class SignRequest {
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence", allocationSize = 1)
    private Long id;

	@Column(unique=true)
	private String token;
	
	private String title;
	
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date createDate;

    @ManyToOne
    private User createBy;

    @Column(columnDefinition = "TEXT")
    private String exportedDocumentURI;

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderColumn
    private List<Document> originalDocuments = new ArrayList<>();

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderColumn
    private List<Document> signedDocuments = new ArrayList<>();

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderColumn
    private List<Document> attachments = new ArrayList<>();

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    private Set<String> links = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private SignRequestStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    private SignBook parentSignBook;

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderColumn
    private List<SignRequestParams> signRequestParams = new LinkedList<>();

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderColumn
    private List<Comment> comments = new ArrayList<>();

    private Boolean warningReaded = false;

    @ManyToMany
    private Set<User> viewedBy = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JsonSerialize(using = RecipientActionMapSerializer.class)
    private Map<Recipient, Action> recipientHasSigned = new HashMap<>();

    @OneToOne(cascade = CascadeType.DETACH)
    private AuditTrail auditTrail;

    @JsonIgnore
    @Transient
    transient Data data;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Set<String> getLinks() {
        return links;
    }

    public void setLinks(Set<String> links) {
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

    public List<Comment> getComments() {
        return comments;
    }

    public Boolean getWarningReaded() {
        return warningReaded;
    }

    public void setWarningReaded(Boolean warningReaded) {
        this.warningReaded = warningReaded;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public Map<Recipient, Action> getRecipientHasSigned() {
        return recipientHasSigned;
    }

    public Set<User> getViewedBy() {
        return viewedBy;
    }

    public void setViewedBy(Set<User> viewedBy) {
        this.viewedBy = viewedBy;
    }

    @JsonIgnore
    public Map<Recipient, Action> getOrderedRecipientHasSigned() {
        if(!recipientHasSigned.isEmpty()) {
            Set<Map.Entry<Recipient, Action>> entries = recipientHasSigned.entrySet().stream().filter(recipientActionEntry -> recipientActionEntry.getValue() != null && recipientActionEntry.getValue().getDate() != null).sorted((o1, o2) -> o2.getValue().getDate().compareTo(o1.getValue().getDate())).collect(Collectors.toCollection(LinkedHashSet::new));
            Map<Recipient, Action> recipientActionMap = new LinkedHashMap<>();
            for (Map.Entry<Recipient, Action> entry : entries) {
                recipientActionMap.put(entry.getKey(), entry.getValue());
            }
            return recipientActionMap;
        } else {
            return recipientHasSigned;
        }
    }

    public void setRecipientHasSigned(Map<Recipient, Action> recipientHasSigned) {
        this.recipientHasSigned = recipientHasSigned;
    }

    public AuditTrail getAuditTrail() {
        return auditTrail;
    }

    public void setAuditTrail(AuditTrail auditTrail) {
        this.auditTrail = auditTrail;
    }

    @JsonIgnore
    public Document getLastSignedDocument() {
        if(!this.getSignedDocuments().isEmpty()) {
            return this.getSignedDocuments().get(this.getSignedDocuments().size() - 1);
        } else {
            return getLastOriginalDocument();
        }
    }

    @JsonIgnore
    public Document getLastOriginalDocument() {
        List<Document> documents = this.getOriginalDocuments();
        if (documents.size() != 1) {
            return null;
        } else {
            return documents.get(0);
        }
    }

    public SignType getCurrentSignType() {
        if(this.getParentSignBook().getLiveWorkflow() != null && this.getParentSignBook().getLiveWorkflow().getCurrentStep() != null) {
            return this.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType();
        } else {
            return null;
        }
    }

}
