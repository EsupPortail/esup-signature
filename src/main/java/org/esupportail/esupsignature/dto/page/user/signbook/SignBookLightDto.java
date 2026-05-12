package org.esupportail.esupsignature.dto.page.user.signbook;

import org.esupportail.esupsignature.dto.page.user.signrequest.ShowSignRequestDto;
import org.esupportail.esupsignature.entity.enums.ArchiveStatus;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;

import java.util.Date;
import java.util.List;

public class SignBookLightDto {

    private Long id;
    private String workflowName;
    private String subject;
    private String description;
    private SignRequestStatus status;
    private Boolean deleted;
    private Boolean editable;
    private ArchiveStatus archiveStatus;
    private Date createDate;
    private List<ShowSignRequestDto.SignBookViewerDto> viewers;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWorkflowName() { return workflowName; }
    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public SignRequestStatus getStatus() { return status; }
    public void setStatus(SignRequestStatus status) { this.status = status; }
    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
    public Boolean getEditable() { return editable; }
    public void setEditable(Boolean editable) { this.editable = editable; }
    public ArchiveStatus getArchiveStatus() { return archiveStatus; }
    public void setArchiveStatus(ArchiveStatus archiveStatus) { this.archiveStatus = archiveStatus; }
    public Date getCreateDate() { return createDate; }
    public void setCreateDate(Date createDate) { this.createDate = createDate; }
    public List<ShowSignRequestDto.SignBookViewerDto> getViewers() { return viewers; }
    public void setViewers(List<ShowSignRequestDto.SignBookViewerDto> viewers) { this.viewers = viewers; }

    public Long id() { return id; }
    public String workflowName() { return workflowName; }
    public String subject() { return subject; }
    public String description() { return description; }
    public SignRequestStatus status() { return status; }
    public Boolean deleted() { return deleted; }
    public Boolean editable() { return editable; }
    public ArchiveStatus archiveStatus() { return archiveStatus; }
    public Date createDate() { return createDate; }
    public List<ShowSignRequestDto.SignBookViewerDto> viewers() { return viewers; }
}
