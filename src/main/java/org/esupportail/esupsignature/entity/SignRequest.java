package org.esupportail.esupsignature.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Configurable
public class SignRequest {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

	@Version
    @Column(name = "version")
    private Integer version;
	
	@Column(unique=true)
	private String name;
	
	private String title;
	
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date createDate;

    private String createBy;
    
    @Transient
    private String comment;
    
    @OneToMany
    private List<Document> originalDocuments = new ArrayList<Document>();
    
    @OneToMany
    private List<Document> signedDocuments = new ArrayList<Document>();
    
    private boolean overloadSignBookParams = false;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private SignRequestParams signRequestParams = new SignRequestParams();
    
    @Enumerated(EnumType.STRING)
    private SignRequestStatus status;	
    
    @ElementCollection(fetch = FetchType.EAGER)
    private Map<Long, Boolean> signBooks = new HashMap<Long, Boolean>();
    
    private Integer signBooksWorkflowStep = 1;
    
    private Integer nbSign = 0;
    
    private boolean allSignToComplete = false;
    
    public enum SignRequestStatus {
		draft, pending, canceled, checked, signed, refused, deleted, exported, completed;
	}
/*
    public List<SignBook> getOriginalSignBooks() {
    	return SignBook.findSignBooksBySignRequestsEquals(Arrays.asList(this)).getResultList();
    }
    
    public Map<String, Boolean> getSignBooksLabels() {
    	Map<String, Boolean> signBookNames = new HashMap<>();
		for(Map.Entry<Long, Boolean> signBookId : signBooks.entrySet()) {
			signBookNames.put(SignBook.findSignBook(signBookId.getKey()).getName(), signBookId.getValue());
		}
		return signBookNames;
		
    }
    */
    public void setStatus(SignRequestStatus status) {
        this.status = status;
    }

    
    
    public int countSignOk() {
    	int nbSign = 0;
		for(Map.Entry<Long, Boolean> signBookId : signBooks.entrySet()) {
			if(signBookId.getValue()) {
				nbSign++;
			}
		}
		nbSign += signedDocuments.size();
		return nbSign;
    }
    
    public List<String> getSignBooksJson() {
    	List<String> result = new ArrayList<>();
    	for(Map.Entry<Long, Boolean> signBookId : signBooks.entrySet()) {
			result.add(signBookId.getKey().toString());
		}
    	return result;
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

	public String getName() {
        return this.name;
    }

	public void setName(String name) {
        this.name = name;
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

	public boolean isOverloadSignBookParams() {
        return this.overloadSignBookParams;
    }

	public void setOverloadSignBookParams(boolean overloadSignBookParams) {
        this.overloadSignBookParams = overloadSignBookParams;
    }

	public SignRequestParams getSignRequestParams() {
        return this.signRequestParams;
    }

	public void setSignRequestParams(SignRequestParams signRequestParams) {
        this.signRequestParams = signRequestParams;
    }

	public SignRequestStatus getStatus() {
        return this.status;
    }

	public Map<Long, Boolean> getSignBooks() {
        return this.signBooks;
    }

	public void setSignBooks(Map<Long, Boolean> signBooks) {
        this.signBooks = signBooks;
    }

	public Integer getSignBooksWorkflowStep() {
        return this.signBooksWorkflowStep;
    }

	public void setSignBooksWorkflowStep(Integer signBooksWorkflowStep) {
        this.signBooksWorkflowStep = signBooksWorkflowStep;
    }

	public Integer getNbSign() {
        return this.nbSign;
    }

	public void setNbSign(Integer nbSign) {
        this.nbSign = nbSign;
    }

	public boolean isAllSignToComplete() {
        return this.allSignToComplete;
    }

	public void setAllSignToComplete(boolean allSignToComplete) {
        this.allSignToComplete = allSignToComplete;
    }

}
