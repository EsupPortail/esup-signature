package org.esupportail.esupsignature.dto.page.admin;

import org.esupportail.esupsignature.dto.page.user.signbook.SignBookLightDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.ShowSignRequestDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.SignRequestFullDto;

import java.util.Date;
import java.util.List;

public record AdminSignRequestShowViewDto(
        SignBookLightDto signBookLight,
        ShowSignRequestDto.SignRequestLightDto signRequestLight,
        SignRequestFullDto signRequestFull,
        ShowSignRequestDto.WorkflowMetaDto workflow,
        List<ShowSignRequestDto.DocumentDto> originalDocuments,
        List<ShowSignRequestDto.DocumentDto> signedDocuments,
        List<ShowSignRequestDto.DocumentDto> documentsHistory,
        List<ShowSignRequestDto.StepDto> steps,
        List<ShowSignRequestDto.TargetDto> targets,
        List<CommentDto> comments,
        List<LogDto> logs,
        boolean manager
) {

    public record CommentDto(
            Long id,
            UserDto createBy,
            Date createDate,
            String text
    ) {
    }

    public record LogDto(
            Date logDate,
            String eppn,
            String action,
            String initialStatus,
            String finalStatus,
            String comment
    ) {
    }

    public record UserDto(
            Long id,
            String eppn,
            String firstname,
            String name,
            String email,
            String phone,
            String userType
    ) {
    }
}

