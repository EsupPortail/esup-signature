package org.esupportail.esupsignature;

import org.esupportail.esupsignature.config.ldap.LdapProperties;
import org.esupportail.esupsignature.service.ldap.LdapPersonLightService;
import org.esupportail.esupsignature.service.ldap.entry.PersonLightLdap;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LdapPersonLightServiceTest {

    @Test
    void exactUserLookupKeepsEntryWithoutMailSoCallerCanReportItClearly() {
        LdapTemplate ldapTemplate = mock(LdapTemplate.class);
        PersonLightLdap person = new PersonLightLdap();
        person.setUid("missingmail");
        person.setMail("");
        when(ldapTemplate.search(any(LdapQuery.class), any(AttributesMapper.class))).thenReturn(List.of(person));

        LdapPersonLightService service = new LdapPersonLightService(new LdapProperties(), ldapTemplate);

        assertThat(service.getPersonLdapLight("missingmail")).containsExactly(person);
        assertThat(service.searchLight("missingmail")).isEmpty();
    }

    @Test
    void searchLightEscapesUserInputBeforeBuildingLdapFilter() {
        LdapTemplate ldapTemplate = mock(LdapTemplate.class);
        when(ldapTemplate.search(any(LdapQuery.class), any(AttributesMapper.class))).thenReturn(List.of());

        LdapPersonLightService service = new LdapPersonLightService(new LdapProperties(), ldapTemplate);
        service.searchLight("*)(uid=*))(|(uid=*");

        ArgumentCaptor<LdapQuery> queryCaptor = ArgumentCaptor.forClass(LdapQuery.class);
        verify(ldapTemplate).search(queryCaptor.capture(), any(AttributesMapper.class));

        String encodedFilter = queryCaptor.getValue().filter().encode();
        assertThat(encodedFilter).contains("\\2a\\29\\28uid=\\2a\\29\\29\\28|\\28uid=\\2a");
        assertThat(encodedFilter).doesNotContain("(|(uid=*)");
    }
}
