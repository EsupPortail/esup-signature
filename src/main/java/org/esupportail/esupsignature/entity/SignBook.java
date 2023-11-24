package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
@Table(indexes =  {
        @Index(name = "sign_book_create_by", columnList = "create_by_id"),
        @Index(name = "sign_book_status", columnList = "status"),
        @Index(name = "sign_book_live_workflow", columnList = "live_workflow_id")
})
public class SignBook {

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence", allocationSize = 1)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String subject;

    private String workflowName;

    @OneToMany(mappedBy = "signBook", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<Otp> otps = new HashSet<>();

    @Deprecated
	private String name;

    @Deprecated
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
    transient String comment;

    @ManyToMany
    private Set<User> viewers = new HashSet<>();

    @ManyToMany
    private Set<User> team = new HashSet<>();

    @ManyToMany
    private Set<User> hidedBy = new HashSet<>();

    private Boolean forceAllDocsSign = false;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date endDate;

    private String lastOtp;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    @Deprecated
    public String getName() {
        return name;
    }

    @Deprecated
    public String getTitle() {
        return title;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Set<User> getViewers() {
        return viewers;
    }

    public void setViewers(Set<User> viewers) {
        this.viewers = viewers;
    }

    public Set<User> getTeam() {
        return team;
    }

    public void setTeam(Set<User> team) {
        this.team = team;
    }

    public Boolean getForceAllDocsSign() {
        return forceAllDocsSign;
    }

    public void setForceAllDocsSign(Boolean forceAllDocsSign) {
        this.forceAllDocsSign = forceAllDocsSign;
    }

    public Set<User> getHidedBy() {
        return hidedBy;
    }

    public void setHidedBy(Set<User> hidedBy) {
        this.hidedBy = hidedBy;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getLastOtp() {
        return lastOtp;
    }

    public void setLastOtp(String lastOtp) {
        this.lastOtp = lastOtp;
    }

    public List<Comment> getPostits() {
        return signRequests.stream().map(SignRequest::getComments).flatMap(comments -> comments.stream().filter(Comment::getPostit)).collect(Collectors.toList());
    }

}
