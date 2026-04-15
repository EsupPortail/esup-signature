package org.esupportail.esupsignature.dto.page.user.signbook;

import org.esupportail.esupsignature.dto.page.user.signrequest.ShowSignRequestDto;
import org.esupportail.esupsignature.entity.enums.ArchiveStatus;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;

import java.util.Date;
import java.util.List;

public record SignBookLightDto(
        Long id,
        String workflowName,
        String description,
        SignRequestStatus status,
        Boolean deleted,
        Boolean editable,
        ArchiveStatus archiveStatus,
        Date createDate,
        List<ShowSignRequestDto.SignBookViewerDto> viewers
) {}