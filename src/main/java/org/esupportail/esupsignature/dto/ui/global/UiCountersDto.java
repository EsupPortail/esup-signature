package org.esupportail.esupsignature.dto.ui.global;

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


