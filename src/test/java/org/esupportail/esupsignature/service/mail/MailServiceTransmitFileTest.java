package org.esupportail.esupsignature.service.mail;

import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.mail.MailConfig;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.interfaces.fs.FsFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MailServiceTransmitFileTest {
    @ParameterizedTest
    @CsvSource({"true,true,true", "true,false,false", "false,true,true", "false,false,false", "true,false,true", "true,true,false"})
    void transmitsOnlyTheSelectedOriginalAndNamesTheActualSender(boolean hasSenderEmail, boolean fromUser, boolean withComment) throws Exception {
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("i18n/messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.setTemplateEngineMessageSource(messages);
        JavaMailSenderImpl transport = mock(JavaMailSenderImpl.class);
        when(transport.createMimeMessage()).thenAnswer(call -> new MimeMessage(Session.getInstance(new Properties())));
        GlobalProperties globals = new GlobalProperties();
        globals.setRootUrl("https://signature.example.org");
        globals.setSendByEmailFromUser(fromUser);
        MailConfig config = mock(MailConfig.class);
        when(config.getMailFrom()).thenReturn("signature@example.org");
        MailService service = new MailService(globals, config, transport, engine, mock(UserService.class), messages, null, null);
        User creator = new User();
        creator.setFirstname("Original");
        creator.setName("Creator");
        User sender = new User();
        sender.setFirstname("Alice");
        String expectedAddress = "signature@example.org";
        if (hasSenderEmail) {
            sender.setEmail("alice@example.org");
            if (fromUser) {
                expectedAddress = "alice@example.org";
            }
        }
        sender.setName("Martin<script>");
        Document original = new Document();
        original.setFileName("original.pdf");
        SignRequest request = new SignRequest();
        request.setId(42L);
        request.setTitle("Selected document");
        request.setToken("selected-token");
        request.setOriginalDocuments(List.of(original));
        SignBook book = new SignBook();
        book.setCreateBy(creator);
        book.setCreateDate(new Date());
        book.setSignRequests(List.of(request, new SignRequest()));
        request.setParentSignBook(book);
        byte[] bytes = "%PDF unchanged signed bytes".getBytes(StandardCharsets.UTF_8);
        FsFile file = new FsFile(new ByteArrayInputStream(bytes), "signed.pdf", "application/pdf");

        String comment = null;
        if (!withComment && hasSenderEmail) {
            comment = "  \n  ";
        }
        if (withComment) {
            comment = "Bonjour <script>alert(1)</script>\nVoici le document signé.";
        }
        assertThat(service.transmitSignedFile(request, file, "recipient@example.org", sender, comment)).isTrue();
        ArgumentCaptor<MimeMessage> captured = ArgumentCaptor.forClass(MimeMessage.class);
        verify(transport).send(captured.capture());
        MimeMessage email = captured.getValue();
        assertThat(email.getAllRecipients()).hasSize(1);
        assertThat(email.getAllRecipients()[0].toString()).isEqualTo("recipient@example.org");
        assertThat(((jakarta.mail.internet.InternetAddress) email.getFrom()[0]).getAddress()).isEqualTo(expectedAddress);
        if (fromUser) {
            assertThat(((jakarta.mail.internet.InternetAddress) email.getFrom()[0]).getPersonal()).contains("Alice");
            assertThat(((jakarta.mail.internet.InternetAddress) email.getSender()).getAddress()).isEqualTo("signature@example.org");
        } else {
            assertThat(((jakarta.mail.internet.InternetAddress) email.getFrom()[0]).getPersonal()).isNull();
            assertThat(email.getSender()).isNull();
        }
        assertThat(((jakarta.mail.internet.InternetAddress) email.getReplyTo()[0]).getAddress()).isEqualTo(expectedAddress);
        assertThat(findHtml(email.getContent())).contains("Transmis par", "Alice", "&lt;script&gt;", "signed.pdf", "/public/control/selected-token")
                .doesNotContain("<script>");
        String html = findHtml(email.getContent());
        if (withComment) {
            assertThat(html).contains("es-email-forwarded-comment", "Bonjour &lt;script&gt;alert(1)&lt;/script&gt;\nVoici le document signé.")
                    .doesNotContain("<script>");
        } else {
            assertThat(html).doesNotContain("es-email-forwarded-comment");
        }
        assertThat(findAttachments(email.getContent(), bytes)).isEqualTo(1);
    }

    @Test
    void rejectsMultipleRecipientsAndDoesNotReportSuccessWithoutTransport() throws Exception {
        MailService service = new MailService(null, null, null, null, null, null, null, null);
        assertThatThrownBy(() -> service.transmitSignedFile(null, null, "a@example.org,b@example.org", null, null))
                .isInstanceOf(jakarta.mail.internet.AddressException.class);
        assertThat(service.transmitSignedFile(null, null, "a@example.org", null, null)).isFalse();
    }

    private String findHtml(Object content) throws Exception {
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof Multipart) {
            Multipart parts = (Multipart) content;
            for (int index = 0; index < parts.getCount(); index++) {
                BodyPart part = parts.getBodyPart(index);
                if (!"attachment".equalsIgnoreCase(part.getDisposition())) {
                    String html = findHtml(part.getContent());
                    if (!html.isEmpty()) {
                        return html;
                    }
                }
            }
        }
        return "";
    }

    private int findAttachments(Object content, byte[] expected) throws Exception {
        int count = 0;
        if (content instanceof Multipart) {
            Multipart parts = (Multipart) content;
            for (int index = 0; index < parts.getCount(); index++) {
                BodyPart part = parts.getBodyPart(index);
                if ("attachment".equalsIgnoreCase(part.getDisposition())) {
                    assertThat(part.getFileName()).isEqualTo("signed.pdf");
                    assertThat(part.getInputStream().readAllBytes()).isEqualTo(expected);
                    count++;
                } else {
                    count += findAttachments(part.getContent(), expected);
                }
            }
        }
        return count;
    }
}
