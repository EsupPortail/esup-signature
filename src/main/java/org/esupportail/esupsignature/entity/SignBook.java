package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
@Table(indexes =  {
        @Index(name = "sign_book_create_by", columnList = "create_by_id"),
        @Index(name = "sign_book_status", columnList = "status"),
        @Index(name = "sign_book_live_workflow", columnList = "live_workflow_id")
})
public class SignBook {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

	@Version
    private Integer version;

	private String name;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date createDate;

    @ManyToOne
    private User createBy;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date updateDate;

    private String updateBy;
    
    private Boolean external = false;

    @Enumerated(EnumType.STRING)
    private SignRequestStatus status;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private LiveWorkflow liveWorkflow;

    @JsonIgnore
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @OrderColumn
    private List<SignRequest> signRequests = new ArrayList<>();

    @JsonIgnore
    @Transient
    transient List<Log> logs;

    @JsonIgnore
    @Transient
    transient String comment;

    @ManyToMany
    private List<User> viewers = new ArrayList<>();

    private Boolean forceAllDocsSign = false;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Boolean getExternal() {
        return external;
    }

    public void setExternal(Boolean external) {
        this.external = external;
    }

    public SignRequestStatus getStatus() {
        return status;
    }

    public void setStatus(SignRequestStatus status) {
        this.status = status;
    }

    public LiveWorkflow getLiveWorkflow() {
        return liveWorkflow;
    }

    public void setLiveWorkflow(LiveWorkflow liveWorkflow) {
        this.liveWorkflow = liveWorkflow;
    }

    public List<SignRequest> getSignRequests() {
        return signRequests;
    }

    public void setSignRequests(List<SignRequest> signRequests) {
        this.signRequests = signRequests;
    }

    public List<Log> getLogs() {
        return logs;
    }

    public void setLogs(List<Log> logs) {
        this.logs = logs;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<User> getViewers() {
        return viewers;
    }

    public void setViewers(List<User> viewers) {
        this.viewers = viewers;
    }

    public Boolean getForceAllDocsSign() {
        return forceAllDocsSign;
    }

    public void setForceAllDocsSign(Boolean forceAllDocsSign) {
        this.forceAllDocsSign = forceAllDocsSign;
    }
}
