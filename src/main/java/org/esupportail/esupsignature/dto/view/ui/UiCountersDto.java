package org.esupportail.esupsignature.dto.view.ui;

public record UiCountersDto(
        Long nbSignRequests,
        Long nbToSign,
        Long nbDeleted,
        Integer reportNumber,
        Integer managedWorkflowsSize,
        Boolean isRoleManager,
        Boolean isOneSignShare,
        Boolean isOneReadShare,
        Boolean certificatProblem
) {
}


