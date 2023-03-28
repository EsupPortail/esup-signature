package org.esupportail.esupsignature.service.mail;

import org.apache.commons.lang.StringUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.mail.MailConfig;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.EmailAlertFrequency;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureMailException;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.UserShareService;
import org.esupportail.esupsignature.service.ldap.entry.OrganizationalUnitLdap;
import org.esupportail.esupsignature.service.ldap.entry.PersonLdap;
import org.esupportail.esupsignature.service.security.otp.Otp;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@EnableConfigurationProperties(GlobalProperties.class)
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    private final GlobalProperties globalProperties;

    private MailConfig mailConfig;

    public MailService(GlobalProperties globalProperties) {
        this.globalProperties = globalProperties;
    }

    @Autowired(required = false)
    public void setMailConfig(MailConfig mailConfig) {
        this.mailConfig = mailConfig;
    }

    private JavaMailSenderImpl mailSender;

    @Autowired(required = false)
    public void setMailSender(JavaMailSenderImpl mailSender) {
        this.mailSender = mailSender;
    }

    @Resource
    private TemplateEngine templateEngine;

    @Resource
    private UserService userService;

//    @Resource
//    private CertificatService certificatService;

    @Resource
    private FileService fileService;

    @Resource
    private MessageSource messageSource;

    @Resource
    private UserShareService userShareService;

    public void sendEmailAlerts(SignRequest signRequest, String userEppn, Data data, boolean forceSend) throws EsupSignatureMailException {
        for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
            User recipientUser = recipient.getUser();
            if (!UserType.external.equals(recipientUser.getUserType())
                    && (!recipientUser.getEppn().equals(userEppn) || forceSend)
                    && (recipientUser.getEmailAlertFrequency() == null
                    || recipientUser.getEmailAlertFrequency().equals(EmailAlertFrequency.immediately)
                    || userService.checkEmailAlert(recipientUser))) {
                sendSignRequestEmailAlert(signRequest, recipientUser, data);
            }
        }
    }

    public void sendSignRequestEmailAlert(SignRequest signRequest, User recipientUser, Data data) throws EsupSignatureMailException {
        Date date = new Date();
        Set<String> toEmails = new HashSet<>();
        toEmails.add(recipientUser.getEmail());
        SignBook signBook = signRequest.getParentSignBook();
        Workflow workflow = signBook.getLiveWorkflow().getWorkflow();
        recipientUser.setLastSendAlertDate(date);
        if(data != null && data.getForm() != null) {
            for (UserShare userShare : userShareService.getUserSharesByUser(recipientUser.getEppn())) {
                if (userShare.getShareTypes().contains(ShareType.sign)) {
                    if ((data.getForm().equals(userShare.getForm())) || (workflow != null && workflow.equals(userShare.getWorkflow()))) {
                        for (User toUser : userShare.getToUsers()) {
                            toEmails.add(toUser.getEmail());
                        }
                    }
                }
            }
        }
        sendSignRequestAlert(new ArrayList<>(toEmails), signRequest);
    }

    public void sendCompletedMail(SignBook signBook, String userEppn) throws EsupSignatureMailException {
        User user = userService.getByEppn(userEppn);
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("signBook", signBook);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        setTemplate(ctx);
        Set<String> toEmails = new HashSet<>();
        toEmails.add(signBook.getCreateBy().getEmail());
        toEmails.remove(user.getEmail());
        if(toEmails.size() > 0) {
            try {
                MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
                String htmlContent = templateEngine.process("mail/email-completed.html", ctx);
                addInLineImages(mimeMessage, htmlContent);
                mimeMessage.setSubject("Votre demande signature est terminée");
                mimeMessage.setFrom(mailConfig.getMailFrom());
                mimeMessage.setTo(toEmails.toArray(String[]::new));
                logger.info("send email completed to : " + StringUtils.join(toEmails.toArray(String[]::new), ";"));
                if (mailSender != null) {
                    mailSender.send(mimeMessage.getMimeMessage());
                }
            } catch (MailSendException | MessagingException e) {
                logger.error("unable to send COMPLETE email", e);
                throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
            }
        }
    }

    public void sendPostit(SignBook signBook, Comment comment) throws EsupSignatureMailException {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("signBook", signBook);
        ctx.setVariable("comment", comment);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        setTemplate(ctx);
        Set<String> toEmails = new HashSet<>();
        toEmails.add(signBook.getCreateBy().getEmail());
        //TODO envoi dans les deux sens ?
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-postit.html", ctx);
            addInLineImages(mimeMessage, htmlContent);
            mimeMessage.setSubject("Un postit a été déposé sur votre demande");
            mimeMessage.setFrom(mailConfig.getMailFrom());
            mimeMessage.setTo(toEmails.toArray(String[]::new));
            logger.info("send email completed to : " + StringUtils.join(toEmails.toArray(String[]::new), ";"));
            if (mailSender != null) {
                mailSender.send(mimeMessage.getMimeMessage());
            }
        } catch (MailSendException | MessagingException e) {
            logger.error("unable to send COMPLETE email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        }
    }

    @Transactional
    public void sendCompletedCCMail(SignBook signBook) throws EsupSignatureMailException {
        if (!checkMailSender()) {
            return;
        }
        User user = signBook.getCreateBy();
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("signBook", signBook);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        setTemplate(ctx);
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-completed-cc.html", ctx);
            addInLineImages(mimeMessage, htmlContent);
            mimeMessage.setSubject("Une demande signature que vous suivez est terminée");
            mimeMessage.setFrom(mailConfig.getMailFrom());
            List<User> viewersArray = getViewers(signBook);
            if (viewersArray.size() > 0) {
                String[] to = new String[viewersArray.size()];
                int i = 0;
                for (User userTo : viewersArray) {
                    to[i] = userTo.getEmail();
                    i++;
                }
                mimeMessage.setTo(to);
                logger.info("send email completes cc for " + user.getEppn());
                if (mailSender != null) {
                    mailSender.send(mimeMessage.getMimeMessage());
                }
            } else {
                logger.debug("no viewers to send mail");
            }
        } catch (MailSendException | MessagingException e) {
            logger.error("unable to send email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        }
    }

    @NotNull
    private List<User> getViewers(SignBook signBook) {
        List<User> viewersArray = new ArrayList<>(signBook.getViewers());
        if(signBook.getLiveWorkflow().getWorkflow() != null && signBook.getLiveWorkflow().getWorkflow().getSendAlertToAllRecipients() != null && signBook.getLiveWorkflow().getWorkflow().getSendAlertToAllRecipients()) {
            List<LiveWorkflowStep> liveWorkflowSteps = signBook.getLiveWorkflow().getLiveWorkflowSteps();
            for (LiveWorkflowStep liveWorkflowStep : liveWorkflowSteps) {
                List<Recipient> recipients = liveWorkflowStep.getRecipients();
                for (Recipient recipient : recipients) {
                    User user1 = userService.getById(recipient.getUser().getId());
                    if (!viewersArray.contains(user1)) {
                        viewersArray.add(user1);
                    }
                }
            }
        }
        if(signBook.getLiveWorkflow().getLiveWorkflowSteps().size() > 0) {
            viewersArray.remove(signBook.getLiveWorkflow().getLiveWorkflowSteps().get(signBook.getLiveWorkflow().getLiveWorkflowSteps().size() - 1).getRecipients().stream().filter(Recipient::getSigned).map(Recipient::getUser).findAny().get());
            return viewersArray;
        } else {
            return new ArrayList<>();
        }
    }

    public void sendRefusedMail(SignBook signBook, String comment, String userEppn) throws EsupSignatureMailException {
        User user = userService.getByEppn(userEppn);
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("signBook", signBook);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        ctx.setVariable("comment", comment);
        ctx.setVariable("user", user);
        setTemplate(ctx);
        Set<String> toEmails = new HashSet<>();
        toEmails.add(signBook.getCreateBy().getEmail());
        for (LiveWorkflowStep liveWorkflowStep : signBook.getLiveWorkflow().getLiveWorkflowSteps()) {
            for(User toUser : liveWorkflowStep.getUsers()) {
                if(!user.getEppn().equals(toUser.getEppn())) {
                    toEmails.add(toUser.getEmail());
                }
            }
        }
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-refused.html", ctx);
            addInLineImages(mimeMessage, htmlContent);
            mimeMessage.setSubject("Votre demande de signature a été refusée");
            mimeMessage.setFrom(mailConfig.getMailFrom());
            mimeMessage.setTo(toEmails.toArray(String[]::new));
            String[] viewersArray = new String[signBook.getViewers().size()];
            for (int i = 0 ;  i < signBook.getViewers().size() ; i++) {
                viewersArray[i] = new ArrayList<>(signBook.getViewers()).get(i).getEmail();
            }
            mimeMessage.setCc(viewersArray);
            logger.info("send email refused to : " + StringUtils.join(toEmails.toArray(String[]::new), ";"));
            mailSender.send(mimeMessage.getMimeMessage());
        } catch (MessagingException e) {
            logger.error("unable to send REFUSE email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        }
    }

    public void sendSignRequestAlert(List<String> recipientsEmails, SignRequest signRequest) throws EsupSignatureMailException {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);

        PersonLdap personLdap = userService.findPersonLdapByUser(signRequest.getCreateBy());
        if(personLdap != null) {
            OrganizationalUnitLdap organizationalUnitLdap = userService.findOrganizationalUnitLdapByPersonLdap(personLdap);
            ctx.setVariable("organizationalUnitLdap", organizationalUnitLdap);
        }
        ctx.setVariable("signRequest", signRequest);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        setTemplate(ctx);
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-alert.html", ctx);
            addInLineImages(mimeMessage, htmlContent);
            mimeMessage.setSubject("Vous avez une nouvelle demande de signature");
            mimeMessage.setFrom(mailConfig.getMailFrom());
            mimeMessage.setTo(recipientsEmails.toArray(String[]::new));
            logger.info("send email alert for " + recipientsEmails.get(0));
//            mailSender.send(signMessage(mimeMessage.getMimeMessage()));
            mailSender.send(mimeMessage.getMimeMessage());
            signRequest.setLastNotifDate(new Date());
        } catch (Exception e) {
            logger.error("unable to send ALERT email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        }

    }

    public void sendSignRequestAlertCC(SignRequest signRequest) throws EsupSignatureMailException {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);

        PersonLdap personLdap = userService.findPersonLdapByUser(signRequest.getCreateBy());
        if(personLdap != null) {
            OrganizationalUnitLdap organizationalUnitLdap = userService.findOrganizationalUnitLdapByPersonLdap(personLdap);
            ctx.setVariable("organizationalUnitLdap", organizationalUnitLdap);
        }
        ctx.setVariable("signRequest", signRequest);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        setTemplate(ctx);
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-cc.html", ctx);
            addInLineImages(mimeMessage, htmlContent);
            mimeMessage.setSubject("Vous êtes en copie d'une demande de signature crée par " + signRequest.getCreateBy().getFirstname() + " " + signRequest.getCreateBy().getName());
            mimeMessage.setFrom(mailConfig.getMailFrom());
            List<User> viewersArray = new ArrayList<>(signRequest.getParentSignBook().getViewers());
            if (viewersArray.size() > 0) {
                String[] to = new String[viewersArray.size()];
                int i = 0;
                for (User userTo : viewersArray) {
                    to[i] = userTo.getEmail();
                    i++;
                }
                mimeMessage.setTo(to);
                logger.info("send email completes cc for " + to);
                if (mailSender != null) {
                    mailSender.send(mimeMessage.getMimeMessage());
                }
            } else {
                logger.debug("no viewers to send mail");
            }
        } catch (Exception e) {
            logger.error("unable to send ALERT email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        }

    }

    public void sendSignRequestReplayAlert(List<String> recipientsEmails, SignRequest signRequest) throws EsupSignatureMailException {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);

        PersonLdap personLdap = userService.findPersonLdapByUser(signRequest.getCreateBy());
        if(personLdap != null) {
            OrganizationalUnitLdap organizationalUnitLdap = userService.findOrganizationalUnitLdapByPersonLdap(personLdap);
            ctx.setVariable("organizationalUnitLdap", organizationalUnitLdap);
        }
        ctx.setVariable("signRequest", signRequest);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        setTemplate(ctx);
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-replay-alert.html", ctx);
            addInLineImages(mimeMessage, htmlContent);
            mimeMessage.setSubject("Relance pour la signature d'un document");
            mimeMessage.setFrom(mailConfig.getMailFrom());
            mimeMessage.setTo(recipientsEmails.toArray(String[]::new));
            logger.info("send email replay alert for " + recipientsEmails.get(0));
            mailSender.send(mimeMessage.getMimeMessage());
            signRequest.setLastNotifDate(new Date());
        } catch (MessagingException e) {
            logger.error("unable to send ALERT email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        }

    }

    public void sendCCtAlert(List<String> recipientsEmails, SignRequest signRequest) throws EsupSignatureMailException {
        if (!checkMailSender() || recipientsEmails.size() == 0) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);

        PersonLdap personLdap = userService.findPersonLdapByUser(signRequest.getCreateBy());
        if(personLdap != null) {
            OrganizationalUnitLdap organizationalUnitLdap = userService.findOrganizationalUnitLdapByPersonLdap(personLdap);
            ctx.setVariable("organizationalUnitLdap", organizationalUnitLdap);
        }
        ctx.setVariable("signRequest", signRequest);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        setTemplate(ctx);
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-cc.html", ctx);
            addInLineImages(mimeMessage, htmlContent);
            User creator = signRequest.getCreateBy();
            mimeMessage.setSubject("Vous êtes en copie d'une demande de signature crée par " + creator.getFirstname() + " " + creator.getName());
            mimeMessage.setFrom(mailConfig.getMailFrom());
            mimeMessage.setTo(recipientsEmails.toArray(String[]::new));
            logger.info("send email cc for " + recipientsEmails.get(0));
            mailSender.send(mimeMessage.getMimeMessage());
            signRequest.setLastNotifDate(new Date());
        } catch (MessagingException e) {
            logger.error("unable to send CC ALERT email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        }

    }

    public void sendSignRequestSummaryAlert(List<String> recipientsEmails, List<SignRequest> signRequests) throws EsupSignatureMailException {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("signRequests", signRequests);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        setTemplate(ctx);
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-alert-summary.html", ctx);
            addInLineImages(mimeMessage, htmlContent);
            mimeMessage.setSubject("Liste des demandes à signer");
            mimeMessage.setFrom(mailConfig.getMailFrom());
            mimeMessage.setTo(recipientsEmails.toArray(String[]::new));
            mimeMessage.setText(htmlContent, true);
            logger.info("send email alert for " + recipientsEmails.get(0));
            mailSender.send(mimeMessage.getMimeMessage());
        } catch (MessagingException e) {
            logger.error("unable to send SUMMARY email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        }

    }

    public void sendOtp(Otp otp, String urlId, SignBook signBook) throws EsupSignatureMailException {
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("url", globalProperties.getRootUrl() + "/otp-access/" + urlId);
        ctx.setVariable("signBook", signBook);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        setTemplate(ctx);
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-otp.html", ctx);
            addInLineImages(mimeMessage, htmlContent);
            mimeMessage.setSubject("Vous avez un document à signer émanant de " + messageSource.getMessage("application.footer", null, Locale.FRENCH));
            mimeMessage.setFrom(mailConfig.getMailFrom());
            mimeMessage.setTo(otp.getEmail());
            logger.info("send email alert for " + otp.getEmail());
            mailSender.send(mimeMessage.getMimeMessage());
        } catch (MessagingException e) {
            logger.error("unable to send OTP email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        }
    }

    public void sendFile(String title, SignBook signBook, String targetUri) throws MessagingException, IOException {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("signBook", signBook);
        ctx.setVariable("signRequests", signBook.getSignRequests());
        User user = signBook.getCreateBy();
        ctx.setVariable("user", user);
        setTemplate(ctx);
        MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
        String htmlContent = templateEngine.process("mail/email-file.html", ctx);
        addInLineImages(mimeMessage, htmlContent);
        mimeMessage.setSubject("Nouveau document signé à télécharger : " + title);
        mimeMessage.setFrom(mailConfig.getMailFrom());
        mimeMessage.setTo(targetUri.replace("mailto:", "").split(","));
        mailSender.send(mimeMessage.getMimeMessage());

    }

    private void addInLineImages(MimeMessageHelper mimeMessage, String htmlContent) throws MessagingException {
        mimeMessage.setText(htmlContent, true);
        mimeMessage.addInline("logo", new ClassPathResource("/static/images/logo.png", MailService.class));
        mimeMessage.addInline("logo-univ", new ClassPathResource("/static/images/logo-univ.png", MailService.class));
    }

    public void sendTest(List<String> recipientsEmails) throws MessagingException {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        final MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper message;
        message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        message.setSubject("esup-signature test mail");
        message.setFrom(mailConfig.getMailFrom());
        message.setTo(recipientsEmails.toArray(String[]::new));
        String htmlContent = templateEngine.process("mail/email-test.html", ctx);
        message.setText(htmlContent, true);
        logger.info("send test email for " + recipientsEmails.get(0));
        mailSender.send(mimeMessage);
    }

    private void setTemplate(Context ctx) {
        try {
            ctx.setVariable("logo", fileService.getBase64Image(new ClassPathResource("/static/images/logo.png", MailService.class).getInputStream(), "logo.png"));
            ctx.setVariable("logoUrn", fileService.getBase64Image(new ClassPathResource("/static/images/logo-univ.png", MailService.class).getInputStream(), "logo-univ.png"));
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

    public JavaMailSenderImpl getMailSender() {
        return mailSender;
    }

//    public MimeMessage signMessage(MimeMessage message) {
//        try {
//            if(globalProperties.getSignEmailWithSealCertificat()) {
//                AbstractKeyStoreTokenConnection tokenConnection = certificatService.getSealToken();
//                DSSPrivateKeyEntry dssPrivateKeyEntry = tokenConnection.getKeys().get(0);
//                X509Certificate x509Certificate = dssPrivateKeyEntry.getCertificate().getCertificate();
//                PrivateKey privateKey = certificatService.getSealPrivateKey();
//                SMIMECapabilityVector capabilities = new SMIMECapabilityVector();
//                capabilities.addCapability(SMIMECapability.dES_EDE3_CBC);
//                capabilities.addCapability(SMIMECapability.rC2_CBC, 128);
//                capabilities.addCapability(SMIMECapability.dES_CBC);
//                capabilities.addCapability(SMIMECapability.aES256_CBC);
//                ASN1EncodableVector attributes = new ASN1EncodableVector();
//                attributes.add(new SMIMECapabilitiesAttribute(capabilities));
//                IssuerAndSerialNumber issAndSer = new IssuerAndSerialNumber(new X500Name(x509Certificate.getIssuerX500Principal().getName()), x509Certificate.getSerialNumber());
//                attributes.add(new SMIMEEncryptionKeyPreferenceAttribute(issAndSer));
//                SMIMESignedGenerator signer = new SMIMESignedGenerator();
//                signer.addSignerInfoGenerator(new JcaSimpleSignerInfoGeneratorBuilder()
//                        .setSignedAttributeGenerator(new AttributeTable(attributes))
//                        .build("SHA1withRSA", privateKey, x509Certificate));
//                List<X509Certificate> certList = new ArrayList<>();
//                certList.add(x509Certificate);
//                JcaCertStore jcaCertStore = new JcaCertStore(certList);
//                signer.addCertificates(jcaCertStore);
//                MimeMultipart mm = signer.generate(message);
//                message.setContent(mm, mm.getContentType());
//                message.saveChanges();
//            }
//            return message;
//        } catch (Exception e) {
//            logger.debug(e.getMessage(), e);
//        }
//        return message;
//    }

}
