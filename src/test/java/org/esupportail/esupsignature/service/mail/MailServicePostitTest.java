package org.esupportail.esupsignature.service.mail;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MailServicePostitTest {

    @Test
    void excludesExternalUsersFromPostitRecipients() {
        User creator = user("creator@example.org", UserType.ldap);
        User sender = user("sender@example.org", UserType.ldap);
        User internalRecipient = user("internal@example.org", UserType.ldap);
        User externalRecipient = user("external@example.org", UserType.external);
        SignBook signBook = new SignBook();
        signBook.setCreateBy(creator);
        signBook.getTeam().add(sender);
        signBook.getTeam().add(internalRecipient);
        signBook.getTeam().add(externalRecipient);

        GlobalProperties globalProperties = mock(GlobalProperties.class);
        when(globalProperties.getDomain()).thenReturn("example.org");
        MailService mailService = new MailService(globalProperties, null, null, null, mock(UserService.class), null, null, null);

        Set<String> recipients = ReflectionTestUtils.invokeMethod(mailService, "getPostitRecipientEmails", signBook, sender, true);

        assertThat(recipients).containsExactlyInAnyOrder("creator@example.org", "internal@example.org");
    }

    @Test
    void excludesAnExternalCreatorFromPostitRecipients() {
        User externalCreator = user("external@example.org", UserType.external);
        User sender = user("sender@example.org", UserType.ldap);
        SignBook signBook = new SignBook();
        signBook.setCreateBy(externalCreator);

        GlobalProperties globalProperties = mock(GlobalProperties.class);
        MailService mailService = new MailService(globalProperties, null, null, null, mock(UserService.class), null, null, null);

        Set<String> recipients = ReflectionTestUtils.invokeMethod(mailService, "getPostitRecipientEmails", signBook, sender, false);

        assertThat(recipients).isEmpty();
    }

    private User user(String email, UserType userType) {
        User user = new User();
        user.setEppn(email);
        user.setEmail(email);
        user.setUserType(userType);
        return user;
    }
}
