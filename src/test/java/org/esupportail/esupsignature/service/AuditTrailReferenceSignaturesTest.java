package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.dto.audit.ReferenceSignatureDto;
import org.esupportail.esupsignature.entity.AuditStep;
import org.esupportail.esupsignature.entity.AuditTrail;
import org.esupportail.esupsignature.repository.AuditTrailRepository;
import org.junit.jupiter.api.Test;
import java.util.Date;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuditTrailReferenceSignaturesTest {
    @Test
    void exposesRecordedIdentityAndDateWithoutTechnicalEvidence() {
        AuditTrailRepository repository = mock(AuditTrailRepository.class);
        AuditTrailService service = new AuditTrailService(repository, null, null, null, null, null, null, null);
        AuditStep step = new AuditStep();
        step.setName("Martin");
        step.setFirstname("Alice");
        step.setEmail("alice@example.org");
        step.setTimeStampDate(new Date(1234));
        step.setSignCertificat("private-evidence");
        AuditTrail audit = new AuditTrail();
        audit.setAuditSteps(List.of(step));
        when(repository.findByToken("known")).thenReturn(audit);
        List<ReferenceSignatureDto> signatures = service.getReferenceSignatures("known");
        assertThat(signatures).singleElement().satisfies(signature -> {
            assertThat(signature.getName()).isEqualTo("Martin");
            assertThat(signature.getFirstname()).isEqualTo("Alice");
            assertThat(signature.getEmail()).isEqualTo("alice@example.org");
            assertThat(signature.getDate()).isEqualTo(new Date(1234));
        });
        assertThat(ReferenceSignatureDto.class.getDeclaredFields()).extracting(java.lang.reflect.Field::getName)
                .containsExactlyInAnyOrder("name", "firstname", "email", "date");
        assertThat(service.getReferenceSignatures("unknown")).isEmpty();
    }
}
