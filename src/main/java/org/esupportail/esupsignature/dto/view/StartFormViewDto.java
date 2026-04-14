package org.esupportail.esupsignature.dto.view;

public class StartFormViewDto {

    private final Long id;
    private final String title;
    private final String messageToDisplay;
    private final WorkflowViewDto workflow;

    public StartFormViewDto(Long id, String title, String messageToDisplay, WorkflowViewDto workflow) {
        this.id = id;
        this.title = title;
        this.messageToDisplay = messageToDisplay;
        this.workflow = workflow;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessageToDisplay() {
        return messageToDisplay;
    }

    public WorkflowViewDto getWorkflow() {
        return workflow;
    }
}
