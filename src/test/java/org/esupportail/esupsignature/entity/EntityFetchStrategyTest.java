package org.esupportail.esupsignature.entity;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityFetchStrategyTest {

    @Test
    void largeElementCollectionsAreLazy() throws NoSuchFieldException {
        ElementCollection transmittedIds = User.class.getDeclaredField("transmittedSignRequestIds")
                .getAnnotation(ElementCollection.class);
        ElementCollection links = SignRequest.class.getDeclaredField("links")
                .getAnnotation(ElementCollection.class);

        assertThat(transmittedIds.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(links.fetch()).isEqualTo(FetchType.LAZY);
    }
}
