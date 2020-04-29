package org.esupportail.esupsignature.service.mail;

import org.apache.commons.compress.utils.IOUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.mail.MailConfig;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.repository.LogRepository;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.repository.UserShareRepository;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@EnableConfigurationProperties(GlobalProperties.class)
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    private GlobalProperties globalProperties;
    private MailConfig mailConfig;
    private JavaMailSenderImpl mailSender;

    public MailService(GlobalProperties globalProperties, MailConfig mailConfig, JavaMailSenderImpl mailSender) {
        this.globalProperties = globalProperties;
        this.mailConfig = mailConfig;
        this.mailSender = mailSender;
    }

    @Resource
    private TemplateEngine templateEngine;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private UserService userService;

    @Resource
    private FileService fileService;

    public void sendCompletedMail(SignBook signBook) {
        if (!checkMailSender()) {
            return;
        }
        User user = signBook.getCreateBy();
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("signBook", signBook);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        setTemplate(ctx);
        final MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper message;
        try {
            message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            message.setSubject("Esup-Signature : demande signature terminée");
            message.setFrom(mailConfig.getMailFrom());
            message.setTo(user.getEmail());
            String htmlContent = templateEngine.process("mail/email-completed.html", ctx);
            message.setText(htmlContent, true);
            logger.info("send email completes for " + user.getName());
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            logger.error("unable to send email", e);
        }
    }

    public void sendRefusedMail(SignBook signBook, String comment) {
        if (!checkMailSender()) {
            return;
        }
        User user = signBook.getCreateBy();
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("signBook", signBook);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        ctx.setVariable("comment", comment);
        setTemplate(ctx);
        final MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper message;
        List<String> toEmails = new ArrayList<>();
        toEmails.add(user.getEmail());
        for(Recipient recipient : signBook.getWorkflowSteps().get(signBook.getWorkflowSteps().size() - 1).getRecipients()) {
            //TODO search shares
            toEmails.add(recipient.getUser().getEmail());
        }
        try {
            message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            message.setSubject("Esup-Signature : demande signature refusée");
            message.setFrom(mailConfig.getMailFrom());
            message.setTo(toEmails.toArray(String[]::new));
            String htmlContent = templateEngine.process("mail/email-refused.html", ctx);
            message.setText(htmlContent, true);
            logger.info("send email refude for " + toEmails.get(0));
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            logger.error("unable to send email", e);
        }
    }

    public void sendSignRequestAlert(List<String> recipientsEmails, SignRequest signRequest) {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("signRequest", signRequest);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        setTemplate(ctx);
        final MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper message;
        try {
            message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            User creator = signRequest.getCreateBy();
            message.setSubject("Nouvelle demande de : " + creator.getFirstname() + " " + creator.getName() + " : " + signRequest.getTitle());
            message.setFrom(mailConfig.getMailFrom());
            message.setTo(recipientsEmails.toArray(String[]::new));
            String htmlContent = templateEngine.process("mail/email-alert.html", ctx);
            message.setText(htmlContent, true);
            logger.info("send email alert for " + recipientsEmails.get(0));
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            logger.error("unable to send email", e);
        }

    }

    public void sendSignRequestSummaryAlert(List<String> recipientsEmails, List<SignRequest> signRequests) {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("signRequests", signRequests);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        setTemplate(ctx);
        final MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper message;
        try {
            message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            message.setSubject("Esup-Signature : nouveau document à signer");
            message.setFrom(mailConfig.getMailFrom());
            message.setTo(recipientsEmails.toArray(String[]::new));
            String htmlContent = templateEngine.process("mail/email-alert-summary.html", ctx);
            message.setText(htmlContent, true);
            logger.info("send email alert for " + recipientsEmails.get(0));
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            logger.error("unable to send email", e);
        }

    }

//    public void sendFile(SignBook signBook) {
//        if (!checkMailSender()) {
//            return;
//        }
//        final Context ctx = new Context(Locale.FRENCH);
//        ctx.setVariable("rootUrl", rootUrl);
//        ctx.setVariable("signBook", signBook);
//        UserUi user = userRepository.findByEppn(signBook.getCreateBy()).get(0);
//        ctx.setVariable("user", user);
//        setTemplate(ctx);
//        final MimeMessage mimeMessage = mailSender.createMimeMessage();
//        MimeMessageHelper message;
//        try {
//            message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
//            message.setSubject("Esup-Signature : nouveau document  " + signBook.getName());
//            message.setFrom(mailConfig.getMailFrom());
//            message.setTo(signBook.getDocumentsTargetUri());
//            List<Document> toSendDocuments = signBookService.getLastSignedDocuments(signBook);
//            for (Document toSendDocument : toSendDocuments) {
//                message.addAttachment(toSendDocument.getFileName(), new ByteArrayResource(IOUtils.toByteArray(toSendDocument.getInputStream())));
//            }
//            String htmlContent = templateEngine.process("mail/email-file.html", ctx);
//            message.setText(htmlContent, true); // true = isHtml
//            mailSender.send(mimeMessage);
//        } catch (MessagingException | IOException e) {
//            logger.error("unable to send email", e);
//        }
//    }

    public void sendFile(String title, List<SignRequest> signRequests, String targetUri) throws MessagingException, IOException {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("signRequests", signRequests);
        User user = signRequests.get(0).getCreateBy();
        ctx.setVariable("user", user);
        setTemplate(ctx);
        final MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper message;
        message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        message.setSubject("Esup-Signature : nouveau document  " + title);
        message.setFrom(mailConfig.getMailFrom());
        message.setTo(targetUri);
        for(SignRequest signRequest : signRequests) {
            Document toSendDocument = signRequestService.getLastSignedDocument(signRequest);
            message.addAttachment(toSendDocument.getFileName(), new ByteArrayResource(IOUtils.toByteArray(toSendDocument.getInputStream())));
        }
        String htmlContent = templateEngine.process("mail/email-file.html", ctx);
        message.setText(htmlContent, true); // true = isHtml
        mailSender.send(mimeMessage);

    }

    public void sendTest(List<String> recipientsEmails) {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        final MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper message;
        try {
            message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            message.setSubject("esup-signature test mail");
            message.setFrom(mailConfig.getMailFrom());
            message.setTo(recipientsEmails.toArray(String[]::new));
            String htmlContent = templateEngine.process("mail/email-test.html", ctx);
            message.setText(htmlContent, true);
            logger.info("send test email for " + recipientsEmails.get(0));
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            logger.error("unable to send test email", e);
        }

    }

    private void setTemplate(Context ctx) {
        try {
            ctx.setVariable("logo", fileService.getBase64Image(new ClassPathResource("/static/images/logo.png", MailService.class).getInputStream(), "logo.png"));
            ctx.setVariable("logoUrn", fileService.getBase64Image(new ClassPathResource("/static/images/logo-urn.png", MailService.class).getInputStream(), "logo-urn.png"));
            try (Reader reader = new InputStreamReader(new ClassPathResource("/static/css/bootstrap.min.css", MailService.class).getInputStream(), UTF_8)) {
                ctx.setVariable("css", FileCopyUtils.copyToString(reader));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean checkMailSender() {
        if (mailSender == null) {
            logger.warn("message not sended : mail host not configured");
            return false;
        }
        return true;
    }

    public MailConfig getMailConfig() {
        return mailConfig;
    }
}
