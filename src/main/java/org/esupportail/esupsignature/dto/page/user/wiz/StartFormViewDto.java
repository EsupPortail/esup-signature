package org.esupportail.esupsignature.dto.page.user.wiz;

public class StartFormViewDto {

    private Long id;
    private String title;
    private String messageToDisplay;
    private WorkflowViewDto workflow;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessageToDisplay() {
        return messageToDisplay;
    }

    public void setMessageToDisplay(String messageToDisplay) {
        this.messageToDisplay = messageToDisplay;
    }

    public WorkflowViewDto getWorkflow() {
        return workflow;
    }

    public void setWorkflow(WorkflowViewDto workflow) {
        this.workflow = workflow;
    }
}
