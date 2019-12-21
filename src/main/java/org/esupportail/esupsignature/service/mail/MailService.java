package org.esupportail.esupsignature.service.mail;

import org.apache.commons.compress.utils.IOUtils;
import org.esupportail.esupsignature.config.mail.MailConfig;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
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
import java.util.List;
import java.util.Locale;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    private MailConfig mailConfig;
    private JavaMailSenderImpl mailSender;


    @Autowired(required = false)
    public void setMailConfig(MailConfig mailConfig) {
        this.mailConfig = mailConfig;
    }

    @Autowired(required = false)
    public void setMailConfig(JavaMailSenderImpl mailSender) {
        this.mailSender = mailSender;
    }

    @Autowired
    ResourceLoader resourceLoader;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TemplateEngine templateEngine;

    @Resource
    private SignBookService signBookService;

    @Autowired
    private FileService fileService;

    @Value("${root.url}")
    private String rootUrl;

    public void sendCompletedMail(SignBook signBook) {
        if (!checkMailSender()) {
            return;
        }
        User user = userRepository.findByEppn(signBook.getCreateBy()).get(0);
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("signRequest", signBook);
        ctx.setVariable("rootUrl", rootUrl);
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
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            logger.error("unable to sens email", e);
        }

    }

    public void sendSignRequestAlert(String recipientEmail, List<SignRequest> signRequests) {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("signRequests", signRequests);
        ctx.setVariable("rootUrl", rootUrl);
        for(SignRequest signRequest : signRequests) {
            User user = userRepository.findByEppn(signRequest.getCreateBy()).get(0);
            signRequest.setCreator(user);
        }
        setTemplate(ctx);
        final MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper message;
        try {
            message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            message.setSubject("Esup-Signature : nouveau document à signer");
            message.setFrom(mailConfig.getMailFrom());
            message.setTo(recipientEmail);
            String htmlContent = templateEngine.process("mail/email-alert.html", ctx);
            message.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            logger.error("unable to sens email", e);
        }

    }

    public void sendFile(SignBook signBook) {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("rootUrl", rootUrl);
        ctx.setVariable("signBook", signBook);
        User user = userRepository.findByEppn(signBook.getCreateBy()).get(0);
        ctx.setVariable("user", user);
        setTemplate(ctx);
        final MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper message;
        try {
            message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            message.setSubject("Esup-Signature : nouveau document  " + signBook.getName());
            message.setFrom(mailConfig.getMailFrom());
            message.setTo(signBook.getDocumentsTargetUri());
            List<Document> toSendDocuments = signBookService.getLastSignedDocuments(signBook);
            for (Document toSendDocument : toSendDocuments) {
                message.addAttachment(toSendDocument.getFileName(), new ByteArrayResource(IOUtils.toByteArray(toSendDocument.getInputStream())));
            }
            String htmlContent = templateEngine.process("mail/email-file.html", ctx);
            message.setText(htmlContent, true); // true = isHtml
            mailSender.send(mimeMessage);
        } catch (MessagingException | IOException e) {
            logger.error("unable to sens email", e);
        }
    }

    private void setTemplate(Context ctx) {
        try {
            ctx.setVariable("logo", fileService.getBase64Image(new ClassPathResource("/static/images/logo.png", MailService.class).getInputStream(), "logo.png"));
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

}
