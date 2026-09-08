package org.esupportail.esupsignature.dto.mapper;

import org.esupportail.esupsignature.dto.page.user.signbook.SignBookFullDto;
import org.esupportail.esupsignature.dto.projection.jpa.SignBookListMetadataProjection;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UiSignBookMapperTest {

    @Test
    void mapsListPrimarySignRequestDirectlyFromProjection() {
        Date createDate = new Date(1_725_186_600_000L);
        Date lastSignedDocumentDate = new Date(1_725_190_200_000L);
        SignBookListMetadataProjection metadata = mock(SignBookListMetadataProjection.class);
        when(metadata.getPrimarySignRequestId()).thenReturn(42L);
        when(metadata.getSignRequestCount()).thenReturn(2L);
        when(metadata.getPrimarySignRequestTitle()).thenReturn("Document principal");
        when(metadata.getPrimarySignRequestStatus()).thenReturn(SignRequestStatus.completed.name());
        when(metadata.getPrimarySignRequestCreateDate()).thenReturn(createDate);
        when(metadata.getPrimarySignRequestViewedByCurrentUser()).thenReturn(true);
        when(metadata.getPrimarySignRequestHasAttachments()).thenReturn(true);
        when(metadata.getPrimarySignRequestFirstOriginalFileName()).thenReturn("document.pdf");
        when(metadata.getPrimarySignRequestLastSignedDocumentDate()).thenReturn(lastSignedDocumentDate);
        when(metadata.getPrimarySignRequestLastComment()).thenReturn("Dernier commentaire");

        SignBook signBook = new SignBook();
        signBook.setId(7L);
        signBook.setSubject("document.pdf");
        signBook.setStatus(SignRequestStatus.completed);

        UiSignBookMapper mapper = new UiSignBookMapper(mock(UiFetchSignRequestMapper.class));
        SignBookFullDto dto = mapper.toSignBookListItemDto(signBook, "user@example.org", metadata);

        assertThat(dto.getSignRequestCount()).isEqualTo(2);
        assertThat(dto.getRefusedCommentTitle()).isEqualTo("Dernier commentaire");
        assertThat(dto.getLastSignedDocumentDateLabel()).isNotBlank();
        assertThat(dto.getPrimarySignRequest().getId()).isEqualTo(42L);
        assertThat(dto.getPrimarySignRequest().getTitle()).isEqualTo("Document principal");
        assertThat(dto.getPrimarySignRequest().getStatus()).isEqualTo("completed");
        assertThat(dto.getPrimarySignRequest().isViewedByCurrentUser()).isTrue();
        assertThat(dto.getPrimarySignRequest().isHasAttachments()).isTrue();
        assertThat(dto.getPrimarySignRequest().getRowTitle()).isEqualTo("document.pdf, ...");
        assertThat(dto.getPrimarySignRequest().isCanDownload()).isTrue();
        assertThat(dto.getPrimarySignRequest().isCanDownloadAll()).isTrue();
    }
}
