package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class SignBook {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

	@Version
    private Integer version;
    
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
	
	@Column(unique=true)
	private String name;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date createDate;

    private String createBy;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date updateDate;

    private String updateBy;
    
    private Boolean external = false;
    
    @Size(max = 500)
    private String description;

    @ElementCollection(targetClass=String.class)
    private List<String> recipientEmails = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	private SignBookType signBookType;
	
	public enum SignBookType {
		system, group, workflow;
	}

	@OneToMany
    @OrderColumn
    private List<SignRequest> signRequests = new ArrayList<>();

    @ManyToOne
    private Workflow workflow;

    public void setSignBookType(SignBookType signBookType) {
        this.signBookType = signBookType;
    }
        
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
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
    
    public Date getUpdateDate() {
        return this.updateDate;
    }
    
    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }
    
    public String getUpdateBy() {
        return this.updateBy;
    }
    
    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }
    
    public String getDescription() {
        return this.description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getRecipientEmails() {
        return this.recipientEmails;
    }
    
    public SignBookType getSignBookType() {
        return this.signBookType;
    }

	public Boolean isExternal() {
		return external;
	}

	public void setExternal(Boolean external) {
		this.external = external;
	}

    public List<SignRequest> getSignRequests() {
        return signRequests;
    }

    public void setSignRequests(List<SignRequest> signRequests) {
        this.signRequests = signRequests;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }
}
